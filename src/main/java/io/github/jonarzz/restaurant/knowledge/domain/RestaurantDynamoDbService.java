package io.github.jonarzz.restaurant.knowledge.domain;

import static io.github.jonarzz.restaurant.knowledge.domain.RestaurantItem.Fields.*;
import static io.github.jonarzz.restaurant.knowledge.technical.dynamodb.AttributesCreator.*;
import static software.amazon.awssdk.services.dynamodb.model.AttributeAction.*;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.*;

import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;
import java.util.stream.Stream;

import io.github.jonarzz.restaurant.knowledge.model.*;
import io.github.jonarzz.restaurant.knowledge.technical.dynamodb.*;

class RestaurantDynamoDbService implements RestaurantService {

    // TODO cache

    private static final AttributeValueUpdate DELETE_UPDATE = AttributeValueUpdate.builder()
                                                                                  .action(DELETE)
                                                                                  .build();

    private DynamoDbRepository<RestaurantItem, RestaurantKey> repository;

    RestaurantDynamoDbService(DynamoDbRepository<RestaurantItem, RestaurantKey> repository) {
        this.repository = repository;
    }

    @Override
    public FetchResult fetch(String restaurantName) {
        // TODO replace with Optional
        return repository.findByKey(new RestaurantKey(restaurantName))
                         .<FetchResult>map(FetchResult.Found::new)
                         .orElseGet(FetchResult.NotFound::new);
    }

    @Override
    public List<RestaurantItem> query(RestaurantQueryCriteria criteria) {
        // TODO case insensitive name query
        //      (save restaurant name lowercase as sort key + actual value as attribute)
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
    public void delete(RestaurantItem restaurantItem) {
        repository.delete(restaurantItem);
    }

    @Override
    public void rename(RestaurantItem restaurant, String newName) {
        create(restaurant.renamedTo(newName));
        delete(restaurant);
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
