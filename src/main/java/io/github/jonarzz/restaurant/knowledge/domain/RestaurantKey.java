package io.github.jonarzz.restaurant.knowledge.domain;

import static io.github.jonarzz.restaurant.knowledge.domain.RestaurantItem.Attributes.*;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.*;

import org.springframework.security.core.context.*;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

import io.github.jonarzz.restaurant.knowledge.technical.dynamodb.*;

record RestaurantKey(
        String userId,
        String nameLowercase
) implements DynamoDbKey {

    RestaurantKey(String userId, String nameLowercase) {
        if (userId == null) {
            userId = contextUserId();
        }
        this.userId = userId;
        this.nameLowercase = nameLowercase.toLowerCase();
    }

    RestaurantKey(String restaurantName) {
        this(null, restaurantName);
    }

    @Override
    public Map<String, AttributeValue> asAttributes() {
        return Map.of(
                USER_ID, fromS(userId),
                NAME_LOWERCASE, fromS(nameLowercase)
        );
    }

    static String contextUserId() {
        return SecurityContextHolder.getContext()
                                    .getAuthentication()
                                    .getName();
    }

}
