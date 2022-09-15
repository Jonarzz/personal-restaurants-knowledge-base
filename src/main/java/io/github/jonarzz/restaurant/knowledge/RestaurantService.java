package io.github.jonarzz.restaurant.knowledge;

import static java.util.function.Function.*;
import static java.util.stream.Collectors.*;
import static software.amazon.awssdk.services.dynamodb.model.AttributeAction.*;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.*;

import org.springframework.security.core.context.*;
import org.springframework.stereotype.*;
import software.amazon.awssdk.services.dynamodb.*;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;
import java.util.function.*;

import io.github.jonarzz.restaurant.knowledge.model.*;

@Repository
class RestaurantService {

    private static final String TABLE_NAME = "Restaurant";

    private static final AttributeValueUpdate DELETE_UPDATE = AttributeValueUpdate.builder()
                                                                                  .action(DELETE)
                                                                                  .build();

    private DynamoDbClient client;

    RestaurantService(DynamoDbClient client) {
        this.client = client;
    }

    FetchResult fetch(String restaurantName) {
        var userId = SecurityContextHolder.getContext()
                                          .getAuthentication()
                                          .getName();
        return findByUserIdAndRestaurantName(userId, restaurantName)
                .<FetchResult>map(FetchResult.Found::new)
                .orElseGet(FetchResult.NotFound::new);
    }

    void create(RestaurantRow restaurant) {
        client.putItem(PutItemRequest.builder()
                                     .tableName(TABLE_NAME)
                                     .item(toItem(restaurant))
                                     .build());
    }

    void rename(RestaurantRow restaurant, String newName) {
        create(restaurant.renamedTo(newName));
        delete(restaurant);
    }

    void delete(RestaurantRow restaurant) {
        var request = DeleteItemRequest.builder()
                                       .tableName(TABLE_NAME)
                                       .key(createKey(restaurant))
                                       .build();
        client.deleteItem(request);
    }

    void setRating(RestaurantRow restaurant, Integer rating) {
        var request = UpdateItemRequest.builder()
                                       .tableName(TABLE_NAME)
                                       .key(createKey(restaurant))
                                       .attributeUpdates(Map.of(
                                               "rating", AttributeValueUpdate.builder()
                                                                             .value(Optional.ofNullable(rating)
                                                                                            .map(String::valueOf)
                                                                                            .map(AttributeValue::fromN)
                                                                                            .orElse(null))
                                                                             .build(),
                                               "triedBefore", AttributeValueUpdate.builder()
                                                                                  .value(fromBool(true))
                                                                                  .build()
                                       ))
                                       .build();
        client.updateItem(request);
    }

    void setReview(RestaurantRow restaurant, String review) {
        var request = UpdateItemRequest.builder()
                                       .tableName(TABLE_NAME)
                                       .key(createKey(restaurant))
                                       .attributeUpdates(Map.of(
                                               "review", AttributeValueUpdate.builder()
                                                                             .value(fromS(review))
                                                                             .build(),
                                               "triedBefore", AttributeValueUpdate.builder()
                                                                                  .value(fromBool(true))
                                                                                  .build()
                                       ))
                                       .build();
        client.updateItem(request);
    }

    void setTriedBefore(RestaurantRow restaurant, boolean tried) {
        Map<String, AttributeValueUpdate> updates = new HashMap<>();
        updates.put("triedBefore", AttributeValueUpdate.builder()
                                                       .value(fromBool(tried))
                                                       .build());
        if (!tried) {
            updates.put("rating", DELETE_UPDATE);
            updates.put("review", DELETE_UPDATE);
        }
        var request = UpdateItemRequest.builder()
                                       .tableName(TABLE_NAME)
                                       .key(createKey(restaurant))
                                       .attributeUpdates(updates)
                                       .build();
        client.updateItem(request);
    }

    void replaceCategories(RestaurantRow restaurant, Set<Category> categories) {
        var attributes = new AttributesCreator()
                .putIfNotEmpty("category", categories, Category::getValue);
        var request = UpdateItemRequest.builder()
                                       .tableName(TABLE_NAME)
                                       .key(createKey(restaurant))
                                       .attributeUpdates(Map.of(
                                               "categories",
                                               // TODO single attribute building -> use in AttributeCreator
                                               AttributeValueUpdate.builder()
                                                                   .value(attributes.attributes.get("category"))
                                                                   .build()
                                       ))
                                       .build();
        client.updateItem(request);
    }

    void replaceNotes(RestaurantRow restaurant, List<String> notes) {
        var attributes = new AttributesCreator()
                .putIfNotEmpty("notes", notes);
        var request = UpdateItemRequest.builder()
                                       .tableName(TABLE_NAME)
                                       .key(createKey(restaurant))
                                       .attributeUpdates(Map.of(
                                               "notes",
                                               // TODO single attribute building -> use in AttributeCreator
                                               AttributeValueUpdate.builder()
                                                                   .value(attributes.attributes.get("notes"))
                                                                   .build()
                                       ))
                                       .build();
        client.updateItem(request);
    }

