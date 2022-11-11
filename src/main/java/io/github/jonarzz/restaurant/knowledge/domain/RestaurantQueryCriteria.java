package io.github.jonarzz.restaurant.knowledge.domain;

import lombok.*;

import io.github.jonarzz.restaurant.knowledge.model.*;

@Builder
public record RestaurantQueryCriteria(
        String nameBeginsWith,
        Category category,
        Boolean triedBefore,
        Integer ratingAtLeast
) {

    public boolean isEmpty() {
        return nameBeginsWith == null
               && category == null
               && triedBefore == null
               && ratingAtLeast == null;
    }
}
