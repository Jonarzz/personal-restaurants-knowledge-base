package io.github.jonarzz.restaurant.knowledge.technical.dynamodb;

import static software.amazon.awssdk.regions.Region.*;

import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.services.dynamodb.*;

import java.net.*;

@Configuration
public class DynamoDbConfig {

    private final String amazonAwsAccessKey;
    private final String amazonAwsSecretKey;
    private final String dynamoDbUrl;

    public DynamoDbConfig(@Value("${amazon.aws.accesskey}") String amazonAwsAccessKey,
                          @Value("${amazon.aws.secretkey}") String amazonAwsSecretKey,
                          @Value("${amazon.aws.dynamodb-url:#{null}}") String dynamoDbUrl) {
        this.amazonAwsAccessKey = amazonAwsAccessKey;
        this.amazonAwsSecretKey = amazonAwsSecretKey;
        this.dynamoDbUrl = dynamoDbUrl;
    }

    @Bean
    public DynamoDbClient amazonDynamoDb() {
        var builder = DynamoDbClient.builder();
        if (dynamoDbUrl != null) {
            builder.endpointOverride(URI.create(dynamoDbUrl));
        }
        return builder.credentialsProvider(awsCredentialsProvider())
                      .region(EU_CENTRAL_1)
                      .build();
    }

    @Bean
    AwsCredentialsProvider awsCredentialsProvider() {
        return StaticCredentialsProvider.create(awsCredentials());
    }

    @Bean
    AwsCredentials awsCredentials() {
        return AwsBasicCredentials.create(amazonAwsAccessKey, amazonAwsSecretKey);
    }

    @Bean
    DynamoDbTableCreator dynamoDbTableCreator() {
        return new DynamoDbTableCreator(amazonDynamoDb());
    }

}
