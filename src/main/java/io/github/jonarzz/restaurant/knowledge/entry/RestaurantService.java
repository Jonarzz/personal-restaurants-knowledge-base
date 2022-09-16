package io.github.jonarzz.restaurant.knowledge.entry;

import static io.github.jonarzz.restaurant.knowledge.dynamodb.AttributesCreator.*;
import static software.amazon.awssdk.services.dynamodb.model.AttributeAction.*;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.*;

import org.springframework.security.core.context.*;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

import io.github.jonarzz.restaurant.knowledge.dynamodb.*;
import io.github.jonarzz.restaurant.knowledge.model.*;

class RestaurantService {

    private static final AttributeValueUpdate DELETE_UPDATE = AttributeValueUpdate.builder()
                                                                                  .action(DELETE)
                                                                                  .build();

    private DynamoDbRepository<RestaurantItem, RestaurantKey> repository;

    RestaurantService(DynamoDbRepository<RestaurantItem, RestaurantKey> repository) {
        this.repository = repository;
    }

    FetchResult fetch(String restaurantName) {
        var userId = SecurityContextHolder.getContext()
                                          .getAuthentication()
                                          .getName();
        return repository.findByKey(new RestaurantKey(userId, restaurantName))
                         .<FetchResult>map(FetchResult.Found::new)
                         .orElseGet(FetchResult.NotFound::new);
    }

    void create(RestaurantItem item) {
        repository.create(item);
    }

    void delete(RestaurantItem restaurantItem) {
        repository.delete(restaurantItem);
    }

    void rename(RestaurantItem restaurant, String newName) {
        create(restaurant.renamedTo(newName));
        delete(restaurant);
    }

    void setRating(RestaurantItem restaurant, Integer rating) {
        repository.update(restaurant, Map.of(
                "rating", asNumberUpdateAttribute(rating),
                "triedBefore", asUpdateAttribute(fromBool(true))
        ));
    }

    void setReview(RestaurantItem restaurant, String review) {
        repository.update(restaurant, Map.of(
                "review", asUpdateAttribute(fromS(review)),
                "triedBefore", asUpdateAttribute(fromBool(true))
        ));
    }

    void setTriedBefore(RestaurantItem restaurant, boolean tried) {
        Map<String, AttributeValueUpdate> updates = new HashMap<>();
        updates.put("triedBefore", asUpdateAttribute(fromBool(tried)));
        if (!tried) {
            updates.put("rating", DELETE_UPDATE);
            updates.put("review", DELETE_UPDATE);
        }
        repository.update(restaurant, updates);
    }

    void replaceCategories(RestaurantItem restaurant, Set<Category> categories) {
        repository.update(restaurant, Map.of(
                "categories", asUpdateAttribute(setAttribute(categories, Category::getValue))
        ));
    }

    void replaceNotes(RestaurantItem restaurant, List<String> notes) {
        repository.update(restaurant, Map.of(
                "notes", asUpdateAttribute(listAttribute(notes))
        ));
    }
}
