package io.github.jonarzz.restaurant.knowledge.domain;

import static io.github.jonarzz.restaurant.knowledge.model.Category.*;
import static org.mockito.Mockito.*;

import java.util.*;

class RestaurantServiceMock {

    static final RestaurantDynamoDbService MOCK_INSTANCE = mock(RestaurantDynamoDbService.class);

    static final RestaurantItem KFC_CITY_CENTRE = RestaurantItem.builder()
                                                                .restaurantName("KFC City Centre")
                                                                .category(FAST_FOOD)
                                                                .category(CHICKEN)
                                                                .triedBefore(true)
                                                                .rating(4)
                                                                .review("Not my gig")
                                                                .note("Try to avoid it")
                                                                .build();
    static final RestaurantItem KFC_SOME_STREET = RestaurantItem.builder()
                                                                .restaurantName("KFC Some Street")
                                                                .category(FAST_FOOD)
                                                                .category(CHICKEN)
                                                                .triedBefore(true)
                                                                .rating(5)
                                                                .build();

    static {
        mockFetch();
        mockQuery();
    }

    private RestaurantServiceMock() {
    }

    private static void mockQuery() {
        when(MOCK_INSTANCE.query(RestaurantQueryCriteria.builder()
                                                        .nameBeginsWith("KF")
                                                        .category(FAST_FOOD)
                                                        .triedBefore(true)
                                                        .ratingAtLeast(3)
                                                        .build()))
                .thenReturn(List.of(KFC_CITY_CENTRE,
                                    KFC_SOME_STREET));
    }

    private static void mockFetch() {
        when(MOCK_INSTANCE.fetch(any()))
                .thenReturn(Optional.empty());
        when(MOCK_INSTANCE.fetch(KFC_CITY_CENTRE.restaurantName()))
                .thenReturn(Optional.of(KFC_CITY_CENTRE));
        when(MOCK_INSTANCE.fetch(KFC_SOME_STREET.restaurantName()))
                .thenReturn(Optional.of(KFC_SOME_STREET));
    }

}
