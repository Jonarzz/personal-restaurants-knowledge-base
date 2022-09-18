package io.github.jonarzz.restaurant.knowledge.domain;

import lombok.*;

import io.github.jonarzz.restaurant.knowledge.model.*;

@Builder
record RestaurantQueryCriteria(
        String nameBeginsWith,
        Category category,
        Boolean triedBefore,
        Integer ratingAtLeast
) {

    boolean isEmpty() {
        return nameBeginsWith == null
               && category == null
               && triedBefore == null
               && ratingAtLeast == null;
    }
}
