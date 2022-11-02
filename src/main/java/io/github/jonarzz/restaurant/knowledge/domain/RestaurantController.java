package io.github.jonarzz.restaurant.knowledge.domain;

import static java.nio.charset.StandardCharsets.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.ResponseEntity.*;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.net.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import io.github.jonarzz.restaurant.knowledge.api.*;
import io.github.jonarzz.restaurant.knowledge.model.*;

@RestController
        // @PreAuthorize("isAuthenticated()")
class RestaurantController implements RestaurantsApi {

    private static final String PATH = "/restaurants";

    private RestaurantService restaurantService;

    RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @Override
    public ResponseEntity<RestaurantData> getRestaurantDetails(String restaurantName) {
        return actOnFound(restaurantName, restaurant -> ok(restaurant.data()));
    }

    @Override
    public ResponseEntity<List<RestaurantData>> queryRestaurantsByCriteria(String nameBeginsWith,
                                                                           Category category,
                                                                           Boolean triedBefore,
                                                                           Integer ratingAtLeast) {
        var criteria = RestaurantQueryCriteria.builder()
                                              .nameBeginsWith(nameBeginsWith)
                                              .category(category)
                                              .triedBefore(triedBefore)
                                              .ratingAtLeast(ratingAtLeast)
                                              .build();
        if (criteria.isEmpty()) {
            return badRequest()
                    .build();
        }
        return ok(restaurantService.query(criteria)
                                   .stream()
                                   .map(RestaurantItem::data)
                                   .toList());
    }

    @Override
    public ResponseEntity<Void> createRestaurant(RestaurantData restaurant) {
        var restaurantName = restaurant.getName();
        return restaurantService.fetch(restaurantName)
                                .map(ignored -> status(CONFLICT).<Void>build())
                                .orElseGet(() -> {
                                    restaurantService.create(RestaurantItem.from(restaurant));
                                    return created(URI.create(
                                            PATH + "/" + URLEncoder.encode(restaurantName, UTF_8)
                                    )).build();
                                });
    }

    @Override
    public ResponseEntity<RestaurantData> updateRestaurant(String restaurantName,
                                                           RestaurantData newData) {
        return actOnFound(restaurantName, restaurant -> {
            var newName = newData.getName();
            if (!restaurantName.equals(newName) && restaurantService.fetch(newName)
                                                                    .isPresent()) {
                return status(CONFLICT).build();
            }
            return restaurantService.update(restaurant, newData)
                                    .map(RestaurantItem::data)
                                    .map(ResponseEntity::ok)
                                    .orElseGet(() -> status(NO_CONTENT).build());
        });
    }

    @Override
    public ResponseEntity<Void> deleteRestaurant(String restaurantName) {
        return actOnFound(restaurantName, restaurant -> {
            restaurantService.delete(restaurant);
            return noContent().build();
        });
    }

    @Override
    public ResponseEntity<Void> updateRestaurantRating(String restaurantName,
                                                       UpdateRestaurantRatingRequest ratingRequest) {
        return actOnFound(restaurantName, restaurant -> {
            restaurantService.setRating(restaurant, ratingRequest.getRating());
            return noContent().build();
        });
    }

    @Override
    public ResponseEntity<Void> updateRestaurantReview(String restaurantName,
                                                       UpdateRestaurantReviewRequest reviewRequest) {
        return actOnFound(restaurantName, restaurant -> {
            restaurantService.setReview(restaurant, reviewRequest.getReview());
            return noContent().build();
        });
    }

    @Override
    public ResponseEntity<Void> deleteRestaurantReview(String restaurantName) {
        return actOnFound(restaurantName, restaurant -> {
            restaurantService.setReview(restaurant, null);
            return noContent().build();
        });
    }

    @Override
    public ResponseEntity<Void> markRestaurantAsTried(String restaurantName) {
        return changeTriedFlag(restaurantName, true);
    }

