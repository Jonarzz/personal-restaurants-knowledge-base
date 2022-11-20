package io.github.jonarzz.restaurant.knowledge;

import static io.github.jonarzz.restaurant.knowledge.CacheConfig.*;

import lombok.*;
import org.springframework.cache.annotation.*;

import java.util.*;

import io.github.jonarzz.restaurant.knowledge.common.*;
import io.github.jonarzz.restaurant.knowledge.domain.*;

@AllArgsConstructor
@org.springframework.cache.annotation.CacheConfig(cacheNames = RESTAURANT_CACHE_NAME)
public class CachingRestaurantService implements RestaurantService {

    private RestaurantService decorated;

    @Override
    @Cacheable(key = "T(io.github.jonarzz.restaurant.knowledge.technical.auth.SecurityContext).getUserId() + #a0")
    public Optional<RestaurantData> fetch(String restaurantName) {
        return decorated.fetch(restaurantName);
    }

    @Override
    public List<RestaurantData> query(RestaurantQueryCriteria criteria) {
        return decorated.query(criteria);
    }

    @Override
    @CacheEvict(key = "T(io.github.jonarzz.restaurant.knowledge.technical.auth.SecurityContext).getUserId() + #a0.name()")
    public ModificationResultType create(RestaurantData restaurantData) {
        return decorated.create(restaurantData);
    }

    @Override
    @CacheEvict(key = "T(io.github.jonarzz.restaurant.knowledge.technical.auth.SecurityContext).getUserId() + #a0")
    public ModificationResult<RestaurantData> update(String restaurantName, RestaurantData updateData) {
        return decorated.update(restaurantName, updateData);
    }

    @Override
    @CacheEvict(key = "T(io.github.jonarzz.restaurant.knowledge.technical.auth.SecurityContext).getUserId() + #a0")
    public ModificationResultType delete(String restaurantName) {
        return decorated.delete(restaurantName);
    }

    @Override
    @CacheEvict(key = "T(io.github.jonarzz.restaurant.knowledge.technical.auth.SecurityContext).getUserId() + #a0")
    public ModificationResultType setRating(String restaurantName, int rating) {
        return decorated.setRating(restaurantName, rating);
    }

    @Override
    @CacheEvict(key = "T(io.github.jonarzz.restaurant.knowledge.technical.auth.SecurityContext).getUserId() + #a0")
    public ModificationResultType setReview(String restaurantName, String review) {
        return decorated.setReview(restaurantName, review);
    }

    @Override
    @CacheEvict(key = "T(io.github.jonarzz.restaurant.knowledge.technical.auth.SecurityContext).getUserId() + #a0")
    public ModificationResultType setTriedBefore(String restaurantName, boolean tried) {
        return decorated.setTriedBefore(restaurantName, tried);
    }

    @Override
    @CacheEvict(key = "T(io.github.jonarzz.restaurant.knowledge.technical.auth.SecurityContext).getUserId() + #a0")
    public ModificationResultType replaceCategories(String restaurantName, Set<Category> categories) {
        return decorated.replaceCategories(restaurantName, categories);
    }

    @Override
    @CacheEvict(key = "T(io.github.jonarzz.restaurant.knowledge.technical.auth.SecurityContext).getUserId() + #a0")
    public ModificationResultType replaceNotes(String restaurantName, List<String> notes) {
        return decorated.replaceNotes(restaurantName, notes);
    }

    RestaurantService getDecoratedService() {
        return decorated;
    }
}
