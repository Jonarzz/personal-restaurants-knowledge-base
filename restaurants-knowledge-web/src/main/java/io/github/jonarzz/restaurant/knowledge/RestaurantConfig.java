package io.github.jonarzz.restaurant.knowledge;

import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import software.amazon.awssdk.services.dynamodb.*;

import java.util.*;
import java.util.function.*;

import io.github.jonarzz.restaurant.knowledge.api.*;
import io.github.jonarzz.restaurant.knowledge.domain.*;
import io.github.jonarzz.restaurant.knowledge.technical.dynamodb.*;

@Configuration
@RequiredArgsConstructor
public class RestaurantConfig {

    @Bean
    public RestaurantController restaurantController(RestaurantService restaurantService) {
        return new RestaurantController(restaurantService);
    }

    @Bean
    CachingRestaurantService restaurantService(DynamoDbClient dynamoDbClient,
                                               Optional<Consumer<RestaurantService>> decoratedServiceWrapper) {
        DynamoDbRepository.createTablesOnInstantiation = true;
        var factory = restaurantDomainDynamoDbFactory(dynamoDbClient);
        var restaurantRepository = factory.restaurantDynamoDbRepository();
        var restaurantService = factory.restaurantDynamoDbService(restaurantRepository);
        decoratedServiceWrapper.ifPresent(wrapper -> wrapper.accept(restaurantService));
        return new CachingRestaurantService(restaurantService);
    }

    @Bean
    RestaurantDomainFactory restaurantDomainDynamoDbFactory(DynamoDbClient dynamoDbClient) {
        return new RestaurantDomainFactory(dynamoDbClient);
    }

    @Bean
    DynamoDbClient dynamoDbClient(@Value("${amazon.aws.accesskey}") String amazonAwsAccessKey,
                                  @Value("${amazon.aws.secretkey}") String amazonAwsSecretKey,
                                  @Value("${amazon.aws.dynamodb-url:#{null}}") String dynamoDbUrl) {
        var factory = new DynamoDbClientFactory();
        return factory.amazonDynamoDb(amazonAwsAccessKey, amazonAwsSecretKey, dynamoDbUrl);
    }

}
