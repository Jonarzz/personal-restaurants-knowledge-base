package io.github.jonarzz.restaurant.knowledge;

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

@Testcontainers
@SpringBootTest(
        webEnvironment = NONE,
        classes = RestaurantServiceTest.Configuration.class
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
        var toSave = RestaurantRow.builder()
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
        var toSave = RestaurantRow.builder()
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

    @Test
    @Order(30)
    void findNotTriedByUserIdAndName_afterAddingTwo() {
        assertRestaurantFound(NOT_TRIED_RESTAURANT_NAME)
                .returns(TEST_USER, RestaurantRow::userId)
                .returns(NOT_TRIED_RESTAURANT_NAME, RestaurantRow::restaurantName)
                .returns(Set.of(FAST_FOOD, BURGER), RestaurantRow::categories)
                .returns(false, RestaurantRow::triedBefore)
                .returns(null, RestaurantRow::rating)
                .returns(null, RestaurantRow::review)
                .returns(List.of(), RestaurantRow::notes);
    }

    @Test
    @Order(30)
    void findTriedByUserIdAndName_afterAddingTwo() {
        assertRestaurantFound(TRIED_RESTAURANT_NAME)
                .returns(TEST_USER, RestaurantRow::userId)
                .returns(TRIED_RESTAURANT_NAME, RestaurantRow::restaurantName)
                .returns(Set.of(FAST_FOOD, SANDWICH), RestaurantRow::categories)
                .returns(true, RestaurantRow::triedBefore)
                .returns(6, RestaurantRow::rating)
                .returns("Good enough", RestaurantRow::review)
                .returns(List.of(
                        "Tuna should be ordered cold",
                        "Don't go too heavy on the veggies"
                ), RestaurantRow::notes);
    }

    @Test
    @Order(40)
    void deleteRestaurant() {
        var restaurantName = "for removal";
        var toSave = RestaurantRow.builder()
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
                .returns(TEST_USER, RestaurantRow::userId)
                .returns(newName, RestaurantRow::restaurantName)
                .returns(Set.of(FAST_FOOD, SANDWICH), RestaurantRow::categories)
                .returns(true, RestaurantRow::triedBefore)
                .returns(6, RestaurantRow::rating)
                .returns("Good enough", RestaurantRow::review)
                .returns(List.of(
                        "Tuna should be ordered cold",
                        "Don't go too heavy on the veggies"
                ), RestaurantRow::notes);
    }

    @Test
    @Order(50)
    void replaceCategories() {
        var newCategories = Set.of(VEGAN, FAST_FOOD, SANDWICH, CHICKEN);
        var restaurantName = TRIED_RESTAURANT_RENAMED;

        actOn(restaurantName,
              restaurant -> restaurantService.replaceCategories(restaurant, newCategories));

        assertRestaurantFound(restaurantName)
                .returns(newCategories, RestaurantRow::categories);
    }

    @Test
    @Order(50)
    void replaceNotes() {
        var newNotes = List.of("No hot tuna!", "4-5 veggies is enough");
        var restaurantName = TRIED_RESTAURANT_RENAMED;

        actOn(restaurantName,
              restaurant -> restaurantService.replaceNotes(restaurant, newNotes));

        assertRestaurantFound(restaurantName)
                .returns(newNotes, RestaurantRow::notes);
    }

    @Test
    @Order(50)
    void setRating() {
        var newRating = 7;
        var restaurantName = NOT_TRIED_RESTAURANT_NAME;

        actOn(restaurantName,
              restaurant -> restaurantService.setRating(restaurant, newRating));

        assertRestaurantFound(restaurantName)
                .returns(newRating, RestaurantRow::rating)
                .returns(true, RestaurantRow::triedBefore);
    }

    @Test
    @Order(60)
    void setNotVisited_afterSettingRating() {
        var restaurantName = NOT_TRIED_RESTAURANT_NAME;

        actOn(restaurantName,
              restaurant -> restaurantService.setTriedBefore(restaurant, false));

        assertRestaurantFound(restaurantName)
                .returns(false, RestaurantRow::triedBefore)
                .returns(null, RestaurantRow::rating);
    }

    @Test
    @Order(70)
    void setReview() {
        var newReview = "It's all right";
        var restaurantName = NOT_TRIED_RESTAURANT_NAME;

        actOn(restaurantName,
              restaurant -> restaurantService.setReview(restaurant, newReview));

        assertRestaurantFound(restaurantName)
                .returns(newReview, RestaurantRow::review)
                .returns(true, RestaurantRow::triedBefore);
    }

    @Test
    @Order(80)
    void setNotVisited_afterSettingReview() {
        var restaurantName = NOT_TRIED_RESTAURANT_NAME;

        actOn(restaurantName,
              restaurant -> restaurantService.setTriedBefore(restaurant, false));

        assertRestaurantFound(restaurantName)
                .returns(false, RestaurantRow::triedBefore)
                .returns(null, RestaurantRow::review);
    }

    @Test
    @Order(90)
    void setVisited() {
        var restaurantName = NOT_TRIED_RESTAURANT_NAME;

        actOn(restaurantName,
              restaurant -> restaurantService.setTriedBefore(restaurant, true));

        assertRestaurantFound(restaurantName)
                .returns(true, RestaurantRow::triedBefore)
                .returns(null, RestaurantRow::rating)
                .returns(null, RestaurantRow::review);
    }

    private void actOn(String restaurantName, Consumer<RestaurantRow> action) {
        if (!(restaurantService.fetch(restaurantName)
                instanceof FetchResult.Found found)) {
            throw new IllegalStateException("Not found restaurant with name " + restaurantName);
        }
        found.then(action);
    }

    private ObjectAssert<RestaurantRow> assertRestaurantFound(String restaurantName) {
        return assertThat(restaurantService.fetch(restaurantName))
                .as("Restaurant with name: " + restaurantName)
                .isInstanceOf(FetchResult.Found.class)
                .extracting("restaurant", type(RestaurantRow.class));
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