package io.github.jonarzz.restaurant.knowledge.domain;

import static io.github.jonarzz.restaurant.knowledge.domain.RestaurantItem.Attributes.*;
import static io.github.jonarzz.restaurant.knowledge.domain.RestaurantModification.*;
import static io.github.jonarzz.restaurant.knowledge.technical.dynamodb.AttributesCreator.*;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.*;

import lombok.extern.slf4j.*;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;
import java.util.stream.Stream;

import io.github.jonarzz.restaurant.knowledge.model.*;

@Slf4j
public class RestaurantDynamoDbService implements RestaurantService {

    private RestaurantRepository repository;

    public RestaurantDynamoDbService(RestaurantRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<RestaurantItem> fetch(String restaurantName) {
        log.debug("Fetching restaurant by name: {}", restaurantName);
        return repository.findByKey(new RestaurantKey(restaurantName));
    }

    @Override
    public List<RestaurantItem> query(RestaurantQueryCriteria criteria) {
        log.debug("Querying restaurant by criteria: {}", criteria);
        return repository.query(new RestaurantDynamoDbCriteria(criteria));
    }

    @Override
    public void create(RestaurantItem restaurant) {
        if (Stream.of(restaurant.rating(), restaurant.review())
                  .anyMatch(Objects::nonNull)) {
            restaurant = restaurant.markedAsTriedBefore();
        }
        log.debug("Creating {}", restaurant);
        repository.create(restaurant);
    }

    @Override
    public Optional<RestaurantItem> update(RestaurantItem restaurant, RestaurantData updateData) {
        var modification = updateItem(restaurant).with(updateData);
        var changes = modification.changes();
        if (changes.empty()) {
            log.debug("Skipping update of {} - no changes", restaurant);
            return Optional.empty();
        }
        var updatedItem = changes.applied();
        if (modification.hasNewKey()) {
            create(updatedItem);
            delete(restaurant);
        } else {
            log.debug("Updating {} with {}", restaurant, changes);
            repository.update(restaurant, changes.toAttributesCreator());
        }
        return Optional.of(updatedItem);
    }

    @Override
    public void delete(RestaurantItem restaurant) {
        log.debug("Deleting {}", restaurant);
        repository.delete(restaurant);
    }

    @Override
    public void setRating(RestaurantItem restaurant, int rating) {
        log.debug("Setting {} rating to {}", restaurant, rating);
        repository.update(restaurant, Map.of(
                RATING, asNumberUpdateAttribute(rating),
                TRIED_BEFORE, asUpdateAttribute(fromBool(true))
        ));
    }

    @Override
    public void setReview(RestaurantItem restaurant, String review) {
        log.debug("Setting {} review: {}", restaurant, review);
        repository.update(restaurant, Map.of(
                REVIEW, asUpdateAttribute(fromS(review)),
                TRIED_BEFORE, asUpdateAttribute(fromBool(true))
        ));
    }

    @Override
    public void setTriedBefore(RestaurantItem restaurant, boolean tried) {
        log.debug("Setting {} tried before flag to {}", restaurant, tried);
        Map<String, AttributeValueUpdate> updates = new HashMap<>();
        updates.put(TRIED_BEFORE, asUpdateAttribute(fromBool(tried)));
        if (!tried) {
            updates.put(RATING, asUpdateAttribute(EMPTY_RATING));
            updates.put(REVIEW, asUpdateAttribute(EMPTY_REVIEW));
        }
        repository.update(restaurant, updates);
    }

    @Override
    public void replaceCategories(RestaurantItem restaurant, Set<Category> categories) {
        log.debug("Replacing {} categories with {}", restaurant, categories);
        repository.update(restaurant, Map.of(
                CATEGORIES, asUpdateAttribute(setAttribute(categories, Category::getValue))
        ));
    }

    @Override
    public void replaceNotes(RestaurantItem restaurant, List<String> notes) {
        log.debug("Replacing {} notes with {}", restaurant, notes);
        repository.update(restaurant, Map.of(
                NOTES, asUpdateAttribute(listAttribute(notes))
        ));
    }

}
