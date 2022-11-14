package io.github.jonarzz.restaurant.knowledge.domain;

import static io.github.jonarzz.restaurant.knowledge.technical.auth.SecurityContext.*;
import static java.lang.Boolean.*;

import lombok.*;
import lombok.experimental.*;

import java.util.*;

import io.github.jonarzz.restaurant.knowledge.technical.dynamodb.*;

@Builder
@Getter
@Accessors(fluent = true)
@ToString
class RestaurantItem implements DynamoDbTable<RestaurantKey> {

    private final String userId;
    private final String restaurantName;
    @Singular
    private final Set<Category> categories;
    private final boolean triedBefore;
    private final Integer rating;
    private final String review;
    @Singular
    private final List<String> notes;

    static class Attributes {

        static final String USER_ID = "userId";
        static final String NAME_LOWERCASE = "nameLowercase";
        static final String RESTAURANT_NAME = "restaurantName";
        static final String CATEGORIES = "categories";
        static final String TRIED_BEFORE = "triedBefore";
        static final String RATING = "rating";
        static final String REVIEW = "review";
        static final String NOTES = "notes";

        private Attributes() {
        }
    }

    static RestaurantItem from(RestaurantData restaurant) {
        return RestaurantItem.builder()
                             .userId(getUserId())
                             .restaurantName(restaurant.name())
                             .categories(restaurant.categories())
                             .triedBefore(TRUE.equals(restaurant.triedBefore()))
                             .rating(restaurant.rating())
                             .review(restaurant.review())
                             .notes(Optional.ofNullable(restaurant.notes())
                                            .orElseGet(ArrayList::new))
                             .build();
    }

    RestaurantData data() {
        return new RestaurantData()
                .name(restaurantName)
                .categories(categories)
                .triedBefore(triedBefore)
                .rating(Optional.ofNullable(rating)
                                // rating = 0 - empty value in DynamoDB
                                .filter(value -> value > 0)
                                .orElse(null))
                .review(review != null && !review.isBlank() ? review : null)
                .notes(notes);
    }

    @Override
    public RestaurantKey getKey() {
        return new RestaurantKey(userId, restaurantName);
    }

    String ratingString() {
        return Optional.ofNullable(rating)
                       .map(String::valueOf)
                       .orElse(null);
    }

}

