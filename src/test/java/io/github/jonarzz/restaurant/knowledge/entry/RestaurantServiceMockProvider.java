package io.github.jonarzz.restaurant.knowledge.entry;

import static io.github.jonarzz.restaurant.knowledge.model.Category.*;
import static org.mockito.Mockito.*;

import org.mockito.exceptions.misusing.*;
import org.mockito.invocation.*;
import org.mockito.stubbing.*;

import java.util.*;

class RestaurantServiceMockProvider {

    static RestaurantService get() {
        var serviceMock = mock(RestaurantService.class, new ThrowWhenNoStubbingAnswer());
        mockQuery(serviceMock);
        return serviceMock;
    }

    private static void mockQuery(RestaurantService serviceMock) {
        doReturn(List.of(
                RestaurantItem.builder()
                              .restaurantName("KFC City Centre")
                              .category(FAST_FOOD)
                              .category(CHICKEN)
                              .triedBefore(true)
                              .rating(4)
                              .review("Not my gig")
                              .build(),
                RestaurantItem.builder()
                              .restaurantName("KFC Some Street")
                              .category(FAST_FOOD)
                              .category(CHICKEN)
                              .triedBefore(true)
                              .rating(5)
                              .build())
        ).when(serviceMock)
         .query(RestaurantQueryCriteria.builder()
                                       .nameBeginsWith("KF")
                                       .category(FAST_FOOD)
                                       .triedBefore(true)
                                       .ratingAtLeast(3)
                                       .build());
    }

    private static class ThrowWhenNoStubbingAnswer implements Answer<Object> {

        @Override
        public Object answer(InvocationOnMock invocation) {
            throw new PotentialStubbingProblem(
                    "Method %s is not mocked for parameters: %s".formatted(
                            invocation.getMethod().getName(),
                            Arrays.toString(invocation.getArguments()))
            );
        }
    }

}
