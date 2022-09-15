package io.github.jonarzz.restaurant.knowledge;

import static software.amazon.awssdk.regions.Region.*;

import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.services.dynamodb.*;

@Configuration
class DynamoDbConfig {

    private final String amazonAwsAccessKey;
    private final String amazonAwsSecretKey;

    DynamoDbConfig(@Value("${amazon.aws.accesskey}") String amazonAwsAccessKey,
                   @Value("${amazon.aws.secretkey}") String amazonAwsSecretKey) {
        this.amazonAwsAccessKey = amazonAwsAccessKey;
        this.amazonAwsSecretKey = amazonAwsSecretKey;
    }

    @Bean
    DynamoDbClient amazonDynamoDb() {
        return DynamoDbClient.builder()
                             .credentialsProvider(awsCredentialsProvider())
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

}
