package io.github.jonarzz.restaurant.knowledge.domain;

import static io.github.jonarzz.restaurant.knowledge.common.ModificationResult.*;
import static io.github.jonarzz.restaurant.knowledge.common.ModificationResultType.*;
import static io.github.jonarzz.restaurant.knowledge.domain.RestaurantItem.Attributes.*;
import static io.github.jonarzz.restaurant.knowledge.domain.RestaurantModification.*;
import static io.github.jonarzz.restaurant.knowledge.technical.dynamodb.AttributesCreator.*;
import static java.util.stream.Collectors.*;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.*;

import lombok.extern.slf4j.*;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;

import io.github.jonarzz.restaurant.knowledge.common.*;

@Slf4j
class RestaurantDynamoDbService implements RestaurantService {

    private RestaurantRepository repository;

    RestaurantDynamoDbService(RestaurantRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<RestaurantData> fetch(String restaurantName) {
        log.debug("Fetching restaurant by name: {}", restaurantName);
        return findRestaurant(restaurantName)
                .map(RestaurantItem::data);
    }

    @Override
    public List<RestaurantData> query(RestaurantQueryCriteria criteria) {
        log.debug("Querying restaurant by criteria: {}", criteria);
        return repository.query(new RestaurantDynamoDbCriteria(criteria))
                         .stream()
                         .map(RestaurantItem::data)
                         .collect(toList());
    }

    @Override
    public ModificationResultType create(RestaurantData restaurant) {
        if (findRestaurant(restaurant.name())
                .isPresent()) {
            return ALREADY_EXISTS;
        }
        if (Stream.of(restaurant.rating(), restaurant.review())
                  .anyMatch(Objects::nonNull)) {
            restaurant.triedBefore(true);
        }
        log.debug("Creating {}", restaurant);
        repository.create(RestaurantItem.from(restaurant));
        return SUCCESS;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ModificationResult<RestaurantData> update(String restaurantName, RestaurantData updateData) {
        return findRestaurant(restaurantName)
                .map(restaurant -> {
                    var modification = updateItem(restaurant).with(updateData);
                    var changes = modification.changes();
                    if (changes.empty()) {
                        log.debug("Skipping update of {} - no changes", restaurant);
                        return noChanges();
                    }
                    var newName = updateData.name();
                    if (newName != null && !restaurantName.equals(newName) && findRestaurant(newName).isPresent()) {
                        return alreadyExists();
                    }
                    var updatedItem = changes.applied();
                    if (modification.hasNewKey()) {
                        create(updatedItem);
                        delete(restaurantName);
                    } else {
                        log.debug("Updating {} with {}", restaurant, changes);
                        repository.update(restaurant, changes.toAttributesCreator());
                    }
                    return success(updatedItem);
                })
                .orElseGet(ModificationResult::notFound);
    }

    @Override
    public ModificationResultType delete(String restaurantName) {
        return actOnFound(restaurantName, restaurant -> {
            log.debug("Deleting {}", restaurantName);
            repository.delete(restaurant);
            return SUCCESS;
        });
    }

    @Override
    public ModificationResultType setRating(String restaurantName, int rating) {
        return actOnFound(restaurantName, restaurant -> {
            if (Objects.equals(rating, restaurant.rating())) {
                return NO_CHANGES;
            }
            log.debug("Setting {} rating to {}", restaurant, rating);
            repository.update(restaurant, Map.of(
                    RATING, asNumberUpdateAttribute(rating),
                    TRIED_BEFORE, asUpdateAttribute(fromBool(true))
            ));
            return SUCCESS;
        });
    }

    @Override
    public ModificationResultType setReview(String restaurantName, String review) {
        return actOnFound(restaurantName, restaurant -> {
            if (Objects.equals(review, restaurant.review())) {
                return NO_CHANGES;
            }
            log.debug("Setting {} review: {}", restaurant, review);
            repository.update(restaurant, Map.of(
                    REVIEW, asUpdateAttribute(fromS(review)),
                    TRIED_BEFORE, asUpdateAttribute(fromBool(true))
            ));
            return SUCCESS;
        });
    }

    @Override
    public ModificationResultType setTriedBefore(String restaurantName, boolean tried) {
        return actOnFound(restaurantName, restaurant -> {
            if (restaurant.triedBefore() == tried) {
                return NO_CHANGES;
            }
            log.debug("Setting {} tried before flag to {}", restaurantName, tried);
            Map<String, AttributeValueUpdate> updates = new HashMap<>();
            updates.put(TRIED_BEFORE, asUpdateAttribute(fromBool(tried)));
            if (!tried) {
                updates.put(RATING, asUpdateAttribute(EMPTY_RATING));
                updates.put(REVIEW, asUpdateAttribute(EMPTY_REVIEW));
            }
            repository.update(restaurant, updates);
            return SUCCESS;
        });
    }

    @Override
    public ModificationResultType replaceCategories(String restaurantName, Set<Category> categories) {
        return actOnFound(restaurantName, restaurant -> {
            if (Objects.equals(categories, restaurant.categories())) {
                return NO_CHANGES;
            }
            log.debug("Replacing {} categories with {}", restaurantName, categories);
            repository.update(restaurant, Map.of(
                    CATEGORIES, asUpdateAttribute(setAttribute(categories, Category::getValue))
            ));
            return SUCCESS;
        });
    }

    @Override
    public ModificationResultType replaceNotes(String restaurantName, List<String> notes) {
        return actOnFound(restaurantName, restaurant -> {
            if (Objects.equals(notes, restaurant.notes())) {
                return NO_CHANGES;
            }
            log.debug("Replacing {} notes with {}", restaurantName, notes);
            repository.update(restaurant, Map.of(
                    NOTES, asUpdateAttribute(listAttribute(notes))
            ));
            return SUCCESS;
        });
    }

    private Optional<RestaurantItem> findRestaurant(String restaurantName) {
        return repository.findByKey(new RestaurantKey(restaurantName));
    }

    private ModificationResultType actOnFound(String restaurantName,
                                              Function<RestaurantItem, ModificationResultType> action) {
        return findRestaurant(restaurantName)
                .map(action)
                .orElse(NOT_FOUND);
    }

}
