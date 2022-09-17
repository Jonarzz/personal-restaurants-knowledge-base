package io.github.jonarzz.restaurant.knowledge.entry;

import static io.github.jonarzz.restaurant.knowledge.model.Category.*;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.InstanceOfAssertFactories.*;
import static org.junit.jupiter.api.TestInstance.Lifecycle.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.*;
import static software.amazon.awssdk.regions.Region.*;
import static software.amazon.awssdk.services.dynamodb.model.KeyType.*;
import static software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType.*;

import org.assertj.core.api.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.*;
import org.springframework.test.context.*;
import org.testcontainers.containers.*;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.*;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.services.dynamodb.*;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import io.github.jonarzz.restaurant.knowledge.dynamodb.*;

@Testcontainers
@SpringBootTest(
        webEnvironment = NONE,
        classes = {
                DynamoDbConfig.class, RestaurantEntryManagementConfig.class, RestaurantServiceTest.Configuration.class
        }
)
@TestPropertySource(properties = {
        "amazon.aws.accesskey = dummy-access",
        "amazon.aws.secretkey = dummy-secret"
})
@TestInstance(PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class RestaurantServiceTest {

    static final String TEST_USER = "test-user";
    static final String TRIED_RESTAURANT_NAME = "Subway";
    static final String TRIED_RESTAURANT_RENAMED = "Subway Nowy Świat";
    static final String NOT_TRIED_RESTAURANT_NAME = "Burger King";
    static final int FILLER_ENTRIES_COUNT = 50;

    @Container
    static final GenericContainer<?> dynamoDbContainer = new GenericContainer<>("amazon/dynamodb-local:latest")
            .withCommand("-jar DynamoDBLocal.jar -inMemory -sharedDb")
            .withExposedPorts(8000);

    @TestConfiguration
    static class Configuration {

        @Bean
        @Primary
        DynamoDbClient amazonDynamoDB(AwsCredentialsProvider awsCredentialsProvider) {
            dynamoDbContainer.start();
            return DynamoDbClient.builder()
                                 .endpointOverride(URI.create("http://localhost:" + dynamoDbContainer.getFirstMappedPort()))
                                 .region(EU_CENTRAL_1)
                                 .credentialsProvider(awsCredentialsProvider)
                                 .build();
        }

    }
    @Autowired
    DynamoDbClient amazonDynamoDb;

    @Autowired
    RestaurantService restaurantService;
    @BeforeAll
    void beforeAll() {
        amazonDynamoDb.createTable(prepareCreateTableRequest());
    }

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext()
                             .setAuthentication(new TestingAuthenticationToken(TEST_USER, null));
    }

    @Test
    @Order(10)
    void findByUserIdAndName_emptyTable() {
        assertThat(restaurantService.fetch(NOT_TRIED_RESTAURANT_NAME))
                .isInstanceOf(FetchResult.NotFound.class);
        assertThat(restaurantService.fetch(TRIED_RESTAURANT_NAME))
                .isInstanceOf(FetchResult.NotFound.class);
        assertThat(restaurantService.fetch(TRIED_RESTAURANT_RENAMED))
                .isInstanceOf(FetchResult.NotFound.class);
    }

    @Test
    @Order(20)
    void createFirstRestaurantEntry() {
        var toSave = RestaurantItem.builder()
                                   .userId(TEST_USER)
                                   .restaurantName(NOT_TRIED_RESTAURANT_NAME)
                                   .category(FAST_FOOD)
                                   .category(BURGER)
                                   .build();

        assertThatNoException()
                .isThrownBy(() -> restaurantService.create(toSave));
    }

    @Test
    @Order(20)
    void createSecondRestaurantEntry() {
        var toSave = RestaurantItem.builder()
                                   .userId(TEST_USER)
                                   .restaurantName(TRIED_RESTAURANT_NAME)
                                   .category(FAST_FOOD)
                                   .category(SANDWICH)
                                   .triedBefore(true)
                                   .rating(6)
                                   .review("Good enough")
                                   .note("Tuna should be ordered cold")
                                   .note("Don't go too heavy on the veggies")
                                   .build();

        assertThatNoException()
                .isThrownBy(() -> restaurantService.create(toSave));
    }

    @ParameterizedTest
    @MethodSource("intRange")
    @Order(20)
    void createManyRestaurantEntries(int number) {
        var toSave = RestaurantItem.builder()
                                   .userId(TEST_USER)
                                   .restaurantName("one-of-many-" + number)
                                   .category(OTHER)
                                   .build();

        assertThatNoException()
                .isThrownBy(() -> restaurantService.create(toSave));
    }

    IntStream intRange() {
        return IntStream.rangeClosed(1, FILLER_ENTRIES_COUNT);
    }

    @Test
    @Order(30)
    void findNotTriedByUserIdAndName_afterAddingTwo() {
        var result = assertRestaurantFound(NOT_TRIED_RESTAURANT_NAME);

        assertNotTriedRestaurantInitial(result);
    }

    @Test
    @Order(30)
    void findTriedByUserIdAndName_afterAddingTwo() {
        var restaurant = assertRestaurantFound(TRIED_RESTAURANT_NAME);

        assertTriedRestaurantInitial(restaurant);
    }

    @Test
    @Order(30)
    void queryByName_noResults() {
        var criteria = RestaurantQueryCriteria.builder()
                                              .nameBeginsWith("i do not exist")
                                              .build();

        var result = restaurantService.query(criteria);

        assertThat(result)
                .isEmpty();
    }

    @Test
    @Order(30)
    void queryByNameStartingWith_triedRestaurant() {
        var criteria = RestaurantQueryCriteria.builder()
                                              .nameBeginsWith(TRIED_RESTAURANT_NAME.substring(0, 3))
                                              .build();

        var result = restaurantService.query(criteria);

        assertTriedRestaurantInitial(assertThat(result)
                                             .singleElement());
    }

    @Test
    @Order(30)
    void queryByNameStartingWith_notTriedRestaurant() {
        var criteria = RestaurantQueryCriteria.builder()
                                              .nameBeginsWith(NOT_TRIED_RESTAURANT_NAME.substring(0, 5))
                                              .build();

        var result = restaurantService.query(criteria);

        assertNotTriedRestaurantInitial(assertThat(result)
                                                .singleElement());
    }

    @Test
    @Order(30)
    void queryByTriedBefore_triedRestaurant() {
        var criteria = RestaurantQueryCriteria.builder()
                                              .triedBefore(true)
                                              .build();

        var result = restaurantService.query(criteria);

        assertTriedRestaurantInitial(assertThat(result)
                                              .singleElement());
    }

    @Test
    @Order(30)
    void queryByTriedBefore_notTriedRestaurant() {
        var criteria = RestaurantQueryCriteria.builder()
                                              .triedBefore(false)
                                              .build();

        var result = restaurantService.query(criteria);

        assertThat(result)
                .hasSize(1 + FILLER_ENTRIES_COUNT)
                .anySatisfy(restaurant -> assertNotTriedRestaurantInitial(assertThat(restaurant)));
    }

    @Test
    @Order(30)
    void queryByCommonCategory() {
        var criteria = RestaurantQueryCriteria.builder()
                                              .category(FAST_FOOD)
                                              .build();

        var result = restaurantService.query(criteria);

        assertThat(result)
                .hasSize(2)
                .anySatisfy(restaurant -> assertTriedRestaurantInitial(assertThat(restaurant)))
                .anySatisfy(restaurant -> assertNotTriedRestaurantInitial(assertThat(restaurant)));
    }

    @Test
    @Order(30)
    void queryByCategory_triedBefore() {
        var criteria = RestaurantQueryCriteria.builder()
                                              .category(SANDWICH)
                                              .build();

        var result = restaurantService.query(criteria);

        assertTriedRestaurantInitial(assertThat(result)
                                             .singleElement());
    }

    @Test
    @Order(30)
    void queryByCategory_notTriedBefore() {
        var criteria = RestaurantQueryCriteria.builder()
                                              .category(BURGER)
                                              .build();

        var result = restaurantService.query(criteria);

        assertNotTriedRestaurantInitial(assertThat(result)
                                                .singleElement());
    }

    @Test
    @Order(30)
    void queryByAll_triedRestaurant() {
        var criteria = RestaurantQueryCriteria.builder()
                                              .nameBeginsWith(TRIED_RESTAURANT_NAME.substring(0, 2))
                                              .category(FAST_FOOD)
                                              .triedBefore(true)
                                              .build();

        var result = restaurantService.query(criteria);

        assertTriedRestaurantInitial(assertThat(result)
                                              .singleElement());
    }

    @Test
    @Order(30)
    void queryByRating_triedRestaurant() {
        var criteria = RestaurantQueryCriteria.builder()
                                              .ratingAtLeast(6)
                                              .build();

        var result = restaurantService.query(criteria);

        assertTriedRestaurantInitial(assertThat(result)
                                              .singleElement());
    }

    @Test
    @Order(30)
    void queryByRating_noResults() {
        var criteria = RestaurantQueryCriteria.builder()
                                              .ratingAtLeast(7)
                                              .build();

        var result = restaurantService.query(criteria);

        assertThat(result)
                .isEmpty();
    }

    @Test
    @Order(30)
    void queryByAll_notTriedRestaurant() {
        var criteria = RestaurantQueryCriteria.builder()
                                              .nameBeginsWith(NOT_TRIED_RESTAURANT_NAME.substring(0, 4))
                                              .category(BURGER)
                                              .triedBefore(false)
                                              // ignored because of triedBefore set to false
                                              .ratingAtLeast(9)
                                              .build();

        var result = restaurantService.query(criteria);

        assertNotTriedRestaurantInitial(assertThat(result)
                                                .singleElement());
    }

    @Test
    @Order(30)
    void queryByAll_noResults() {
        var criteria = RestaurantQueryCriteria.builder()
                                              .nameBeginsWith(NOT_TRIED_RESTAURANT_NAME.substring(0, 4))
                                              .category(FAST_FOOD)
                                              .triedBefore(true)
                                              .ratingAtLeast(3)
                                              .build();

        var result = restaurantService.query(criteria);

        assertThat(result)
                .isEmpty();
    }

    @Test
    @Order(30)
    void queryWithEmptyCriteria() {
        var criteria = RestaurantQueryCriteria.builder()
                                              .build();

        ThrowableAssert.ThrowingCallable queryMethod = () -> restaurantService.query(criteria);

        assertThatThrownBy(queryMethod)
                .hasMessage("Query criteria cannot be empty");
    }

    @Test
    @Order(40)
    void deleteRestaurant() {
        var restaurantName = "for removal";
        var toSave = RestaurantItem.builder()
                                   .userId(TEST_USER)
                                   .restaurantName(restaurantName)
                                   .category(PIZZA)
                                   .build();
        restaurantService.create(toSave);

        actOn(restaurantName, restaurantService::delete);

        assertThat(restaurantService.fetch(restaurantName))
                .isInstanceOf(FetchResult.NotFound.class);
    }

    @Test
    @Order(40)
    void renameRestaurant() {
        var oldName = TRIED_RESTAURANT_NAME;
        var newName = TRIED_RESTAURANT_RENAMED;

        actOn(oldName,
              restaurant -> restaurantService.rename(restaurant, newName));

        assertThat(restaurantService.fetch(oldName))
                .as("Restaurant fetched by old name")
                .isInstanceOf(FetchResult.NotFound.class);
        assertRestaurantFound(newName)
                .returns(TEST_USER, RestaurantItem::userId)
                .returns(newName, RestaurantItem::restaurantName)
                .returns(Set.of(FAST_FOOD, SANDWICH), RestaurantItem::categories)
                .returns(true, RestaurantItem::triedBefore)
                .returns(6, RestaurantItem::rating)
                .returns("Good enough", RestaurantItem::review)
                .returns(List.of(
                        "Tuna should be ordered cold",
                        "Don't go too heavy on the veggies"
                ), RestaurantItem::notes);
    }

    @Test
    @Order(FILLER_ENTRIES_COUNT)
    void replaceCategories() {
        var newCategories = Set.of(VEGAN, FAST_FOOD, SANDWICH, CHICKEN);
        var restaurantName = TRIED_RESTAURANT_RENAMED;

        actOn(restaurantName,
              restaurant -> restaurantService.replaceCategories(restaurant, newCategories));

        assertRestaurantFound(restaurantName)
                .returns(newCategories, RestaurantItem::categories);
    }

    @Test
    @Order(FILLER_ENTRIES_COUNT)
    void replaceNotes() {
        var newNotes = List.of("No hot tuna!", "4-5 veggies is enough");
        var restaurantName = TRIED_RESTAURANT_RENAMED;

        actOn(restaurantName,
              restaurant -> restaurantService.replaceNotes(restaurant, newNotes));

        assertRestaurantFound(restaurantName)
                .returns(newNotes, RestaurantItem::notes);
    }

    @Test
    @Order(FILLER_ENTRIES_COUNT)
    void setRating() {
        var newRating = 7;
        var restaurantName = NOT_TRIED_RESTAURANT_NAME;

        actOn(restaurantName,
              restaurant -> restaurantService.setRating(restaurant, newRating));

        assertRestaurantFound(restaurantName)
                .returns(newRating, RestaurantItem::rating)
                .returns(true, RestaurantItem::triedBefore);
    }

    @Test
    @Order(60)
    void setNotVisited_afterSettingRating() {
        var restaurantName = NOT_TRIED_RESTAURANT_NAME;

        actOn(restaurantName,
              restaurant -> restaurantService.setTriedBefore(restaurant, false));

        assertRestaurantFound(restaurantName)
                .returns(false, RestaurantItem::triedBefore)
                .returns(null, RestaurantItem::rating);
    }

    @Test
    @Order(70)
    void setReview() {
        var newReview = "It's all right";
        var restaurantName = NOT_TRIED_RESTAURANT_NAME;

        actOn(restaurantName,
              restaurant -> restaurantService.setReview(restaurant, newReview));

        assertRestaurantFound(restaurantName)
                .returns(newReview, RestaurantItem::review)
                .returns(true, RestaurantItem::triedBefore);
    }

    @Test
    @Order(80)
    void setNotVisited_afterSettingReview() {
        var restaurantName = NOT_TRIED_RESTAURANT_NAME;

        actOn(restaurantName,
              restaurant -> restaurantService.setTriedBefore(restaurant, false));

        assertRestaurantFound(restaurantName)
                .returns(false, RestaurantItem::triedBefore)
                .returns(null, RestaurantItem::review);
    }

    @Test
    @Order(90)
    void setVisited() {
        var restaurantName = NOT_TRIED_RESTAURANT_NAME;

        actOn(restaurantName,
              restaurant -> restaurantService.setTriedBefore(restaurant, true));

        assertRestaurantFound(restaurantName)
                .returns(true, RestaurantItem::triedBefore)
                .returns(null, RestaurantItem::rating)
                .returns(null, RestaurantItem::review);
    }

    private void actOn(String restaurantName, Consumer<RestaurantItem> action) {
        if (!(restaurantService.fetch(restaurantName)
                instanceof FetchResult.Found found)) {
            throw new IllegalStateException("Not found restaurant with name " + restaurantName);
        }
        found.then(action);
    }

    private ObjectAssert<RestaurantItem> assertRestaurantFound(String restaurantName) {
        return assertThat(restaurantService.fetch(restaurantName))
                .as("Restaurant with name: " + restaurantName)
                .isInstanceOf(FetchResult.Found.class)
                .extracting("restaurant", type(RestaurantItem.class));
    }

    private static void assertNotTriedRestaurantInitial(ObjectAssert<RestaurantItem> restaurant) {
        restaurant.returns(TEST_USER, RestaurantItem::userId)
                  .returns(NOT_TRIED_RESTAURANT_NAME, RestaurantItem::restaurantName)
                  .returns(Set.of(FAST_FOOD, BURGER), RestaurantItem::categories)
                  .returns(false, RestaurantItem::triedBefore)
                  .returns(null, RestaurantItem::rating)
                  .returns(null, RestaurantItem::review)
                  .returns(List.of(), RestaurantItem::notes);
    }

    private static void assertTriedRestaurantInitial(ObjectAssert<RestaurantItem> restaurant) {
        restaurant.returns(TEST_USER, RestaurantItem::userId)
                  .returns(TRIED_RESTAURANT_NAME, RestaurantItem::restaurantName)
                  .returns(Set.of(FAST_FOOD, SANDWICH), RestaurantItem::categories)
                  .returns(true, RestaurantItem::triedBefore)
                  .returns(6, RestaurantItem::rating)
                  .returns("Good enough", RestaurantItem::review)
                  .returns(List.of(
                          "Tuna should be ordered cold",
                          "Don't go too heavy on the veggies"
                  ), RestaurantItem::notes);
    }

    private static CreateTableRequest prepareCreateTableRequest() {
        var userIdAttribute = "userId";
        var restaurantNameAttribute = "restaurantName";
        return CreateTableRequest.builder()
                                 .tableName("Restaurant")
                                 .keySchema(
                                         KeySchemaElement.builder()
                                                         .attributeName(userIdAttribute)
                                                         .keyType(HASH)
                                                         .build(),
                                         KeySchemaElement.builder()
                                                         .attributeName(restaurantNameAttribute)
                                                         .keyType(RANGE)
                                                         .build()
                                 )
                                 .attributeDefinitions(
                                         AttributeDefinition.builder()
                                                            .attributeName(userIdAttribute)
                                                            .attributeType(S)
                                                            .build(),
                                         AttributeDefinition.builder()
                                                            .attributeName(restaurantNameAttribute)
                                                            .attributeType(S)
                                                            .build()
                                 )
                                 .provisionedThroughput(ProvisionedThroughput.builder()
                                                                             .readCapacityUnits(1L)
                                                                             .writeCapacityUnits(1L)
                                                                             .build())
                                 .build();
    }
}