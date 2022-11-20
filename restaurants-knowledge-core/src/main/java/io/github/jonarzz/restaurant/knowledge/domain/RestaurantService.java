package io.github.jonarzz.restaurant.knowledge.domain;

import java.util.*;

import io.github.jonarzz.restaurant.knowledge.common.*;

public interface RestaurantService {

    Optional<RestaurantData> fetch(String restaurantName);

    List<RestaurantData> query(RestaurantQueryCriteria criteria);

    ModificationResultType create(RestaurantData restaurantData);

    ModificationResult<RestaurantData> update(String restaurantName, RestaurantData updateData);

    ModificationResultType delete(String restaurantName);

    ModificationResultType setRating(String restaurantName, int rating);

    ModificationResultType setReview(String restaurantName, String review);

    ModificationResultType setTriedBefore(String restaurantName, boolean tried);

    ModificationResultType replaceCategories(String restaurantName, Set<Category> categories);

    ModificationResultType replaceNotes(String restaurantName, List<String> notes);
}
