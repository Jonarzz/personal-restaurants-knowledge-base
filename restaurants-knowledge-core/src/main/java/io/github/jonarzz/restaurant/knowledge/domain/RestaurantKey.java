package io.github.jonarzz.restaurant.knowledge.domain;

import static io.github.jonarzz.restaurant.knowledge.domain.RestaurantItem.Attributes.*;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.*;

import lombok.*;
import lombok.experimental.*;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

import io.github.jonarzz.restaurant.knowledge.technical.auth.*;
import io.github.jonarzz.restaurant.knowledge.technical.dynamodb.*;

@Getter
@Accessors(fluent = true)
@ToString
@EqualsAndHashCode
class RestaurantKey implements DynamoDbKey {

    private final String userId;
    private final String nameLowercase;

    RestaurantKey(String userId, String name) {
        if (userId == null) {
            userId = SecurityContext.getUserId();
        }
        this.userId = userId;
        nameLowercase = name.toLowerCase();
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

}
