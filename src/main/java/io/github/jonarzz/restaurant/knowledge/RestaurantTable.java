package io.github.jonarzz.restaurant.knowledge;

import static lombok.AccessLevel.*;

import lombok.*;
import lombok.experimental.*;

import java.util.*;

import io.github.jonarzz.restaurant.knowledge.model.*;

@Data
@Setter(NONE)
@Builder
@Accessors(fluent = true)
class RestaurantTable {

    private String userId;
    private String restaurantName;
    @Singular
    private Set<Category> categories;
    private boolean triedBefore;
    private Integer rating;
    private String review;
    @Singular
    private List<String> notes;

    String ratingString() {
        return Optional.ofNullable(rating)
                       .map(String::valueOf)
                       .orElse(null);
    }

}

