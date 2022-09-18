package io.github.jonarzz.restaurant.knowledge.domain;

import org.springframework.context.annotation.*;
import software.amazon.awssdk.services.dynamodb.*;

import io.github.jonarzz.restaurant.knowledge.technical.dynamodb.*;

@Configuration
public class RestaurantEntryManagementConfig {

    private static final String TABLE_NAME = "Restaurant";

    @Bean
    public RestaurantController restaurantController(RestaurantService restaurantService) {
        return new RestaurantController(restaurantService);
    }

    @Bean
    RestaurantService restaurantDynamoDbService(DynamoDbRepository<RestaurantItem, RestaurantKey> restaurantRepository) {
        return new RestaurantDynamoDbService(restaurantRepository);
    }

    @Bean
    DynamoDbRepository<RestaurantItem, RestaurantKey> restaurantRepository(DynamoDbClient dynamoDbClient) {
        return new DynamoDbRepository<>(TABLE_NAME, restaurantItemMapper(), dynamoDbClient);
    }

    @Bean
    RestaurantItemMapper restaurantItemMapper() {
        return new RestaurantItemMapper();
    }

}
