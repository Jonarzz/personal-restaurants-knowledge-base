package io.github.jonarzz.restaurant.knowledge.technical.dynamodb;

import static software.amazon.awssdk.regions.Region.*;

import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.services.dynamodb.*;

import java.net.*;

public class DynamoDbClientFactory {

    public DynamoDbClient amazonDynamoDb(String amazonAwsAccessKey,
                                         String amazonAwsSecretKey) {
        return amazonDynamoDb(amazonAwsAccessKey, amazonAwsSecretKey, null);
    }

    public DynamoDbClient amazonDynamoDb(String amazonAwsAccessKey,
                                         String amazonAwsSecretKey,
                                         String dynamoDbUrl) {
        var builder = DynamoDbClient.builder();
        if (dynamoDbUrl != null) {
            builder.endpointOverride(URI.create(dynamoDbUrl));
        }
        var awsCredentials = AwsBasicCredentials.create(amazonAwsAccessKey, amazonAwsSecretKey);
        return builder.credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                      .region(EU_CENTRAL_1)
                      .build();
    }

}
