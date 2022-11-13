package io.github.jonarzz.restaurant.knowledge;

import org.springframework.context.annotation.*;
import software.amazon.awssdk.services.dynamodb.*;

import io.github.jonarzz.restaurant.knowledge.api.*;
import io.github.jonarzz.restaurant.knowledge.domain.*;

@Configuration
public class RestaurantDomainConfig {

    @Bean
    public RestaurantController restaurantController(RestaurantService restaurantService) {
        return new RestaurantController(restaurantService);
    }

    @Bean
    public RestaurantService restaurantDynamoDbService(RestaurantRepository restaurantRepository) {
        return new RestaurantDynamoDbService(restaurantRepository);
    }

    @Bean
    public RestaurantRepository restaurantRepository(DynamoDbClient dynamoDbClient) {
        return new RestaurantRepository(dynamoDbClient);
    }

}
