package io.github.jonarzz.restaurant.knowledge.technical.dynamodb;

import software.amazon.awssdk.services.dynamodb.*;

public class DynamoDbTestUtil {

    private DynamoDbTestUtil() {
    }

    public static DynamoDbClient createClient(String dynamoDbUrl) {
        var clientFactory = new DynamoDbClientFactory();
        var credentialsProvider = clientFactory.awsCredentialsProvider(
                clientFactory.awsCredentials("testaccess", "testsecret")
        );
        return clientFactory.amazonDynamoDb(credentialsProvider, dynamoDbUrl);
    }

    public static void createTableFor(DynamoDbRepository<?, ?> repository) {
        repository.createTable();
    }

}
