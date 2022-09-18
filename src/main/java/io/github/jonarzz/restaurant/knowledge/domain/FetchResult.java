package io.github.jonarzz.restaurant.knowledge.domain;

import static io.github.jonarzz.restaurant.knowledge.domain.FetchResult.*;

import org.springframework.http.*;

import java.util.function.*;

sealed interface FetchResult permits Found, NotFound {

    final class Found implements FetchResult {

        private final RestaurantItem restaurant;

        Found(RestaurantItem restaurant) {
            this.restaurant = restaurant;
        }

        <T> ResponseEntity<T> then(Consumer<RestaurantItem> action) {
            action.accept(restaurant);
            return ResponseEntity.ok()
                                 .build();
        }

        <T> ResponseEntity<T> then(Function<RestaurantItem, T> action) {
            var result = action.apply(restaurant);
            return ResponseEntity.ok()
                                 .body(result);
        }

        <T> ResponseEntity<T> thenReturn(Function<RestaurantItem, ResponseEntity<T>> supplier) {
            return supplier.apply(restaurant);
        }
    }

    final class NotFound implements FetchResult {

        <T> ResponseEntity<T> then(Supplier<ResponseEntity<T>> supplier) {
            return supplier.get();
        }

        <T> ResponseEntity<T> response() {
            return ResponseEntity.notFound()
                                 .build();
        }
    }

}
