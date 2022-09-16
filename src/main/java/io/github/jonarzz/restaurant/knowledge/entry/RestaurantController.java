package io.github.jonarzz.restaurant.knowledge.entry;

import static io.github.jonarzz.restaurant.knowledge.entry.FetchResult.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.ResponseEntity.*;

import org.springframework.http.*;
import org.springframework.security.access.prepost.*;
import org.springframework.web.bind.annotation.*;

import java.net.*;
import java.util.*;
import java.util.stream.*;

import io.github.jonarzz.restaurant.knowledge.api.*;
import io.github.jonarzz.restaurant.knowledge.model.*;

@RestController
@RequestMapping(RestaurantController.PATH)
@PreAuthorize("isAuthenticated()")
class RestaurantController implements RestaurantsApi {

    static final String PATH = "/restaurants";

    private RestaurantService restaurantService;

    RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @Override
    public ResponseEntity<RestaurantData> getRestaurantDetails(String restaurantName) {
        return switch (restaurantService.fetch(restaurantName)) {
            case Found found -> found.then(RestaurantItem::data);
            case NotFound notFound -> notFound.response();
        };
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
    public ResponseEntity<Object> createRestaurant(RestaurantData restaurant) {
        var restaurantName = restaurant.getName();
        return switch (restaurantService.fetch(restaurantName)) {
            case Found found -> found.thenReturn(ignored -> status(CONFLICT).build());
            case NotFound notFound -> notFound.then(() -> {
                restaurantService.create(RestaurantItem.from(restaurant));
                return created(URI.create(PATH + "/" + restaurantName))
                        .build();
            });
        };
    }

    @Override
    public ResponseEntity<Void> renameRestaurant(String restaurantName,
                                                 RenameRestaurantRequest renameRequest) {
        return switch (restaurantService.fetch(restaurantName)) {
            case NotFound notFound -> notFound.response();
            case Found found -> found.thenReturn(restaurant -> {
                var newName = renameRequest.getName();
                if (restaurant.restaurantName().equals(newName)) {
                    return ok().build();
                }
                return switch (restaurantService.fetch(newName)) {
                    case Found foundNew -> foundNew.thenReturn(ignored -> status(CONFLICT).build());
                    case NotFound notFoundNew -> notFoundNew.then(() -> {
                        restaurantService.rename(restaurant, newName);
                        return ok().build();
                    });
                };
            });
        };
    }

    @Override
    public ResponseEntity<Void> deleteRestaurant(String restaurantName) {
        return switch (restaurantService.fetch(restaurantName)) {
            case Found found -> found.then(restaurantService::delete);
            case NotFound notFound -> notFound.response();
        };
    }

    @Override
    public ResponseEntity<Void> updateRestaurantRating(String restaurantName,
                                                       UpdateRestaurantRatingRequest ratingRequest) {
        return switch (restaurantService.fetch(restaurantName)) {
            case Found found -> found.then(restaurant -> {
                restaurantService.setRating(restaurant, ratingRequest.getRating());
            });
            case NotFound notFound -> notFound.response();
        };
    }

    @Override
    public ResponseEntity<Void> updateRestaurantReview(String restaurantName,
                                                       UpdateRestaurantReviewRequest reviewRequest) {
        return switch (restaurantService.fetch(restaurantName)) {
            case Found found -> found.then(restaurant -> {
                restaurantService.setReview(restaurant, reviewRequest.getReview());
            });
            case NotFound notFound -> notFound.response();
        };
    }

    @Override
    public ResponseEntity<Void> deleteRestaurantReview(String restaurantName) {
        return switch (restaurantService.fetch(restaurantName)) {
            case Found found -> found.then(restaurant -> {
                restaurantService.setReview(restaurant, null);
            });
            case NotFound notFound -> notFound.response();
        };
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
        return switch (restaurantService.fetch(restaurantName)) {
            case Found found -> found.then(restaurant -> {
                var categoryToAdd = categoryRequest.getName();
                var categories = new HashSet<>(restaurant.categories());
                if (!categories.contains(categoryToAdd)) {
                    categories.add(categoryToAdd);
                    restaurantService.replaceCategories(restaurant, categories);
                }
                return categories;
            });
            case NotFound notFound -> notFound.response();
        };
    }

    @Override
    public ResponseEntity<Void> replaceRestaurantCategories(String restaurantName, Set<Category> categories) {
        return switch (restaurantService.fetch(restaurantName)) {
            case Found found -> found.then(restaurant -> {
                restaurantService.replaceCategories(restaurant, categories);
            });
            case NotFound notFound -> notFound.response();
        };
    }

    @Override
    public ResponseEntity<Void> removeRestaurantCategory(String restaurantName, Category category) {
        return switch (restaurantService.fetch(restaurantName)) {
            case Found found -> found.then(restaurant -> {
                if (restaurant.categories().contains(category)) {
                    var categories = new HashSet<>(restaurant.categories());
                    categories.remove(category);
                    restaurantService.replaceCategories(restaurant, categories);
                }
            });
            case NotFound notFound -> notFound.response();
        };
    }

    @Override
    public ResponseEntity<Set<String>> addRestaurantNote(String restaurantName,
                                                         AddRestaurantNoteRequest noteRequest) {
        return switch (restaurantService.fetch(restaurantName)) {
            case Found found -> found.then(restaurant -> {
                var notes = new ArrayList<>(restaurant.notes());
                notes.add(noteRequest.getNote());
                restaurantService.replaceNotes(restaurant, notes);
            });
            case NotFound notFound -> notFound.response();
        };
    }

    @Override
    public ResponseEntity<Void> replaceRestaurantNote(String restaurantName, Integer noteIndex,
                                                      AddRestaurantNoteRequest addNoteRequest) {
        if (isInvalidIndex(noteIndex)) {
            return ResponseEntity.badRequest()
                                 .build();
        }
        return switch (restaurantService.fetch(restaurantName)) {
            case Found found -> found.then(restaurant -> {
                var currentNotes = restaurant.notes();
                var notesCount = currentNotes.size();
                if (notesCount > noteIndex) {
                    var notes = splice(currentNotes, noteIndex, addNoteRequest.getNote());
                    restaurantService.replaceNotes(restaurant, notes);
                }
            });
            case NotFound notFound -> notFound.response();
        };
    }

    @Override
    public ResponseEntity<Void> removeRestaurantNote(String restaurantName, Integer noteIndex) {
        if (isInvalidIndex(noteIndex)) {
            return ResponseEntity.badRequest()
                                 .build();
        }
        return switch (restaurantService.fetch(restaurantName)) {
            case Found found -> found.then(restaurant -> {
                var currentNotes = restaurant.notes();
                var notesCount = currentNotes.size();
                if (notesCount > noteIndex) {
                    var notes = splice(currentNotes, noteIndex);
                    restaurantService.replaceNotes(restaurant, notes);
                }
            });
            case NotFound notFound -> notFound.response();
        };
    }

    private ResponseEntity<Void> changeTriedFlag(String restaurantName, boolean tried) {
        return switch (restaurantService.fetch(restaurantName)) {
            case Found found -> found.then(restaurant -> {
                restaurantService.setTriedBefore(restaurant, tried);
            });
            case NotFound notFound -> notFound.response();
        };
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
        return noteIndex == null || noteIndex < 1;
    }

}
