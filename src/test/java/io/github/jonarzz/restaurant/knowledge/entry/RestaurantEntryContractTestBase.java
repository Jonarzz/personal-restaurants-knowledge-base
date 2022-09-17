package io.github.jonarzz.restaurant.knowledge.entry;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.*;

import io.restassured.module.mockmvc.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;

@SpringBootTest(
        webEnvironment = MOCK,
        classes = {RestaurantController.class, RestaurantEntryContractTestBase.Config.class}
)
class RestaurantEntryContractTestBase {

    @Configuration
    @EnableAutoConfiguration
    static class Config {

        @Bean
        RestaurantService responseProvider() {
            return RestaurantServiceMockProvider.get();
        }

    }

    @Autowired
    RestaurantController restaurantController;

    @BeforeEach
    void setup() {
        RestAssuredMockMvc.standaloneSetup(restaurantController);
    }

}
