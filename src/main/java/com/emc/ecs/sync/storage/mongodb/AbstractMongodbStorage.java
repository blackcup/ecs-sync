package com.emc.ecs.sync.storage.mongodb;

import com.emc.ecs.sync.config.storage.MongodbConfig;
import com.emc.ecs.sync.model.ObjectMetadata;
import com.emc.ecs.sync.model.ObjectSummary;
import com.emc.ecs.sync.model.SyncObject;
import com.emc.ecs.sync.storage.AbstractStorage;
import com.emc.ecs.sync.storage.ObjectNotFoundException;
import com.emc.ecs.sync.storage.mongodb.io.MongoDBInputStream;
import com.emc.ecs.sync.storage.mongodb.util.IdentifierHandler;
import com.emc.ecs.sync.storage.mongodb.util.IdentifierHandlerFactory;
import com.emc.ecs.sync.util.LazyValue;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoNamespace;
import com.mongodb.client.*;
import org.bson.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by charlie on 5/3/17.
 */
public abstract class AbstractMongodbStorage <C extends MongodbConfig> extends AbstractStorage<C> {
    public final static String DATABASE_PARAM="database";
    public final static String COLLECTION_PARAM = "collection";
    public final static String DEFAULT_DABABASE_NAME = "storage";
    private List<String>databaseCache;
    private IdentifierHandler handler = null ;
    private MongoClient client;
    public MongoDatabase getDatabase(String baseName){
        return client.getDatabase(baseName);
    }
    public AbstractMongodbStorage(MongodbConfig mongodbConfig){
        databaseCache = new ArrayList<String>();
        String url = config.getUrl();
        client = new MongoClient(url);
    }

    @Override
    public void setConfig(C config) {
        super.setConfig(config);
        handler = IdentifierHandlerFactory.getIdentifierHandler(config);
        initClient();
    }

    public AbstractMongodbStorage(){
        databaseCache = new ArrayList<String>();
    }
    @Override
    public String getRelativePath(String identifier, boolean directory) {
        return handler.getRelativePath(identifier,directory);
    }

    @Override
    public String getIdentifier(String relativePath, boolean directory) {
        return handler.getIdentifier(relativePath,directory);
    }
    private void initClient(){
        if(client == null){
            String url = config.getUrl();
            client = new MongoClient(new MongoClientURI(url));
        }
    }
    @Override
    public Iterable<ObjectSummary> allObjects() {
        String databaseName = config.getDatabase();
        List<ObjectSummary> objectSummaries = new ArrayList<ObjectSummary>();
        if(databaseName == null){
            MongoCursor<String> iterator = client.listDatabaseNames().iterator();
            while(iterator.hasNext()){
                String next = iterator.next();
                MongoDatabase db = client.getDatabase(next);
                ObjectSummary objectSummary = getObjectSummary(db);
                objectSummaries.add(objectSummary);
            }
        }else if(config.getCollection() == null){
            MongoDatabase database = getDatabase(databaseName);
            MongoCursor<String> collections = database.listCollectionNames().iterator();
            while(collections.hasNext()){
                String next = collections.next();
                MongoCollection<Document> collection = database.getCollection(next);
                ObjectSummary objectSummary = getObjectSummary(collection);
                objectSummaries.add(objectSummary);
            }
        }else if(config.getCollection()!=null) {
            MongoDatabase database = getDatabase(databaseName);
            MongoCollection<Document> collection = database.getCollection(config.getCollection());
            ObjectSummary objectSummary = getObjectSummary(collection);
            objectSummaries.add(objectSummary);
        }
        return objectSummaries;
    }
    private ObjectSummary getObjectSummary(MongoDatabase db){
        String name = db.getName();
        String identifier = handler.getIdentifier(name, null);
        return new ObjectSummary(identifier,true,0);
    }
    private long getCollectionSize(String databaseName,String collectionNameSpace){
        long length = 0L;
        MongoDatabase database = client.getDatabase(databaseName);
        Document dataSize = database.runCommand(new Document("dataSize", collectionNameSpace));
        Object size = dataSize.get("size");
        if(size!=null){
            String lengthStr = size.toString();
            length = Long.parseLong(lengthStr);
        }
        return length;
    }
    private ObjectSummary getObjectSummary(MongoCollection<Document> collection){
        MongoNamespace namespace = collection.getNamespace();
        String collectionName = namespace.getCollectionName();
        String databaseName = namespace.getDatabaseName();
        long size = getCollectionSize(databaseName, namespace.getFullName());
        String identifier = handler.getIdentifier(databaseName, collectionName);
        return new ObjectSummary(identifier,false,size);
    }
    @Override
    public Iterable<ObjectSummary> children(ObjectSummary parent) {
        if(!parent.isDirectory()){
            return null;
        }else{
            String identifier = parent.getIdentifier();
            Map<String, String> parameters = handler.getParameters(identifier);
            String databaseName = parameters.get(DATABASE_PARAM);
            String collectionName = parameters.get(COLLECTION_PARAM);
            if (databaseName!=null&&collectionName==null){
                return getAllCollections(databaseName);
            }
            return null;
        }

    }
    private  Iterable<ObjectSummary> getAllCollections(String databaseName){
        MongoDatabase database = client.getDatabase(databaseName);
        List<ObjectSummary> objectSummaries = new ArrayList<ObjectSummary>();
        MongoCursor<String> collectionNames = database.listCollectionNames().iterator();
        while(collectionNames.hasNext()){
            String collectionName = collectionNames.next();
            MongoCollection<Document> collection = database.getCollection(collectionName);
            ObjectSummary objectSummary = getObjectSummary(collection);
            objectSummaries.add(objectSummary);
        }
        return objectSummaries;
    }

