package io.github.jonarzz.restaurant.knowledge;

import static io.github.jonarzz.restaurant.knowledge.FetchResult.*;

import org.springframework.http.*;

import java.util.function.*;

sealed interface FetchResult permits Found, NotFound {

    final class Found implements FetchResult {

        private final RestaurantRow restaurant;

        Found(RestaurantRow restaurant) {
            this.restaurant = restaurant;
        }

        <T> ResponseEntity<T> then(Consumer<RestaurantRow> action) {
            action.accept(restaurant);
            return ResponseEntity.ok()
                                 .build();
        }

        <T> ResponseEntity<T> then(Function<RestaurantRow, T> action) {
            var result = action.apply(restaurant);
            return ResponseEntity.ok()
                                 .body(result);
        }

        <T> ResponseEntity<T> thenReturn(Function<RestaurantRow, ResponseEntity<T>> supplier) {
            return supplier.apply(restaurant);
        }
    }

    final class NotFound implements FetchResult {

        <T> ResponseEntity<T> then(Supplier<ResponseEntity<T>> supplier) {
            return supplier.get();
        }

        public <T> ResponseEntity<T> response() {
            return ResponseEntity.notFound()
                                 .build();
        }
    }

}
