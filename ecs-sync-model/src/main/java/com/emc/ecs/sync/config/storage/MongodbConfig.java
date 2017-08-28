package com.emc.ecs.sync.config.storage;

import com.emc.ecs.sync.config.AbstractConfig;
import com.emc.ecs.sync.config.annotation.*;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import static com.emc.ecs.sync.config.storage.MongodbConfig.URI_PREFIX;


/**
 * Created by charlie on 5/3/17.
 *
 */
@XmlRootElement
@StorageConfig(uriPrefix = URI_PREFIX)
@Label("Mongodb")
@Documentation("I have nothing to say")
public class MongodbConfig extends AbstractConfig {
    public static final String URI_PREFIX = "mongodb";
    private final static int DEFAULT_PORT = 27017;
    private String username;
    private String password;
    private int port;
    private String host;
    private String database;
    private String collection;
    private String url;
    private String uri;

    @XmlTransient
    @UriGenerator
    public String getUri() {
        if(port <= 0){
            port = DEFAULT_PORT;
        }
        if(username==null || password==null){
            return URI_PREFIX + "://"  + host + ":" + port + "/";
        }
        return URI_PREFIX + "://" + username + ":" + password + "@" + host + ":" + port + "/";
    }

    @UriParser
    public void setUri(String uri) {
        this.uri = uri ;
    }
    @Option(orderIndex = 11,required = false, description = "mongodb username")
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    @Option(orderIndex = 12,  required = false, description = "password to validate")
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    @Option(orderIndex = 15, locations = Option.Location.Form, required = false, description = "collection want to operator")
    public String getCollection() {
        return collection;
    }
    public void setCollection(String collection) {
        this.collection = collection;
    }

    @Option(orderIndex = 13, advanced=true, description = "mongodb port")
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        if(port <= 0){
            this.port = DEFAULT_PORT;
        }else{
            this.port = port ;
        }
    }

    @Option(orderIndex = 10, locations = Option.Location.Form, required = true, description = "mongodb host")
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    /***
     * @return
     */
    public String getUrl() {
        if(url==null){
            url = getUri();
        }
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
    @Option(orderIndex = 14, locations = Option.Location.Form, required = false, description = "mongodb database")
    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }


}
