package io.github.jonarzz.restaurant.knowledge.domain;

import org.springframework.context.annotation.*;
import software.amazon.awssdk.services.dynamodb.*;

@Configuration
public class RestaurantDomainFactory {

    @Bean
    public RestaurantService restaurantDynamoDbService(RestaurantRepository restaurantRepository) {
        return new RestaurantDynamoDbService(restaurantRepository);
    }

    @Bean
    RestaurantRepository restaurantRepository(DynamoDbClient dynamoDbClient) {
        return new RestaurantRepository(dynamoDbClient);
    }

}
