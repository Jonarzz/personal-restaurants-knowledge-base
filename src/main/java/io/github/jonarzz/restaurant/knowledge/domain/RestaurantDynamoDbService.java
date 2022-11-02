package io.github.jonarzz.restaurant.knowledge.domain;

import static io.github.jonarzz.restaurant.knowledge.domain.RestaurantItem.Attributes.*;
import static io.github.jonarzz.restaurant.knowledge.domain.RestaurantModification.*;
import static io.github.jonarzz.restaurant.knowledge.technical.dynamodb.AttributesCreator.*;
import static software.amazon.awssdk.services.dynamodb.model.AttributeAction.*;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.*;

import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;
import java.util.stream.Stream;

import io.github.jonarzz.restaurant.knowledge.model.*;
import io.github.jonarzz.restaurant.knowledge.technical.dynamodb.*;

class RestaurantDynamoDbService implements RestaurantService {

    private static final AttributeValueUpdate DELETE_UPDATE = AttributeValueUpdate.builder()
                                                                                  .action(DELETE)
                                                                                  .build();

    private DynamoDbRepository<RestaurantItem, RestaurantKey> repository;

    RestaurantDynamoDbService(DynamoDbRepository<RestaurantItem, RestaurantKey> repository) {
        this.repository = repository;
    }

    @Override
    public Optional<RestaurantItem> fetch(String restaurantName) {
        return repository.findByKey(new RestaurantKey(restaurantName));
    }

    @Override
    public List<RestaurantItem> query(RestaurantQueryCriteria criteria) {
        return repository.query(new RestaurantDynamoDbCriteria(criteria));
    }

    @Override
    public void create(RestaurantItem item) {
        if (Stream.of(item.rating(), item.review())
                  .anyMatch(Objects::nonNull)) {
            item = item.markedAsTriedBefore();
        }
        repository.create(item);
    }

    @Override
    public Optional<RestaurantItem> update(RestaurantItem restaurant, RestaurantData updateData) {
        var modification = updateItem(restaurant).with(updateData);
        var changes = modification.changes();
        if (changes.empty()) {
            return Optional.empty();
        }
        var updatedItem = changes.applied();
        if (modification.hasNewKey()) {
            create(updatedItem);
            delete(restaurant);
        } else {
            repository.update(restaurant, changes.toAttributesCreator());
        }
        return Optional.of(updatedItem);
    }

    @Override
    public void delete(RestaurantItem restaurantItem) {
        repository.delete(restaurantItem);
    }

    @Override
    public void setRating(RestaurantItem restaurant, int rating) {
        repository.update(restaurant, Map.of(
                RATING, asNumberUpdateAttribute(rating),
                TRIED_BEFORE, asUpdateAttribute(fromBool(true))
        ));
    }

    @Override
    public void setReview(RestaurantItem restaurant, String review) {
        repository.update(restaurant, Map.of(
                REVIEW, asUpdateAttribute(fromS(review)),
                TRIED_BEFORE, asUpdateAttribute(fromBool(true))
        ));
    }

    @Override
    public void setTriedBefore(RestaurantItem restaurant, boolean tried) {
        Map<String, AttributeValueUpdate> updates = new HashMap<>();
        updates.put(TRIED_BEFORE, asUpdateAttribute(fromBool(tried)));
        if (!tried) {
            updates.put(RATING, DELETE_UPDATE);
            updates.put(REVIEW, DELETE_UPDATE);
        }
        repository.update(restaurant, updates);
    }

    @Override
    public void replaceCategories(RestaurantItem restaurant, Set<Category> categories) {
        repository.update(restaurant, Map.of(
                CATEGORIES, asUpdateAttribute(setAttribute(categories, Category::getValue))
        ));
    }

    @Override
    public void replaceNotes(RestaurantItem restaurant, List<String> notes) {
        repository.update(restaurant, Map.of(
                NOTES, asUpdateAttribute(listAttribute(notes))
        ));
    }

}
