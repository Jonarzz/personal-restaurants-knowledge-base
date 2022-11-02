package io.github.jonarzz.restaurant.knowledge.domain;

import static io.github.jonarzz.restaurant.knowledge.domain.RestaurantItem.Attributes.*;
import static java.util.Optional.*;

import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

import io.github.jonarzz.restaurant.knowledge.model.*;
import io.github.jonarzz.restaurant.knowledge.technical.dynamodb.*;

class RestaurantModification {

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
        var newKey = new RestaurantKey(updateData.getName());
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

    class Changes {

        private String restaurantName;
        private String review;
        private String rating;
        private Boolean triedBefore;
        private List<String> notes;
        private Set<Category> categories;

        private boolean empty = true;

        Changes() {
            var updatedName = updateData.getName();
            if (updatedName != null && !base.restaurantName().equals(updatedName)) {
                restaurantName = updatedName;
                empty = false;
            }
            var updatedReview = updateData.getReview();
            if (updatedReview != null && !base.review().equals(updatedReview)) {
                review = updatedReview;
                empty = false;
            }
            var updatedRating = updateData.getRating();
            if (updatedRating != null && !base.rating().equals(updatedRating)) {
                rating = String.valueOf(updatedRating);
                empty = false;
            }
            var updatedTriedBefore = updateData.getTriedBefore();
            if (updatedTriedBefore != null && base.triedBefore() != updatedTriedBefore) {
                triedBefore = updatedTriedBefore;
                empty = false;
            }
            var updatedNotes = updateData.getNotes();
            if (updatedNotes != null && !base.notes().equals(updatedNotes)) {
                notes = updatedNotes;
                empty = false;
            }
            var updatedCategories = updateData.getCategories();
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
            return creator.putIfPresent(REVIEW, review, AttributeValue::fromS)
                          .putIfPresent(RATING, rating, AttributeValue::fromN)
                          .putIfPresent(TRIED_BEFORE, triedBefore, AttributeValue::fromBool)
                          .putIfNotEmpty(NOTES, notes)
                          .putIfNotEmpty(CATEGORIES, categories, Category::getValue);
        }

        RestaurantItem applied() {
            return RestaurantItem.builder()
                                 .userId(base.userId())
                                 .restaurantName(ofNullable(restaurantName)
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
                                                .orElseGet(base::notes))
                                 .build();
        }
    }
}
