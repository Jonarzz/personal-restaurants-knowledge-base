package io.github.jonarzz.restaurant.knowledge.entry;

import static io.github.jonarzz.restaurant.knowledge.model.Category.*;
import static org.mockito.Mockito.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.*;

import io.restassured.module.mockmvc.*;
import org.junit.jupiter.api.*;
import org.mockito.exceptions.misusing.*;
import org.mockito.invocation.*;
import org.mockito.stubbing.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;

import java.util.*;

@SpringBootTest(
        webEnvironment = MOCK,
        classes = {RestaurantController.class, RestaurantEntryContractTestBase.Config.class}
)
public class RestaurantEntryContractTestBase {

    static final RestaurantService RESTAURANT_SERVICE_MOCK = mock(RestaurantService.class,
                                                                  new ThrowWhenNoStubbingAnswer());

    static {
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
        ).when(RESTAURANT_SERVICE_MOCK)
         .query(RestaurantQueryCriteria.builder()
                                       .nameBeginsWith("KF")
                                       .category(FAST_FOOD)
                                       .triedBefore(true)
                                       .ratingAtLeast(3)
                                       .build());
    }

    @Configuration
    @EnableAutoConfiguration
    static class Config {

        @Bean
        RestaurantService responseProvider() {
            return RESTAURANT_SERVICE_MOCK;
        }

    }

    @Autowired
    RestaurantController restaurantController;

    @BeforeEach
    public void setup() {
        RestAssuredMockMvc.standaloneSetup(restaurantController);
    }

    private static class ThrowWhenNoStubbingAnswer implements Answer<Object> {

        @Override
        public Object answer(InvocationOnMock invocation) {
            throw new PotentialStubbingProblem(
                    "Method %s is not mocked for parameters: %s".formatted(
                            invocation.getMethod().getName(), Arrays.toString(invocation.getArguments()))
            );
        }
    }
}
