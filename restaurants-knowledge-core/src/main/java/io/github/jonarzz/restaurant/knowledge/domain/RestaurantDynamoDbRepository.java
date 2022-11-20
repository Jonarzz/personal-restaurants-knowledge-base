package io.github.jonarzz.restaurant.knowledge.domain;

import static software.amazon.awssdk.services.dynamodb.model.KeyType.*;
import static software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType.*;

import software.amazon.awssdk.services.dynamodb.*;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

import io.github.jonarzz.restaurant.knowledge.technical.dynamodb.*;

class RestaurantDynamoDbRepository extends DynamoDbRepository<RestaurantItem, RestaurantKey> {

    private static final String TABLE_NAME = "Restaurant";

    RestaurantDynamoDbRepository(DynamoDbClient client) {
        super(TABLE_NAME, new RestaurantItemMapper(), client);
    }

    @Override
    public Optional<RestaurantItem> findByKey(RestaurantKey key) {
        return super.findByKey(key);
    }

    @Override
    public void create(RestaurantItem item) {
        super.create(item);
    }

    @Override
    public void update(RestaurantItem item, Map<String, AttributeValueUpdate> updates) {
        super.update(item, updates);
    }

    @Override
    public void update(RestaurantItem item, AttributesCreator attributesCreator) {
        super.update(item, attributesCreator);
    }

    @Override
    public void delete(RestaurantItem item) {
        super.delete(item);
    }

    @Override
    protected CreateTableRequest prepareCreateTableRequest() {
        var userIdAttribute = "userId";
        var nameLowercaseAttribute = "nameLowercase";
        return CreateTableRequest.builder()
                                 .tableName(TABLE_NAME)
                                 .keySchema(
                                         KeySchemaElement.builder()
                                                         .attributeName(userIdAttribute)
                                                         .keyType(HASH)
                                                         .build(),
                                         KeySchemaElement.builder()
                                                         .attributeName(nameLowercaseAttribute)
                                                         .keyType(RANGE)
                                                         .build()
                                 )
                                 .attributeDefinitions(
                                         AttributeDefinition.builder()
                                                            .attributeName(userIdAttribute)
                                                            .attributeType(S)
                                                            .build(),
                                         AttributeDefinition.builder()
                                                            .attributeName(nameLowercaseAttribute)
                                                            .attributeType(S)
                                                            .build()
                                 )
                                 .provisionedThroughput(ProvisionedThroughput.builder()
                                                                             .readCapacityUnits(1L)
                                                                             .writeCapacityUnits(1L)
                                                                             .build())
                                 .build();
    }
}
