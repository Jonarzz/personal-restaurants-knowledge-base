package io.github.jonarzz.restaurant.knowledge;

import static java.util.function.Function.*;
import static java.util.stream.Collectors.*;

import org.springframework.security.core.context.*;
import org.springframework.stereotype.*;
import software.amazon.awssdk.services.dynamodb.*;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;
import java.util.function.*;

import io.github.jonarzz.restaurant.knowledge.model.*;

@Repository
class RestaurantService {

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

    void save(RestaurantRow restaurant) {
        client.putItem(PutItemRequest.builder()
                                     .tableName("Restaurant")
                                     .item(toItem(restaurant))
                                     .build());
    }

    Optional<RestaurantRow> findByUserIdAndRestaurantName(String userId, String restaurantName) {
        var request = GetItemRequest.builder()
                                    .tableName("Restaurant")
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

    void rename(RestaurantRow restaurant, String newName) {
        // TODO add copy with new name + delete old
    }

    void delete(RestaurantRow restaurant) {
    }

    void setRating(RestaurantRow restaurant, Integer rating) {
        // TODO
        if (rating == null) {

        } else {

        }
    }

    void setReview(RestaurantRow restaurant, String review) {
        // TODO
        if (review == null) {

        } else {

        }
    }

    void setVisited(RestaurantRow restaurant, boolean visited) {
        // TODO (remove rating, review and notes when not visited)
        if (visited) {

        } else {

        }
    }

    void replaceCategories(RestaurantRow restaurant, Set<Category> categories) {
        var attributes = new AttributesCreator()
                .putIfNotEmpty("category", categories, Category::getValue);
        var request = UpdateItemRequest.builder()
                                       .tableName("Restaurant")
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
                .putIfPresent("userId",         restaurant.userId(),         AttributeValue.Builder::s)
                .putIfPresent("restaurantName", restaurant.restaurantName(), AttributeValue.Builder::s)
                .putIfPresent("triedBefore",    restaurant.triedBefore(),    AttributeValue.Builder::bool)
                .putIfPresent("review",         restaurant.review(),         AttributeValue.Builder::s)
                .putIfPresent("rating",         restaurant.ratingString(),   AttributeValue.Builder::n)
                .putIfNotEmpty("notes",      restaurant.notes())
                .putIfNotEmpty("categories", restaurant.categories(), Category::getValue)
                .create();
    }

    static class AttributesCreator {

        private Map<String, AttributeValue> attributes = new HashMap<>();

        <T> AttributesCreator putIfPresent(String attributeName, T nullable,
                                           BiConsumer<AttributeValue.Builder, T> builderMethod) {
            if (nullable == null) {
                return this;
            }
            var builder = AttributeValue.builder();
            builderMethod.accept(builder, nullable);
            attributes.put(attributeName, builder.build());
            return this;
        }

        AttributesCreator putIfNotEmpty(String attributeName, List<String> values) {
            if (values.isEmpty()) {
                return this;
            }
            attributes.put(attributeName, AttributeValue.builder()
                                                        .l(values.stream()
                                                                 .map(value -> AttributeValue.builder()
                                                                                             .s(value)
                                                                                             .build())
                                                                 .toList())
                                                        .build());
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
