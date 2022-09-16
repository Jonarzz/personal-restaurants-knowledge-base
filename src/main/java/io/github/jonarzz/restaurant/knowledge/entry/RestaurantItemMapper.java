package io.github.jonarzz.restaurant.knowledge.entry;

import static io.github.jonarzz.restaurant.knowledge.entry.RestaurantItem.Fields.*;

import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

import io.github.jonarzz.restaurant.knowledge.dynamodb.*;
import io.github.jonarzz.restaurant.knowledge.model.*;

class RestaurantItemMapper implements ItemMapper<RestaurantItem> {

    @Override
    public RestaurantItem createItem(ItemExtractor extractor) {
        return RestaurantItem.builder()
                             .userId(extractor.string(USER_ID))
                             .restaurantName(extractor.string(RESTAURANT_NAME))
                             .categories(extractor.set(CATEGORIES, Category::valueOf))
                             .triedBefore(extractor.bool(TRIED_BEFORE))
                             .rating(extractor.integer(RATING))
                             .review(extractor.string(REVIEW))
                             .notes(extractor.list(NOTES))
                             .build();
    }

    @Override
    public Map<String, AttributeValue> createAttributes(RestaurantItem restaurant) {
        return new AttributesCreator()
                .putIfPresent(USER_ID,         restaurant.userId(),         AttributeValue::fromS)
                .putIfPresent(RESTAURANT_NAME, restaurant.restaurantName(), AttributeValue::fromS)
                .putIfPresent(REVIEW,          restaurant.review(),         AttributeValue::fromS)
                .putIfPresent(RATING,          restaurant.ratingString(),   AttributeValue::fromN)
                .putIfPresent(TRIED_BEFORE,    restaurant.triedBefore(),    AttributeValue::fromBool)
                .putIfNotEmpty(NOTES,      restaurant.notes())
                .putIfNotEmpty(CATEGORIES, restaurant.categories(), Category::getValue)
                .create();
    }
}