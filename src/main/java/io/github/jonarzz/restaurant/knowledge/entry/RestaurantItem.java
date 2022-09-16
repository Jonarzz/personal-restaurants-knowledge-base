package io.github.jonarzz.restaurant.knowledge.entry;

import static java.lang.Boolean.*;

import lombok.*;
import org.springframework.security.core.context.*;

import java.util.*;

import io.github.jonarzz.restaurant.knowledge.dynamodb.*;
import io.github.jonarzz.restaurant.knowledge.model.*;

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

    static class Fields {

        static final String USER_ID = "userId";
        static final String RESTAURANT_NAME = "restaurantName";
        static final String CATEGORIES = "categories";
        static final String TRIED_BEFORE = "triedBefore";
        static final String RATING = "rating";
        static final String REVIEW = "review";
        static final String NOTES = "notes";

        private Fields() {
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
                             .userId(SecurityContextHolder.getContext()
                                                         .getAuthentication()
                                                         .getName())
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

    String ratingString() {
        return Optional.ofNullable(rating)
                       .map(String::valueOf)
                       .orElse(null);
    }

}

