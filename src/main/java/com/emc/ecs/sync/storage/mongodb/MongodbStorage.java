package com.emc.ecs.sync.storage.mongodb;

import com.emc.ecs.sync.config.storage.MongodbConfig;

/**
 * Created by charlie on 5/3/17.
 */
public class MongodbStorage extends AbstractMongodbStorage<MongodbConfig>{

    public MongodbStorage(MongodbConfig mongodbConfig) {
        super(mongodbConfig);
    }
    public MongodbStorage(){
        super();
    }
}
