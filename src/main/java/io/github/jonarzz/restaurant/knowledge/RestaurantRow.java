package io.github.jonarzz.restaurant.knowledge;

import static java.lang.Boolean.*;
import static lombok.AccessLevel.*;

import lombok.*;
import lombok.experimental.*;
import org.springframework.security.core.context.*;

import java.util.*;

import io.github.jonarzz.restaurant.knowledge.model.*;

@Data
@Setter(NONE)
@Builder
@Accessors(fluent = true)
class RestaurantRow {

    private String userId;
    private String restaurantName;
    @Singular
    private Set<Category> categories;
    private boolean triedBefore;
    private Integer rating;
    private String review;
    @Singular
    private List<String> notes;

    static RestaurantRow from(RestaurantData restaurant) {
        var name = restaurant.getName();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Restaurant name cannot be blank");
        }
        var categories = restaurant.getCategories();
        if (categories == null || categories.isEmpty()) {
            throw new IllegalArgumentException("At least one restaurant category is required");
        }
        return RestaurantRow.builder()
                            .userId(SecurityContextHolder.getContext()
                                                         .getAuthentication()
                                                         .getName())
                            .restaurantName(name)
                            .categories(categories)
                            .triedBefore(TRUE.equals(restaurant.getTriedBefore()))
                            .rating(restaurant.getRating())
                            .review(restaurant.getReview())
                            .notes(Optional.ofNullable(restaurant.getNotes())
                                           .orElseGet(ArrayList::new))
                            .build();
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

    RestaurantRow renamedTo(String newName) {
        return RestaurantRow.builder()
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

