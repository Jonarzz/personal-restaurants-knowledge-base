package io.github.jonarzz.restaurant.knowledge.domain;

import static io.github.jonarzz.restaurant.knowledge.domain.Category.*;
import static io.github.jonarzz.restaurant.knowledge.domain.RestaurantEntryContractTestBase.*;
import static org.mockito.Mockito.*;

import java.util.*;

class RestaurantServiceStub {

    private static final RestaurantRepository REPOSITORY = mock(RestaurantRepository.class);
    private static final RestaurantDomainFactory FACTORY = new RestaurantDomainFactory();
    static final RestaurantService INSTANCE = spy(FACTORY.restaurantDynamoDbService(REPOSITORY));

    static final RestaurantItem KFC_CITY_CENTRE = RestaurantItem.builder()
                                                                .userId(TEST_USER)
                                                                .restaurantName("KFC City Centre")
                                                                .category(FAST_FOOD)
                                                                .category(CHICKEN)
                                                                .triedBefore(true)
                                                                .rating(4)
                                                                .review("Not my gig")
                                                                .note("Try to avoid it")
                                                                .build();
    static final RestaurantItem KFC_SOME_STREET = RestaurantItem.builder()
                                                                .userId(TEST_USER)
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

    private RestaurantServiceStub() {
    }

    private static void mockQuery() {
        when(REPOSITORY.query(new RestaurantDynamoDbCriteria(
                RestaurantQueryCriteria.builder()
                                       .nameBeginsWith("KF")
                                       .category(FAST_FOOD)
                                       .triedBefore(true)
                                       .ratingAtLeast(3)
                                       .build())))
                .thenReturn(List.of(KFC_CITY_CENTRE,
                                    KFC_SOME_STREET));
    }

    private static void mockFetch() {
        when(REPOSITORY.findByKey(any()))
                .thenReturn(Optional.empty());
        when(REPOSITORY.findByKey(KFC_CITY_CENTRE.getKey()))
                .thenReturn(Optional.of(KFC_CITY_CENTRE));
        when(REPOSITORY.findByKey(KFC_SOME_STREET.getKey()))
                .thenReturn(Optional.of(KFC_SOME_STREET));
    }

}