    private Optional<RestaurantRow> findByUserIdAndRestaurantName(String userId, String restaurantName) {
        var request = GetItemRequest.builder()
                                    .tableName(TABLE_NAME)
                                    .key(createKey(userId, restaurantName))
                                    .build();
        var response = client.getItem(request);
        if (!response.hasItem()) {
            return Optional.empty();
        }
        var extractor = new ItemExtractor(response.item());
        return Optional.of(RestaurantRow.builder()
                                        .userId(extractor.string("userId"))
                                        .restaurantName(extractor.string("restaurantName"))
                                        .categories(extractor.set("categories", Category::valueOf))
                                        .triedBefore(extractor.bool("triedBefore"))
                                        .rating(extractor.integer("rating"))
                                        .review(extractor.string("review"))
                                        .notes(extractor.list("notes"))
                                        .build());
    }

    // TODO vvv extract (DAO layer?) vvv

    private static Map<String, AttributeValue> createKey(RestaurantRow restaurant) {
        return createKey(restaurant.userId(), restaurant.restaurantName());
    }

    private static Map<String, AttributeValue> createKey(String userId, String restaurantName) {
        return Map.of(
                "userId", AttributeValue.builder()
                                        .s(userId)
                                        .build(),
                "restaurantName", AttributeValue.builder()
                                                .s(restaurantName)
                                                .build()
        );
    }

    private static Map<String, AttributeValue> toItem(RestaurantRow restaurant) {
        return new AttributesCreator()
                .putIfPresent("userId", restaurant.userId(), AttributeValue::fromS)
                .putIfPresent("restaurantName", restaurant.restaurantName(), AttributeValue::fromS)
                .putIfPresent("triedBefore", restaurant.triedBefore(), AttributeValue::fromBool)
                .putIfPresent("review", restaurant.review(), AttributeValue::fromS)
                .putIfPresent("rating", restaurant.ratingString(), AttributeValue::fromN)
                .putIfNotEmpty("notes", restaurant.notes())
                .putIfNotEmpty("categories", restaurant.categories(), Category::getValue)
                .create();
    }

    static class AttributesCreator {

        private Map<String, AttributeValue> attributes = new HashMap<>();

        <T> AttributesCreator putIfPresent(String attributeName, T nullable,
                                           Function<T, AttributeValue> attributeCreator) {
            if (nullable == null) {
                return this;
            }
            attributes.put(attributeName, attributeCreator.apply(nullable));
            return this;
        }

        AttributesCreator putIfNotEmpty(String attributeName, List<String> values) {
            if (values.isEmpty()) {
                return this;
            }
            attributes.put(attributeName, fromL(values.stream()
                                                      .map(AttributeValue::fromS)
                                                      .toList()));
            return this;
        }

        <S> AttributesCreator putIfNotEmpty(String attributeName, Set<S> values, Function<S, String> mapper) {
            if (values.isEmpty()) {
                return this;
            }
            var mapped = values.stream()
                               .map(mapper)
                               .collect(toSet());
            attributes.put(attributeName, AttributeValue.builder()
                                                        .ss(mapped)
                                                        .build());
            return this;
        }

        Map<String, AttributeValue> create() {
            return attributes;
        }
    }

    static class ItemExtractor {

        private final Map<String, AttributeValue> item;

        ItemExtractor(Map<String, AttributeValue> item) {
            this.item = item;
        }

        String string(String attributeName) {
            return get(attributeName, AttributeValue::s);
        }

        Integer integer(String attributeName) {
            return get(attributeName, AttributeValue::n, Integer::valueOf);
        }

        boolean bool(String attributeName) {
            return getOrDefault(attributeName, AttributeValue::bool, false);
        }

        <M> Set<M> set(String attributeName, Function<String, M> mapper) {
            return Optional.ofNullable(item.get(attributeName))
                           .map(AttributeValue::ss)
                           .stream()
                           .flatMap(Collection::stream)
                           .map(mapper)
                           .collect(toSet());
        }

        List<String> list(String attributeName) {
            return Optional.ofNullable(item.get(attributeName))
                           .map(AttributeValue::l)
                           .stream()
                           .flatMap(Collection::stream)
                           .map(AttributeValue::s)
                           .toList();
        }

        private <A> A get(String attributeName, Function<AttributeValue, A> extractor) {
            return getOrDefault(attributeName, extractor, (A) null);
        }

        private <A, M> M get(String attributeName, Function<AttributeValue, A> extractor, Function<A, M> mapper) {
            return getOrDefault(attributeName, extractor, mapper, null);
        }

        private <A> A getOrDefault(String attributeName, Function<AttributeValue, A> extractor, A defaultValue) {
            return getOrDefault(attributeName, extractor, identity(), defaultValue);
        }

        private <A, M> M getOrDefault(String attributeName, Function<AttributeValue, A> extractor,
                                      Function<A, M> mapper, M defaultValue) {
            return Optional.ofNullable(item.get(attributeName))
                           .map(extractor)
                           .map(mapper)
                           .orElse(defaultValue);
        }
    }

}
