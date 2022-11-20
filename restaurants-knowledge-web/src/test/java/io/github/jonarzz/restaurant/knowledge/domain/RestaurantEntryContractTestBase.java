package io.github.jonarzz.restaurant.knowledge.domain;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.*;

import io.restassured.module.mockmvc.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;

import io.github.jonarzz.restaurant.knowledge.api.*;
import io.github.jonarzz.restaurant.knowledge.technical.auth.*;

@SpringBootTest(
        webEnvironment = MOCK,
        classes = {RestaurantController.class, RestaurantEntryContractTestBase.Config.class}
)
class RestaurantEntryContractTestBase {

    protected static final String TEST_USER = "test-user";

    @Configuration
    @EnableAutoConfiguration
    static class Config {

        @Bean
        RestaurantService restaurantService() {
            return RestaurantServiceStub.INSTANCE;
        }

    }

    @Autowired
    RestaurantController restaurantController;

    @BeforeEach
    void setup() {
        RestAssuredMockMvc.standaloneSetup(restaurantController);
        SecurityContext.setUserId(TEST_USER);
    }

}
