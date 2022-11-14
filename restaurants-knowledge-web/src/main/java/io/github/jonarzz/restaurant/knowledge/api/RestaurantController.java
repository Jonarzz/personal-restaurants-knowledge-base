package io.github.jonarzz.restaurant.knowledge.api;

import static io.github.jonarzz.restaurant.knowledge.common.ModificationResultType.NOT_FOUND;
import static io.github.jonarzz.restaurant.knowledge.common.ModificationResultType.*;
import static java.nio.charset.StandardCharsets.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.ResponseEntity.*;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.net.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import io.github.jonarzz.restaurant.knowledge.domain.Category;
import io.github.jonarzz.restaurant.knowledge.domain.RestaurantData;
import io.github.jonarzz.restaurant.knowledge.domain.*;
import io.github.jonarzz.restaurant.knowledge.model.*;

@RestController
// @PreAuthorize("isAuthenticated()")
public class RestaurantController implements RestaurantsApi {

    private static final String PATH = "/restaurants";

    private RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @Override
    public ResponseEntity<RestaurantData> getRestaurantDetails(String restaurantName) {
        return actOnFound(restaurantName, ResponseEntity::ok);
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
        return ok(restaurantService.query(criteria));
    }

    @Override
    public ResponseEntity<Void> createRestaurant(RestaurantData restaurant) {
        var result = restaurantService.create(restaurant);
        if (ALREADY_EXISTS == result) {
            return status(CONFLICT).build();
        }
        var targetUrl = PATH + "/" + URLEncoder.encode(restaurant.name(), UTF_8);
        return created(URI.create(targetUrl))
                .build();
    }

    @Override
    public ResponseEntity<RestaurantData> updateRestaurant(String restaurantName,
                                                           RestaurantData newData) {
        var result = restaurantService.update(restaurantName, newData);
        return switch (result.resultType()) {
            case SUCCESS -> ok(result.content());
            case NOT_FOUND -> notFound().build();
            case NO_CHANGES -> status(NO_CONTENT).build();
            case ALREADY_EXISTS -> status(CONFLICT).build();
        };
    }

    @Override
    public ResponseEntity<Void> deleteRestaurant(String restaurantName) {
        restaurantService.delete(restaurantName);
        return noContent().build();
    }

    @Override
    public ResponseEntity<Void> updateRestaurantRating(String restaurantName,
                                                       UpdateRestaurantRatingRequest ratingRequest) {
        var result = restaurantService.setRating(restaurantName, ratingRequest.getRating());
        if (NOT_FOUND == result) {
            return notFound().build();
        }
        return noContent().build();
    }

    @Override
    public ResponseEntity<Void> updateRestaurantReview(String restaurantName,
                                                       UpdateRestaurantReviewRequest reviewRequest) {
        var result = restaurantService.setReview(restaurantName, reviewRequest.getReview());
        if (NOT_FOUND == result) {
            return notFound().build();
        }
        return noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteRestaurantReview(String restaurantName) {
        var result = restaurantService.setReview(restaurantName, null);
        if (NOT_FOUND == result) {
            return notFound().build();
        }
        return noContent().build();
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
                restaurantService.replaceCategories(restaurantName, categories);
            }
            return ok(categories);
        });
    }

    @Override
    public ResponseEntity<Void> replaceRestaurantCategories(String restaurantName, Set<Category> categories) {
        var result = restaurantService.replaceCategories(restaurantName, categories);
        if (NOT_FOUND == result) {
            return notFound().build();
        }
        return noContent().build();
    }

    @Override
    public ResponseEntity<Void> removeRestaurantCategory(String restaurantName, Category category) {
        return actOnFound(restaurantName, restaurant -> {
            if (restaurant.categories().contains(category)) {
                var categories = new HashSet<>(restaurant.categories());
                categories.remove(category);
                restaurantService.replaceCategories(restaurantName, categories);
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
            restaurantService.replaceNotes(restaurantName, notes);
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
            restaurantService.replaceNotes(restaurantName, notes);
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
                restaurantService.replaceNotes(restaurantName, notes);
            }
            return noContent().build();
        });
    }

    private ResponseEntity<Void> changeTriedFlag(String restaurantName, boolean tried) {
        var result = restaurantService.setTriedBefore(restaurantName, tried);
        if (NOT_FOUND == result) {
            return notFound().build();
        }
        return noContent().build();
    }

    private <T> ResponseEntity<T> actOnFound(String restaurantName,
                                             Function<RestaurantData, ResponseEntity<T>> action) {
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
