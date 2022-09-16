package io.github.jonarzz.restaurant.knowledge.entry;

import static io.github.jonarzz.restaurant.knowledge.entry.RestaurantItem.Fields.*;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.*;

import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

import io.github.jonarzz.restaurant.knowledge.dynamodb.*;

record RestaurantKey(
        String userId,
        String restaurantName
) implements DynamoDbKey {

    @Override
    public Map<String, AttributeValue> asAttributes() {
        return Map.of(
                USER_ID, fromS(userId),
                RESTAURANT_NAME, fromS(restaurantName)
        );
    }

}
