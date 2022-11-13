package io.github.jonarzz.restaurant.knowledge.domain;

import static io.github.jonarzz.restaurant.knowledge.domain.RestaurantItem.Attributes.*;

import software.amazon.awssdk.services.dynamodb.model.*;

import io.github.jonarzz.restaurant.knowledge.technical.dynamodb.*;

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
    public AttributesCreator attributesCreator(RestaurantItem restaurant) {
        var restaurantName = restaurant.restaurantName();
        return new AttributesCreator()
                .putIfPresent(USER_ID, restaurant.userId(), AttributeValue::fromS)
                .putIfPresent(NAME_LOWERCASE,  restaurantName.toLowerCase(), AttributeValue::fromS)
                .putIfPresent(RESTAURANT_NAME, restaurantName,               AttributeValue::fromS)
                .putIfPresent(REVIEW,          restaurant.review(),          AttributeValue::fromS)
                .putIfPresent(RATING,          restaurant.ratingString(),    AttributeValue::fromN)
                .putIfPresent(TRIED_BEFORE,    restaurant.triedBefore(),     AttributeValue::fromBool)
                .putIfNotEmpty(NOTES,      restaurant.notes())
                .putIfNotEmpty(CATEGORIES, restaurant.categories(), Category::getValue);
    }
}
