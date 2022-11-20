package io.github.jonarzz.restaurant.knowledge.technical.dynamodb;

import software.amazon.awssdk.services.dynamodb.*;

public class DynamoDbTestUtil {

    private DynamoDbTestUtil() {
    }

    public static void enableTableCreation() {
        DynamoDbRepository.createTablesOnInstantiation = true;
    }

    public static DynamoDbClient createClient(String dynamoDbUrl) {
        var clientFactory = new DynamoDbClientFactory();
        return clientFactory.amazonDynamoDb("testaccess", "testsecret", dynamoDbUrl);
    }
}