    /**
     * Load context about this identifier,especially for inputstream
     * First, you should get some params from identifier,example database and collection.
     * Second,get metadata according to these params. the Collection regard as the regular file,the database
     * regard as the directory.
     * Third,metadata.contentLength does not equal the dateSize in mongodb,and I choose give it a Long.MAX_VALUE,
     * because every record from collection will transfer into json date, and the json date will be supplied for
     * other streams, Pls see com.emc.ecs.sync.storage.mongodb.io.MongoDBInputStream
     * @param identifier
     * @return
     * @throws ObjectNotFoundException
     */
    @Override
    public SyncObject loadObject(String identifier) throws ObjectNotFoundException {
        Map<String, String> parameters = handler.getParameters(identifier);
        String databaseName = parameters.get(DATABASE_PARAM);
        String collectionName = parameters.get(COLLECTION_PARAM);
        MongoDatabase database = getDatabase(databaseName);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setModificationTime(new Date());
        metadata.setMetaChangeTime(new Date());
        SyncObject syncObject = null;
        if(databaseName==null||collectionName==null){
            metadata.setDirectory(true);
            syncObject = new SyncObject(this,getRelativePath(identifier,true),metadata);
        }else{
            String namespace = database.getCollection(collectionName).getNamespace().getFullName();
            long collectionSize = getCollectionSize(databaseName, namespace);
            metadata.setContentLength(Long.MAX_VALUE);
            syncObject = new SyncObject(this,getRelativePath(identifier,false),metadata);
            final MongoCollection<Document> collection = getCollection(databaseName, collectionName);
            syncObject.withLazyAcl(null);
            LazyValue<InputStream> inputStream = new LazyValue<InputStream>(){
                @Override
                public InputStream get() {
                    return new MongoDBInputStream(collection);
                }
            };
            syncObject.withLazyStream(inputStream);
        }
        return syncObject;
    }

    /**
     * return the MongoCollection<Document> collection
     * @param databaseName
     * @param collectionName
     * @return dataset
     */
    private MongoCollection<Document> getCollection(String databaseName,String collectionName){
        return client.getDatabase(databaseName).getCollection(collectionName);
    }

    /**
     * parse the identifier and get the DATABASE and COLLECTION
     * example : http://localhost:9200?database=AAA&collection=BBB
     * parse result : database : AAA ; collection : BBB
     * @param identifier
     * @return
     */

    /**
     * import or update dataset into mongodb
     * the importIntoMongo method does the actual work;
     *
     * @param identifier
     * @param object
     */
    @Override
    public void updateObject(String identifier, SyncObject object) {
        Map<String, String> parameters = handler.getParameters(identifier);
        String database = parameters.get(DATABASE_PARAM);
        String collectionName = parameters.get(COLLECTION_PARAM);
        if(database==null){
            throw new RuntimeException("without database");
        }else if(database!=null && collectionName==null){
            getDatabase(database);
        }else if(database!=null&&collectionName!=null){
            MongoDatabase db = getDatabase(database);
            MongoCollection<Document> collection = null;
            collection = db.getCollection(collectionName);
            importIntoMongo(collection,object);
        }
    }

    /**
     *
     * @param collection
     * @param object
     */
    public void importIntoMongo(MongoCollection<Document> collection,SyncObject object){
        ObjectMetadata metadata = object.getMetadata();
        boolean directory = metadata.isDirectory();
        if(directory){
            throw new RuntimeException(object.getRelativePath() + "is a directory");
        }else{
            InputStream dataStream = object.getDataStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(dataStream));
            int maxCount = 10000;
            List<Document> list = new ArrayList<Document>(maxCount);
            int count = 0;
            while (true){
                try{
                    String line = br.readLine();
                    if(line!=null){
                        count ++ ;
                        Document parse = Document.parse(line);
                        list.add(parse);
                        if(count>maxCount){
                            collection.insertMany(list);
                            helpGC(list);
                            list.clear();
                            count = 0;
                        }
                    }else{
                        if(list.size()>0){
                            collection.insertMany(list);
                            helpGC(list);
                            list.clear();
                        }
                        break;
                    }

                }catch (IOException e){
                    try {
                        br.close();
                        break;
                    }catch (IOException e1){
                        e1.printStackTrace();
                    }
                }
            }
        }
    }
    private <T> void helpGC(List<T> list){
        for (T t : list) {
            t = null;
        }
    }
    @Override
    protected ObjectSummary createSummary(String identifier) {
        boolean isCollectionExist = identifier.contains(COLLECTION_PARAM);
        boolean isDatabaseExist = identifier.contains(DATABASE_PARAM);
        ObjectSummary summary;
        if(isDatabaseExist==false&&isCollectionExist==true){
            System.out.println("wrong identifier:" + identifier );
            return null;
        }
        if ((isCollectionExist&&isDatabaseExist)==false){
            summary = new ObjectSummary(identifier,true,0);
        }else{
            summary = new ObjectSummary(identifier,false,0);
        }
        return summary;
    }
}
