package io.github.jonarzz.restaurant.knowledge.entry;

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
                "userId", fromS(userId),
                "restaurantName", fromS(restaurantName)
        );
    }

}
