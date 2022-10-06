package io.github.jonarzz.restaurant.knowledge.domain;

import static io.github.jonarzz.restaurant.knowledge.domain.RestaurantKey.*;
import static java.lang.Boolean.*;

import lombok.*;

import java.util.*;

import io.github.jonarzz.restaurant.knowledge.model.*;
import io.github.jonarzz.restaurant.knowledge.technical.dynamodb.*;

@Builder
record RestaurantItem(
        String userId,
        String restaurantName,
        @Singular Set<Category> categories,
        boolean triedBefore,
        Integer rating,
        String review,
        @Singular List<String> notes
) implements DynamoDbTable<RestaurantKey> {

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

    RestaurantItem {
        if (restaurantName == null || restaurantName.isBlank()) {
            throw new IllegalArgumentException("Restaurant name cannot be blank");
        }
        if (categories == null || categories.isEmpty()) {
            throw new IllegalArgumentException("At least one restaurant category is required");
        }
    }

    static RestaurantItem from(RestaurantData restaurant) {
        return RestaurantItem.builder()
                             .userId(contextUserId())
                             .restaurantName(restaurant.getName())
                             .categories(restaurant.getCategories())
                             .triedBefore(TRUE.equals(restaurant.getTriedBefore()))
                             .rating(restaurant.getRating())
                             .review(restaurant.getReview())
                             .notes(Optional.ofNullable(restaurant.getNotes())
                                            .orElseGet(ArrayList::new))
                             .build();
    }

    @Override
    public RestaurantKey getKey() {
        return new RestaurantKey(userId, restaurantName);
    }

    RestaurantData data() {
        return new RestaurantData()
                .name(restaurantName)
                .categories(categories)
                .triedBefore(triedBefore)
                .rating(rating)
                .review(review)
                .notes(notes);
    }

    RestaurantItem renamedTo(String newName) {
        return RestaurantItem.builder()
                             .restaurantName(newName)
                             .userId(userId)
                             .categories(categories)
                             .triedBefore(triedBefore)
                             .rating(rating)
                             .review(review)
                             .notes(notes)
                             .build();
    }

    RestaurantItem markedAsTriedBefore() {
        return RestaurantItem.builder()
                             .triedBefore(true)
                             .restaurantName(restaurantName)
                             .userId(userId)
                             .categories(categories)
                             .rating(rating)
                             .review(review)
                             .notes(notes)
                             .build();
    }

    String ratingString() {
        return Optional.ofNullable(rating)
                       .map(String::valueOf)
                       .orElse(null);
    }

}

