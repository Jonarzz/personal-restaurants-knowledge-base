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
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
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
                RestaurantConfig.class, CacheConfig.class, RestaurantDynamoDbCacheTest.Config.class
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

    static DynamoDbRepository<RestaurantItem, RestaurantKey> repositorySpy;

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("amazon.aws.dynamodb-url",
                     () -> "http://localhost:" + dynamoDbContainer.getFirstMappedPort());
    }

    @TestConfiguration
    static class Config {

        @Bean
        @Primary
        @SuppressWarnings("unchecked")
        RestaurantDomainFactory restaurantDomainDynamoDbFactory(DynamoDbClient dynamoDbClient) {
            var factory = spy(new RestaurantDomainFactory(dynamoDbClient));
            when(factory.restaurantDynamoDbRepository())
                    .thenAnswer(invocation -> {
                        var repository = (DynamoDbRepository<RestaurantItem, RestaurantKey>) invocation.callRealMethod();
                        return repositorySpy = spy(repository);
                    });
            return factory;
        }

    }

    @Autowired
    DynamoDbClient amazonDynamoDb;
    @Autowired
    RestaurantService restaurantService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        setUpSecurityContext(TEST_USER);
        Mockito.clearInvocations(repositorySpy);
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
    void findNotTriedByUserIdAndName_cacheShouldBeUsed() {
        assertRestaurantFound(NOT_TRIED_RESTAURANT_NAME);

        verifyNoInteractions(repositorySpy);
    }

    @RepeatedTest(5)
    @Order(25)
    void findTriedByUserIdAndName_cacheShouldBeUsed() {
        assertRestaurantFound(TRIED_RESTAURANT_NAME);

        verifyNoInteractions(repositorySpy);
    }

    @Order(25)
    @ParameterizedTest(name = "restaurant name = {0}")
    @ValueSource(strings = {TRIED_RESTAURANT_NAME, NOT_TRIED_RESTAURANT_NAME})
    void findForOtherUser_cacheShouldNotBeUsed(String restaurantName) {
        setUpSecurityContext("some other user");

        optionalRestaurantWithName(restaurantName)
                .isEmpty();
        verify(repositorySpy)
                .findByKey(any());
    }

    private ObjectAssert<RestaurantData> assertRestaurantFound(String restaurantName) {
        return optionalRestaurantWithName(restaurantName)
                .get(type(RestaurantData.class));
    }

    private OptionalAssert<RestaurantData> optionalRestaurantWithName(String restaurantName) {
        return assertThat(restaurantService.fetch(restaurantName))
                .as("Restaurant with name: " + restaurantName);
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

    private static void setUpSecurityContext(String userId) {
        SecurityContext.setUserId(userId);
    }
}