package io.github.jonarzz.restaurant.knowledge.technical.dynamodb;

import static software.amazon.awssdk.regions.Region.*;

import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.services.dynamodb.*;

import java.net.*;

@Configuration
public class DynamoDbClientFactory {

    @Bean
    public DynamoDbClient amazonDynamoDb(AwsCredentialsProvider awsCredentialsProvider,
                                         @Value("${amazon.aws.dynamodb-url:#{null}}") String dynamoDbUrl) {
        var builder = DynamoDbClient.builder();
        if (dynamoDbUrl != null) {
            builder.endpointOverride(URI.create(dynamoDbUrl));
        }
        return builder.credentialsProvider(awsCredentialsProvider)
                      .region(EU_CENTRAL_1)
                      .build();
    }

    @Bean
    AwsCredentialsProvider awsCredentialsProvider(AwsCredentials awsCredentials) {
        return StaticCredentialsProvider.create(awsCredentials);
    }

    @Bean
    AwsCredentials awsCredentials(@Value("${amazon.aws.accesskey}") String amazonAwsAccessKey,
                                  @Value("${amazon.aws.secretkey}") String amazonAwsSecretKey) {
        return AwsBasicCredentials.create(amazonAwsAccessKey, amazonAwsSecretKey);
    }

}
