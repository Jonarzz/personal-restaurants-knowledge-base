package io.github.jonarzz.restaurant.knowledge.domain;

import static io.github.jonarzz.restaurant.knowledge.domain.Category.*;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.InstanceOfAssertFactories.*;
import static org.junit.jupiter.api.TestInstance.Lifecycle.*;
import static org.mockito.Mockito.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.*;

import org.assertj.core.api.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.mock.mockito.*;
import org.springframework.test.context.*;
import org.testcontainers.containers.*;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.*;
import software.amazon.awssdk.services.dynamodb.*;

import java.util.*;

import io.github.jonarzz.restaurant.knowledge.*;
import io.github.jonarzz.restaurant.knowledge.technical.auth.*;
import io.github.jonarzz.restaurant.knowledge.technical.dynamodb.*;

@Testcontainers
@SpringBootTest(
        webEnvironment = NONE,
        classes = {
                DynamoDbClientFactory.class, ApiConfig.class, CacheConfig.class
        }
)
@TestPropertySource(properties = {
        "amazon.aws.accesskey = dummy-access",
        "amazon.aws.secretkey = dummy-secret"
})
@TestInstance(PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class RestaurantDynamoDbCacheTest {

    static final String TEST_USER = "test-user";
    static final String TRIED_RESTAURANT_NAME = "Subway";
    static final String NOT_TRIED_RESTAURANT_NAME = "Burger King";

    @Container
    static final GenericContainer<?> dynamoDbContainer = new GenericContainer<>("amazon/dynamodb-local:latest")
            .withCommand("-jar DynamoDBLocal.jar -inMemory -sharedDb")
            .withExposedPorts(8000);

    static {
        dynamoDbContainer.start();
    }

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("amazon.aws.dynamodb-url",
                     () -> "http://localhost:" + dynamoDbContainer.getFirstMappedPort());
    }

    @Autowired
    DynamoDbClient amazonDynamoDb;
    @SpyBean
    DynamoDbRepository<RestaurantItem, RestaurantKey> repositorySpy;

    @Autowired
    RestaurantService restaurantService;

    @BeforeEach
    void setUp() {
        setUpSecurityContext();
    }

    @AfterAll
    static void afterAll() {
        dynamoDbContainer.close();
    }

    @Test
    @Order(10)
    void createFirstRestaurantEntry() {
        var toSave = new RestaurantData()
                .name(NOT_TRIED_RESTAURANT_NAME)
                .categories(Set.of(FAST_FOOD, BURGER));

        assertThatNoException()
                .isThrownBy(() -> restaurantService.create(toSave));
    }

    @Test
    @Order(10)
    void createSecondRestaurantEntry() {
        var toSave = new RestaurantData()
                .name(TRIED_RESTAURANT_NAME)
                .categories(Set.of(FAST_FOOD, SANDWICH))
                .triedBefore(true)
                .rating(6);

        assertThatNoException()
                .isThrownBy(() -> restaurantService.create(toSave));
    }

    @Test
    @Order(20)
    void findNotTriedByUserIdAndName_afterAddingTwo() {
        var result = assertRestaurantFound(NOT_TRIED_RESTAURANT_NAME);

        assertNotTriedRestaurantInitial(result);
        // verify that cache was not used
        verify(repositorySpy)
                .findByKey(argThat(key -> NOT_TRIED_RESTAURANT_NAME.toLowerCase()
                                                                   .equals(key.nameLowercase())));
    }

    @Test
    @Order(20)
    void findTriedByUserIdAndName_afterAddingTwo() {
        var restaurant = assertRestaurantFound(TRIED_RESTAURANT_NAME);

        assertTriedRestaurantInitial(restaurant);
        // verify that cache was not used
        verify(repositorySpy)
                .findByKey(argThat(key -> TRIED_RESTAURANT_NAME.toLowerCase()
                                                               .equals(key.nameLowercase())));
    }

    @RepeatedTest(5)
    @Order(25)
    @SuppressWarnings("unchecked")
    void findNotTriedByUserIdAndName_cacheShouldBeUsed() {
        clearInvocations(repositorySpy);

        assertRestaurantFound(NOT_TRIED_RESTAURANT_NAME);

        verifyNoInteractions(repositorySpy);
    }

    @RepeatedTest(5)
    @Order(25)
    @SuppressWarnings("unchecked")
    void findTriedByUserIdAndName_cacheShouldBeUsed() {
        clearInvocations(repositorySpy);

        assertRestaurantFound(TRIED_RESTAURANT_NAME);

        verifyNoInteractions(repositorySpy);
    }

    private ObjectAssert<RestaurantData> assertRestaurantFound(String restaurantName) {
        return assertThat(restaurantService.fetch(restaurantName))
                .as("Restaurant with name: " + restaurantName)
                .get(type(RestaurantData.class));
    }

    private static void assertNotTriedRestaurantInitial(ObjectAssert<RestaurantData> restaurant) {
        restaurant.returns(NOT_TRIED_RESTAURANT_NAME, RestaurantData::name)
                  .returns(Set.of(FAST_FOOD, BURGER), RestaurantData::categories)
                  .returns(false, RestaurantData::triedBefore)
                  .returns(null, RestaurantData::rating)
                  .returns(null, RestaurantData::review)
                  .returns(List.of(), RestaurantData::notes);
    }

    private static void assertTriedRestaurantInitial(ObjectAssert<RestaurantData> restaurant) {
        restaurant.returns(TRIED_RESTAURANT_NAME, RestaurantData::name)
                  .returns(Set.of(FAST_FOOD, SANDWICH), RestaurantData::categories)
                  .returns(true, RestaurantData::triedBefore)
                  .returns(6, RestaurantData::rating);
    }

    private static void setUpSecurityContext() {
        SecurityContext.setUserId(TEST_USER);
    }
}