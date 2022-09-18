package io.github.jonarzz.restaurant.knowledge.domain;

import static io.github.jonarzz.restaurant.knowledge.domain.RestaurantItem.Fields.*;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.*;

import org.springframework.security.core.context.*;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

import io.github.jonarzz.restaurant.knowledge.technical.dynamodb.*;

record RestaurantKey(
        String userId,
        String restaurantName
) implements DynamoDbKey {

    RestaurantKey {
        if (userId == null) {
            userId = contextUserId();
        }
    }

    RestaurantKey(String restaurantName) {
        this(null, restaurantName);
    }

    @Override
    public Map<String, AttributeValue> asAttributes() {
        return Map.of(
                USER_ID, fromS(userId),
                RESTAURANT_NAME, fromS(restaurantName)
        );
    }

    static String contextUserId() {
        return SecurityContextHolder.getContext()
                                    .getAuthentication()
                                    .getName();
    }

}
