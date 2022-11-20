package io.github.jonarzz.restaurant.knowledge;

import org.springframework.cache.*;
import org.springframework.cache.annotation.*;
import org.springframework.cache.concurrent.*;
import org.springframework.context.annotation.*;

@Configuration
@EnableCaching
public class CacheConfig {

    static final String RESTAURANT_CACHE_NAME = "RestaurantCache";

    @Bean
    CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(RESTAURANT_CACHE_NAME);
    }

}
