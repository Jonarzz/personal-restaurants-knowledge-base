package io.github.jonarzz.restaurant.knowledge;

import org.springframework.context.annotation.*;

import io.github.jonarzz.restaurant.knowledge.api.*;
import io.github.jonarzz.restaurant.knowledge.domain.*;

@Configuration
@Import(RestaurantDomainFactory.class)
public class ApiConfig {

    @Bean
    public RestaurantController restaurantController(RestaurantService restaurantService) {
        return new RestaurantController(restaurantService);
    }

}
