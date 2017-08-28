package com.emc.ecs.sync.storage.mongodb.util.imp;

import com.emc.ecs.sync.config.storage.MongodbConfig;
import com.emc.ecs.sync.storage.mongodb.AbstractMongodbStorage;
import com.emc.ecs.sync.storage.mongodb.util.IdentifierHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by charlie on 5/10/17.
 * parse url like this localhost:xxxx/?database=AAA&collection=BBB
 */
public class DatabaseStyleIdentifierHandler implements IdentifierHandler {

    MongodbConfig config;
    private List<String> databaseCache;
    public DatabaseStyleIdentifierHandler(MongodbConfig config){
        this.config = config ;
        databaseCache = new ArrayList<String>();

    }
    @Override
    public String getIdentifier(String relativePath, boolean directory) {

        String identifier = null;
        boolean tag = false;
        for (String databaseName : databaseCache) {
            if (relativePath.contains(databaseName)) {
                if (directory) {
                    throw new RuntimeException(relativePath + "is a directory");
                } else {
                    identifier = getIdentifier(databaseName, relativePath);
                }
                tag = true;
                break;
            }
        }
        if(!tag){
            if(directory){
                databaseCache.add(relativePath);
                identifier = getIdentifier(relativePath,null);
            }else{
                identifier = getIdentifier(AbstractMongodbStorage.DEFAULT_DABABASE_NAME,relativePath);
            }
        }
        return identifier;
    }
    public String getIdentifier(String databse,String collection){
        String result = null;
        if(databse!=null){
            if(collection!=null){
                result = config.getUrl() + "?" + "database=" + databse + "&" + "collection=" + collection;
            }else{
                result = config.getUrl() + "?" + "database=" + databse;
            }
        }else{
            if(collection==null){
                result = config.getUrl();
            }else{
                throw new RuntimeException("database is null ,but collection is not null");
            }
        }
        return result;
    }

    @Override
    public String getRelativePath(String identifier, boolean directory) {
        String[] split = identifier.split("\\?");
        if(split.length<=1){
            return "";
        }
        return split[1];
    }

    @Override
    public Map<String, String> getParameters(String identifier) {
        String[] split = identifier.split("\\?");
        Map<String,String> hashMap =null;
        if(split.length<=1){
            return null;
        }else if(split.length==2){
            String s = split[1];
            hashMap = doGetParameters(s);
        }else if(split.length>2){
            String paramString = "";
            for (int i = 1; i < split.length; i++) {
                if(i==split.length - 1){
                    paramString = paramString + split[i] ;
                }else if(i< split.length - 1 ){
                    paramString = paramString + split[i] + "?";
                }
            }
            hashMap = doGetParameters(paramString);
        }
        return hashMap;
    }
    /***
     *  parse the subString of identifier;example: database=AAA&collection=BBB
     * @param paramString
     * @return
     */
    private Map<String,String> doGetParameters(String paramString){
        String[] kvs = paramString.split("&");
        HashMap<String, String> map = new HashMap<>();
        for (String kv : kvs) {
            String[] split = kv.split("=");
            map.put(split[0],split[split.length-1]);
        }
        return map;
    }
}
