package io.github.jonarzz.restaurant.knowledge.domain;

import java.util.*;

public interface RestaurantService {

    String CACHE_NAME = "RestaurantCache";

    Optional<RestaurantItem> fetch(String restaurantName);

    List<RestaurantItem> query(RestaurantQueryCriteria criteria);

    void create(RestaurantItem item);

    Optional<RestaurantItem> update(RestaurantItem restaurant, RestaurantData updateData);

    void delete(RestaurantItem restaurantItem);

    void setRating(RestaurantItem restaurant, int rating);

    void setReview(RestaurantItem restaurant, String review);

    void setTriedBefore(RestaurantItem restaurant, boolean tried);

    void replaceCategories(RestaurantItem restaurant, Set<Category> categories);

    void replaceNotes(RestaurantItem restaurant, List<String> notes);
}
