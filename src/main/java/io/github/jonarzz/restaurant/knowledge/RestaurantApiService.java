package io.github.jonarzz.restaurant.knowledge;

import org.springframework.http.*;
import org.springframework.security.access.prepost.*;
import org.springframework.security.core.context.*;
import org.springframework.stereotype.*;

import java.util.*;

import io.github.jonarzz.restaurant.knowledge.api.*;
import io.github.jonarzz.restaurant.knowledge.model.*;

@Service
@PreAuthorize("isAuthenticated()")
class RestaurantApiService implements RestaurantsApi {

    private RestaurantTableDao restaurantTableDao;

    RestaurantApiService(RestaurantTableDao restaurantTableDao) {
        this.restaurantTableDao = restaurantTableDao;
    }

    @Override
    public ResponseEntity<RestaurantData> getRestaurantDetails(String restaurantName) {
        return restaurantTableDao.findByUserIdAndRestaurantName(currentUserId(), restaurantName)
                                 .map(todo -> new RestaurantData()) // TODO
                                 .map(ResponseEntity::ok)
                                 .orElseGet(() -> ResponseEntity.notFound()
                                                                  .build());
    }

    @Override
    public ResponseEntity<List<RestaurantData>> queryRestaurantsByCriteria(String nameContaining,
                                                                           Set<Category> categories,
                                                                           Boolean triedBefore,
                                                                           Integer ratingAtLeast) {
        return null;
    }

    @Override
    public ResponseEntity<RestaurantData> createRestaurant(RestaurantData restaurant) {
        return null;
    }

    @Override
    public ResponseEntity<Void> renameRestaurant(String restaurantName,
                                                 RenameRestaurantRequest renameRestaurantRequest) {
        return null;
    }

    @Override
    public ResponseEntity<Void> deleteRestaurant(String restaurantName) {
        return null;
    }

    @Override
    public ResponseEntity<Set<Category>> addRestaurantCategory(String restaurantName,
                                                               AddRestaurantCategoryRequest addRestaurantCategoryRequest) {
        return null;
    }

    @Override
    public ResponseEntity<Void> replaceRestaurantCategories(String restaurantName, Set<Category> category) {
        return null;
    }

    @Override
    public ResponseEntity<Void> removeRestaurantCategory(String restaurantName, Category categoryName) {
        return null;
    }

    @Override
    public ResponseEntity<Set<String>> addRestaurantNote(String restaurantName,
                                                         AddRestaurantNoteRequest addRestaurantNoteRequest) {
        return null;
    }

    @Override
    public ResponseEntity<Void> replaceRestaurantNote(String restaurantName, Integer noteIndex,
                                                      AddRestaurantNoteRequest addRestaurantNoteRequest) {
        return null;
    }

    @Override
    public ResponseEntity<Void> removeRestaurantNote(String restaurantName, Integer noteIndex) {
        return null;
    }

    @Override
    public ResponseEntity<Void> updateRestaurantRating(String restaurantName, UpdateRestaurantRatingRequest updateRestaurantRatingRequest) {
        return null;
    }

    @Override
    public ResponseEntity<Void> markRestaurantAsVisited(String restaurantName) {
        return null;
    }

    @Override
    public ResponseEntity<Void> markRestaurantAsNotVisited(String restaurantName) {
        return null;
    }

    private static String currentUserId() {
        return SecurityContextHolder.getContext()
                                    .getAuthentication()
                                    .getName();
    }
}
