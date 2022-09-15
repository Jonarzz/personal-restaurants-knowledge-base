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
        return RestaurantRow.builder()
                            .build();
    }

    String ratingString() {
        return Optional.ofNullable(rating)
                       .map(String::valueOf)
                       .orElse(null);
    }

    RestaurantData data() {
        // TODO
        return new RestaurantData();
    }
}

