package io.github.jonarzz.restaurant.knowledge.domain;

import static io.github.jonarzz.restaurant.knowledge.domain.RestaurantItem.Attributes.*;
import static java.lang.Boolean.*;
import static java.util.Optional.*;
import static java.util.function.Predicate.*;
import static java.util.stream.Collectors.*;

import lombok.*;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

import io.github.jonarzz.restaurant.knowledge.technical.dynamodb.*;

class RestaurantModification {

    static final AttributeValue EMPTY_REVIEW = AttributeValue.fromS("");
    static final AttributeValue EMPTY_RATING = AttributeValue.fromN("0");

    private final RestaurantItem base;
    private final RestaurantData updateData;

    private final Changes changes;

    RestaurantModification(RestaurantItem base, RestaurantData updateData) {
        this.base = base;
        this.updateData = updateData;
        changes = new Changes();
    }

    static Creator updateItem(RestaurantItem base) {
        return new Creator(base);
    }

    boolean hasNewKey() {
        var newName = updateData.name();
        if (newName == null) {
            return false;
        }
        var newKey = new RestaurantKey(newName);
        return !base.getKey().equals(newKey);
    }

    Changes changes() {
        return changes;
    }

    static class Creator {

        private RestaurantItem base;

        private Creator(RestaurantItem base) {
            this.base = base;
        }

        RestaurantModification with(RestaurantData updateData) {
            return new RestaurantModification(base, updateData);
        }
    }

    @ToString
    class Changes {

        private String restaurantName;
        private String review;
        private String rating;
        private Boolean triedBefore;
        private List<String> notes;
        private Set<Category> categories;

        private boolean empty = true;

        Changes() {
            var updatedName = updateData.name();
            if (updatedName != null && !base.restaurantName().equals(updatedName)) {
                restaurantName = updatedName;
                empty = false;
            }
            var updatedTriedBefore = updateData.triedBefore();
            if (updatedTriedBefore != null && base.triedBefore() != updatedTriedBefore) {
                triedBefore = updatedTriedBefore;
                empty = false;
            }
            var updatedReview = updateData.review();
            if (!Objects.equals(base.review(), updatedReview)) {
                review = updatedReview;
                empty = false;
            }
            var updatedRating = updateData.rating();
            if (!Objects.equals(base.rating(), updatedRating)) {
                rating = Optional.ofNullable(updatedRating)
                                 .map(Object::toString)
                                 .orElse(null);
                empty = false;
            }
            var updatedNotes = updateData.notes();
            if (updatedNotes != null && !updatedNotes.equals(base.notes())) {
                notes = updatedNotes.stream()
                                    .filter(not(String::isBlank))
                                    .collect(toList());
                empty = false;
            }
            var updatedCategories = updateData.categories();
            if (updatedCategories != null && !base.categories().equals(updatedCategories)) {
                categories = updatedCategories;
                empty = false;
            }
        }

        boolean empty() {
            return empty;
        }

        AttributesCreator toAttributesCreator() {
            var creator = new AttributesCreator();
            if (restaurantName != null) {
                creator.putIfPresent(RESTAURANT_NAME, restaurantName, AttributeValue::fromS)
                       .putIfPresent(NAME_LOWERCASE, restaurantName.toLowerCase(), AttributeValue::fromS);
            }
            if (FALSE.equals(triedBefore)) {
                creator.put(REVIEW, EMPTY_REVIEW)
                       .put(RATING, EMPTY_RATING);
            } else if (TRUE.equals(triedBefore) || base.triedBefore()) {
                creator.putIfPresent(REVIEW, review, AttributeValue::fromS)
                       .putIfPresent(RATING, rating, AttributeValue::fromN);
            }
            return creator.putIfPresent(TRIED_BEFORE, triedBefore, AttributeValue::fromBool)
                          .putIfNotEmpty(CATEGORIES, categories, Category::getValue)
                          .putOrEmpty(NOTES, notes);
        }

        RestaurantData applied() {
            return new RestaurantData()
                    .name(ofNullable(restaurantName)
                                  .orElseGet(base::restaurantName))
                    .categories(ofNullable(categories)
                                        .orElseGet(base::categories))
                    .triedBefore(ofNullable(triedBefore)
                                         .orElseGet(base::triedBefore))
                    .rating(ofNullable(rating)
                                    .map(Integer::valueOf)
                                    .orElseGet(base::rating))
                    .review(ofNullable(review)
                                    .orElseGet(base::review))
                    .notes(ofNullable(notes)
                                   .orElseGet(base::notes));
        }
    }
}