    @Override
    public ResponseEntity<Void> markRestaurantAsNotTried(String restaurantName) {
        return changeTriedFlag(restaurantName, false);
    }

    @Override
    public ResponseEntity<Set<Category>> addRestaurantCategory(String restaurantName,
                                                               AddRestaurantCategoryRequest categoryRequest) {
        return actOnFound(restaurantName, restaurant -> {
            var categoryToAdd = categoryRequest.getCategory();
            var categories = new HashSet<>(restaurant.categories());
            if (!categories.contains(categoryToAdd)) {
                categories.add(categoryToAdd);
                restaurantService.replaceCategories(restaurant, categories);
            }
            return ok(categories);
        });
    }

    @Override
    public ResponseEntity<Void> replaceRestaurantCategories(String restaurantName, Set<Category> categories) {
        return actOnFound(restaurantName, restaurant -> {
            restaurantService.replaceCategories(restaurant, categories);
            return noContent().build();
        });
    }

    @Override
    public ResponseEntity<Void> removeRestaurantCategory(String restaurantName, Category category) {
        return actOnFound(restaurantName, restaurant -> {
            if (restaurant.categories().contains(category)) {
                var categories = new HashSet<>(restaurant.categories());
                categories.remove(category);
                restaurantService.replaceCategories(restaurant, categories);
            }
            return noContent().build();
        });
    }

    @Override
    public ResponseEntity<List<String>> addRestaurantNote(String restaurantName,
                                                          AddRestaurantNoteRequest noteRequest) {
        return actOnFound(restaurantName, restaurant -> {
            var notes = new ArrayList<>(restaurant.notes());
            notes.add(noteRequest.getNote());
            restaurantService.replaceNotes(restaurant, notes);
            return ok(notes);
        });
    }

    @Override
    public ResponseEntity<List<String>> replaceRestaurantNote(String restaurantName, Integer noteIndex,
                                                              AddRestaurantNoteRequest addNoteRequest) {
        if (isInvalidIndex(noteIndex)) {
            return badRequest().build();
        }
        return actOnFound(restaurantName, restaurant -> {
            var currentNotes = restaurant.notes();
            var notesCount = currentNotes.size();
            if (notesCount <= noteIndex) {
                return badRequest().build();
            }
            var notes = splice(currentNotes, noteIndex, addNoteRequest.getNote());
            restaurantService.replaceNotes(restaurant, notes);
            return ok(notes);
        });
    }

    @Override
    public ResponseEntity<Void> removeRestaurantNote(String restaurantName, Integer noteIndex) {
        if (isInvalidIndex(noteIndex)) {
            return badRequest().build();
        }
        return actOnFound(restaurantName, restaurant -> {
            var currentNotes = restaurant.notes();
            var notesCount = currentNotes.size();
            if (notesCount > noteIndex) {
                var notes = splice(currentNotes, noteIndex);
                restaurantService.replaceNotes(restaurant, notes);
            }
            return noContent().build();
        });
    }

    private ResponseEntity<Void> changeTriedFlag(String restaurantName, boolean tried) {
        return actOnFound(restaurantName, restaurant -> {
            restaurantService.setTriedBefore(restaurant, tried);
            return noContent().build();
        });
    }

    private <T> ResponseEntity<T> actOnFound(String restaurantName,
                                             Function<RestaurantItem, ResponseEntity<T>> action) {
        return restaurantService.fetch(restaurantName)
                                .map(action)
                                .orElseGet(() -> notFound().build());
    }

    @SafeVarargs
    private static <T> List<T> splice(List<T> values, int index, T... toInsert) {
        return Stream.of(values.subList(0, index),
                         List.of(toInsert),
                         values.subList(index + 1, values.size()))
                     .flatMap(Collection::stream)
                     .toList();
    }

    private static boolean isInvalidIndex(Integer noteIndex) {
        return noteIndex == null || noteIndex < 0;
    }

}
