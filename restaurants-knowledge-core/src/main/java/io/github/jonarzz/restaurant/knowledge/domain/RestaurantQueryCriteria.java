package io.github.jonarzz.restaurant.knowledge.domain;

import lombok.*;
import lombok.experimental.*;

@Builder
@Getter
@Accessors(fluent = true)
@ToString
@EqualsAndHashCode
public class RestaurantQueryCriteria {

    private final String nameBeginsWith;
    private final Category category;
    private final Boolean triedBefore;
    private final Integer ratingAtLeast;

    public boolean isEmpty() {
        return nameBeginsWith == null
               && category == null
               && triedBefore == null
               && ratingAtLeast == null;
    }

}
