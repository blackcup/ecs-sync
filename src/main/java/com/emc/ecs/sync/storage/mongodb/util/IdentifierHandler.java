package com.emc.ecs.sync.storage.mongodb.util;

import java.util.Map;

/**
 * Created by charlie on 5/10/17.
 * parse the url and translate it for mongodb
 */
public interface IdentifierHandler {

    String getIdentifier(String relativePath, boolean directory);
    String getIdentifier(String databse, String collection);
    public String getRelativePath(String identifier, boolean directory);
    Map<String,String> getParameters(String identifier);
}
