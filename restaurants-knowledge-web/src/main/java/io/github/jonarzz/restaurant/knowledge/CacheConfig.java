package io.github.jonarzz.restaurant.knowledge;

import org.springframework.cache.*;
import org.springframework.cache.annotation.*;
import org.springframework.cache.concurrent.*;
import org.springframework.context.annotation.*;

import io.github.jonarzz.restaurant.knowledge.domain.*;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                RestaurantService.CACHE_NAME
        );
    }

}
