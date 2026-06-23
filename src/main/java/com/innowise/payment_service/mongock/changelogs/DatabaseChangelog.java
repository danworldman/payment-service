package com.innowise.payment_service.mongock.changelogs;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import java.util.ArrayList;

@ChangeUnit(id = "initPaymentsCollectionAndIndexes", order = "001", author = "danworldman")
public class DatabaseChangelog {

    private static final String COLLECTION_NAME = "payments";

    @Execution
    public void execution(MongoDatabase database) {
        if (!collectionExists(database, COLLECTION_NAME)) {
            database.createCollection(COLLECTION_NAME);
        }

        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
        collection.createIndex(new Document("order_id", 1));
        collection.createIndex(new Document("user_id", 1));
        collection.createIndex(new Document("status", 1));
    }

    @RollbackExecution
    public void rollback(MongoDatabase database) {
        database.getCollection(COLLECTION_NAME).drop();
    }

    private boolean collectionExists(MongoDatabase database, String collectionName) {
        return database.listCollectionNames()
                .into(new ArrayList<>())
                .contains(collectionName);
    }
}
