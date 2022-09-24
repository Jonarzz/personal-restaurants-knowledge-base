package io.github.jonarzz.restaurant.knowledge.domain;

import java.util.*;

import io.github.jonarzz.restaurant.knowledge.model.*;

interface RestaurantService {

    Optional<RestaurantItem> fetch(String restaurantName);

    List<RestaurantItem> query(RestaurantQueryCriteria criteria);

    void create(RestaurantItem item);

    void delete(RestaurantItem restaurantItem);

    void rename(RestaurantItem restaurant, String newName);

    void setRating(RestaurantItem restaurant, int rating);

    void setReview(RestaurantItem restaurant, String review);

    void setTriedBefore(RestaurantItem restaurant, boolean tried);

    void replaceCategories(RestaurantItem restaurant, Set<Category> categories);

    void replaceNotes(RestaurantItem restaurant, List<String> notes);
}
