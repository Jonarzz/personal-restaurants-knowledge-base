package io.github.jonarzz.restaurant.knowledge.entry;

import org.springframework.context.annotation.*;
import software.amazon.awssdk.services.dynamodb.*;

import io.github.jonarzz.restaurant.knowledge.dynamodb.*;

@Configuration
public class RestaurantEntryManagementConfig {

    private static final String TABLE_NAME = "Restaurant";

    @Bean
    public RestaurantController restaurantController(RestaurantService restaurantService) {
        return new RestaurantController(restaurantService);
    }

    @Bean
    RestaurantService restaurantService(DynamoDbRepository<RestaurantItem, RestaurantKey> restaurantRepository) {
        return new RestaurantService(restaurantRepository);
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
