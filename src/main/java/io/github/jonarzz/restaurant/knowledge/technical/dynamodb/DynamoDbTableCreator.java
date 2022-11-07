package io.github.jonarzz.restaurant.knowledge.technical.dynamodb;

import static software.amazon.awssdk.services.dynamodb.model.KeyType.*;
import static software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType.*;

import lombok.extern.slf4j.*;
import software.amazon.awssdk.services.dynamodb.*;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.annotation.*;

@Slf4j
class DynamoDbTableCreator {

    private static final String RESTAURANT_TABLE_NAME = "Restaurant";

    private final DynamoDbClient dynamoDbClient;

    DynamoDbTableCreator(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    @PostConstruct
    void create() {
        try {
            dynamoDbClient.createTable(prepareCreateRestaurantTableRequest());
        } catch (ResourceInUseException exception) {
            log.info("Tried to create table {}, but it already exists", RESTAURANT_TABLE_NAME);
        }
    }

    private static CreateTableRequest prepareCreateRestaurantTableRequest() {
        var userIdAttribute = "userId";
        var nameLowercaseAttribute = "nameLowercase";
        return CreateTableRequest.builder()
                                 .tableName(RESTAURANT_TABLE_NAME)
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
