package com.emc.ecs.sync.storage.mongodb.io;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.bson.Document;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by charlie on 5/5/17.
 * MongoDBInputStream provide the source stream for other steams.
 * every collection can regard as the file in FS,if the collection is over ,read() will return -1,read completed.
 */
public class MongoDBInputStream extends InputStream{

    MongoCollection<Document> collection;
    //a record in collection
    private byte [] row;
    //the position next to be read;
    private int position;
    //current row length
    private int bufferLength;
    //tag true if collection is over.
    private boolean isEnd;
    MongoCursor<Document> iterator;
    public MongoDBInputStream(MongoCollection<Document> collection){
        if(collection==null){
            throw new RuntimeException("the collection is null");
        }
        this.collection = collection;
        iterator = collection.find().iterator();
    }
    @Override
    public int read() throws IOException {
        if(row==null||row.length==0||position>=bufferLength){
            readData();
        }
        if(isEnd){
            return -1;
        }
        return row[position++];
    }
    private void readData(){
        isEnd = !iterator.hasNext();
        if(!isEnd){
            Document next = iterator.next();
            String s = next.toJson() + "\n";
            row = s.getBytes();
            position = 0;
            bufferLength = row.length;
        }else{
            row = null;
        }
    }
}
