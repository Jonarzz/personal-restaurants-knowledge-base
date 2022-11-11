package io.github.jonarzz.restaurant.knowledge;

import org.springframework.cache.*;
import org.springframework.cache.annotation.*;
import org.springframework.cache.concurrent.*;
import org.springframework.context.annotation.*;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String RESTAURANTS_CACHE_NAME = "restaurants";

    @Bean
    CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(RESTAURANTS_CACHE_NAME);
    }

}
