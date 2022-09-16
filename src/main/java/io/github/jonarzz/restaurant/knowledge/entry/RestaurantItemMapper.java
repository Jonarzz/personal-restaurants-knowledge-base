package io.github.jonarzz.restaurant.knowledge.entry;

import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

import io.github.jonarzz.restaurant.knowledge.dynamodb.*;
import io.github.jonarzz.restaurant.knowledge.model.*;

class RestaurantItemMapper implements ItemMapper<RestaurantItem> {

    @Override
    public RestaurantItem createItem(ItemExtractor extractor) {
        return RestaurantItem.builder()
                             .userId(extractor.string("userId"))
                             .restaurantName(extractor.string("restaurantName"))
                             .categories(extractor.set("categories", Category::valueOf))
                             .triedBefore(extractor.bool("triedBefore"))
                             .rating(extractor.integer("rating"))
                             .review(extractor.string("review"))
                             .notes(extractor.list("notes"))
                             .build();
    }

    @Override
    public Map<String, AttributeValue> createAttributes(RestaurantItem restaurant) {
        return new AttributesCreator()
                .putIfPresent("userId",         restaurant.userId(),         AttributeValue::fromS)
                .putIfPresent("restaurantName", restaurant.restaurantName(), AttributeValue::fromS)
                .putIfPresent("triedBefore",    restaurant.triedBefore(),    AttributeValue::fromBool)
                .putIfPresent("review",         restaurant.review(),         AttributeValue::fromS)
                .putIfPresent("rating",         restaurant.ratingString(),   AttributeValue::fromN)
                .putIfNotEmpty("notes",      restaurant.notes())
                .putIfNotEmpty("categories", restaurant.categories(), Category::getValue)
                .create();
    }
}
