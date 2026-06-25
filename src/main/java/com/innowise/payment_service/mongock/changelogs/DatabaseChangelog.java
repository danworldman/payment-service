package com.innowise.payment_service.mongock.changelogs;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@ChangeUnit(id = "initPaymentsCollectionAndIndexes", order = "001", author = "danworldman")
public class DatabaseChangelog {

    private static final String COLLECTION_NAME = "payments";

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        if (!mongoTemplate.collectionExists(COLLECTION_NAME)) {
            mongoTemplate.createCollection(COLLECTION_NAME);
        }

        mongoTemplate.indexOps(COLLECTION_NAME).createIndex(new Index().on("user_id", Sort.Direction.ASC));
        mongoTemplate.indexOps(COLLECTION_NAME).createIndex(new Index().on("order_id", Sort.Direction.ASC));
        mongoTemplate.indexOps(COLLECTION_NAME).createIndex(new Index().on("status", Sort.Direction.ASC));
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        if (mongoTemplate.collectionExists(COLLECTION_NAME)) {
            mongoTemplate.dropCollection(COLLECTION_NAME);
        }
    }
}