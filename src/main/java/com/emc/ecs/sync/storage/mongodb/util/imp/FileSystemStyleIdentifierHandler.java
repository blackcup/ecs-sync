package com.emc.ecs.sync.storage.mongodb.util.imp;

import com.emc.ecs.sync.config.storage.MongodbConfig;
import com.emc.ecs.sync.storage.mongodb.AbstractMongodbStorage;
import com.emc.ecs.sync.storage.mongodb.util.IdentifierHandler;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by charlie on 5/10/17.
 * parse url like this localhost:xxxx/AAA/BBB
 */
public class FileSystemStyleIdentifierHandler implements IdentifierHandler {
    private MongodbConfig config ;
    public FileSystemStyleIdentifierHandler(MongodbConfig config){
        this.config = config ;
    }
    @Override
    public String getIdentifier(String relativePath, boolean directory) {
        String identifier = null ;
        String database = config.getDatabase();
        if(database!=null){
            if(relativePath == null||relativePath.isEmpty()){
                if(config.getCollection()==null||config.getCollection().isEmpty()){
                    relativePath = new Date().toLocaleString();
                }else{
                    relativePath = config.getCollection();
                }
            }
            identifier = config.getUrl() + database + "/" + relativePath;
        }else{
            identifier = config.getUrl() + relativePath;
        }
        /*String identifier = config.getUrl();
        if(relativePath==null||relativePath.equals("")){
            return identifier;
        }else{
            relativePath = doSomeWork(relativePath);
            if(directory){
                identifier = identifier + relativePath;
            }else{
                if(config.getDatabase()==null||config.getDatabase().equals("")){
                    identifier = identifier + relativePath;
                }else{
                    identifier = identifier + config.getDatabase() + "/" + relativePath;
                }
            }
        }*/
        return identifier;
    }
    private String  doSomeWork(String relativePath){
        if(relativePath.startsWith("/")){
            relativePath = relativePath.substring(1);
        }
        if(!relativePath.endsWith("/")){
            relativePath = relativePath + "/";
        }
        return relativePath;
    }
    @Override
    public String getIdentifier(String databse, String collection) {
        String rootPath = config.getUrl();
        String result = null;
        if(databse!=null&&(!databse.equals(""))){
            if(collection==null||collection.equals("")){
                result = config.getUrl() + databse + "/";
            }else{
                result = config.getUrl() + databse + "/" + collection ;
            }
        }else{
            if(collection==null || collection.equals("")){
                result = config.getUrl();
            }else{
                throw new RuntimeException("the database param is empty but the collection param is not empty");
            }
        }
        return result;
    }

    @Override
    public String getRelativePath(String identifier, boolean directory) {
        Map<String, String> parameters = getParameters(identifier);
        String databaseName = parameters.get(AbstractMongodbStorage.DATABASE_PARAM);
        String collectionName = parameters.get(AbstractMongodbStorage.COLLECTION_PARAM);
        if(config.getDatabase()!=null){
            return collectionName;
        }else{
            if(collectionName!=null)
                return databaseName + "/" + collectionName;
            else
                return databaseName + "/";
        }
        /*String regx = config.getPort() + "/";
        String[] split = identifier.split(regx);
        if(split.length==1){
            return "";
        }else if(split.length==2){
            if(directory){
                return split[0];
            }else{
                String relative = split[1];
                if(relative.endsWith("/")){
                    relative = relative.substring(0,relative.length()-1);
                }
                return relative;
            }
        }else{
            new RuntimeException("not supported identifier : " + identifier);
        }*/
        //return null;
    }
    @Override
    public Map<String, String> getParameters(String identifier) {
        Map<String ,String> paramsters = null;
        String regx = config.getPort() + "/";
        String[] split = identifier.split(regx);
        if(split.length==1){
            return new HashMap<String,String>();
        }else if(split.length == 2) {
            String s = split[1];
            paramsters = doGetParameters(s);
        }
        return paramsters;
    }
    private Map<String, String>  doGetParameters(String s){
        Map<String,String> map = new HashMap<String,String>();
        if(s==null||s.isEmpty()){
            return map;
        }
        if(s.startsWith("/")){
            s = s.substring(1);
        }
        if(s.endsWith("/")){
            s = s.substring(0,s.length()-1);
        }
        String[] split = s.split("/");
        if(split.length==1){
            map.put(AbstractMongodbStorage.DATABASE_PARAM,split[0]);
        }else if(split.length == 2){
            map.put(AbstractMongodbStorage.DATABASE_PARAM,split[0]);
            map.put(AbstractMongodbStorage.COLLECTION_PARAM,split[1]);
        }else{
            throw new RuntimeException("path is too deep");
        }
        return map;
    }
}
