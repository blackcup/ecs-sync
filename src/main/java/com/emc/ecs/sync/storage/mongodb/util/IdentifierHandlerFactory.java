package com.emc.ecs.sync.storage.mongodb.util;

import com.emc.ecs.sync.config.storage.MongodbConfig;
import com.emc.ecs.sync.storage.mongodb.util.imp.FileSystemStyleIdentifierHandler;

/**
 * Created by charlie on 5/10/17.
 */
public class IdentifierHandlerFactory {
    public static IdentifierHandler getIdentifierHandler(MongodbConfig config){
        return new FileSystemStyleIdentifierHandler(config);
    }
}
