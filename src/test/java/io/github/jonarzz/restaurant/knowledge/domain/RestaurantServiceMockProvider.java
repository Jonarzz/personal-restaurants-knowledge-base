package io.github.jonarzz.restaurant.knowledge.domain;

import static io.github.jonarzz.restaurant.knowledge.model.Category.*;
import static org.mockito.Mockito.*;

import java.util.*;

class RestaurantServiceMockProvider {

    public static final RestaurantItem KFC_CITY_CENTRE = RestaurantItem.builder()
                                                                       .restaurantName("KFC City Centre")
                                                                       .category(FAST_FOOD)
                                                                       .category(CHICKEN)
                                                                       .triedBefore(true)
                                                                       .rating(4)
                                                                       .review("Not my gig")
                                                                       .note("Try to avoid it")
                                                                       .build();
    public static final RestaurantItem KFC_SOME_STREET = RestaurantItem.builder()
                                                                       .restaurantName("KFC Some Street")
                                                                       .category(FAST_FOOD)
                                                                       .category(CHICKEN)
                                                                       .triedBefore(true)
                                                                       .rating(5)
                                                                       .build();

    static RestaurantService get() {
        var serviceMock = mock(RestaurantDynamoDbService.class);
        mockFetch(serviceMock);
        mockQuery(serviceMock);
        return serviceMock;
    }

    private static void mockQuery(RestaurantService serviceMock) {
        when(serviceMock.query(RestaurantQueryCriteria.builder()
                                                      .nameBeginsWith("KF")
                                                      .category(FAST_FOOD)
                                                      .triedBefore(true)
                                                      .ratingAtLeast(3)
                                                      .build()))
                .thenReturn(List.of(KFC_CITY_CENTRE,
                                    KFC_SOME_STREET));
    }

    private static void mockFetch(RestaurantService serviceMock) {
        when(serviceMock.fetch(any()))
                .thenReturn(new FetchResult.NotFound());
        when(serviceMock.fetch(KFC_CITY_CENTRE.restaurantName()))
                .thenReturn(new FetchResult.Found(KFC_CITY_CENTRE));
        when(serviceMock.fetch(KFC_SOME_STREET.restaurantName()))
                .thenReturn(new FetchResult.Found(KFC_SOME_STREET));
    }

}
