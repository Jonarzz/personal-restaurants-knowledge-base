package io.github.jonarzz.restaurant.knowledge.domain;

import lombok.*;
import software.amazon.awssdk.services.dynamodb.*;

@RequiredArgsConstructor
public class RestaurantDomainFactory {

    private final DynamoDbClient dynamoDbClient;

    public RestaurantService restaurantDynamoDbService(RestaurantDynamoDbRepository restaurantRepository) {
        return new RestaurantDynamoDbService(restaurantRepository);
    }

    public RestaurantDynamoDbRepository restaurantDynamoDbRepository() {
        return new RestaurantDynamoDbRepository(dynamoDbClient);
    }

}
