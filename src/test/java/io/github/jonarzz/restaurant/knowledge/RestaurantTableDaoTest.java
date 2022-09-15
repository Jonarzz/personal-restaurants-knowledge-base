package io.github.jonarzz.restaurant.knowledge;

import static io.github.jonarzz.restaurant.knowledge.model.Category.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.TestInstance.Lifecycle.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.*;
import static software.amazon.awssdk.regions.Region.*;
import static software.amazon.awssdk.services.dynamodb.model.KeyType.*;
import static software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.test.context.*;
import org.testcontainers.containers.*;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.*;
import software.amazon.awssdk.services.dynamodb.*;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.*;
import java.util.*;

@Testcontainers
@SpringBootTest(
        webEnvironment = NONE,
        classes = RestaurantTableDaoTest.Configuration.class
)
@TestPropertySource(properties = {
        "amazon.aws.accesskey = dummy-access",
        "amazon.aws.secretkey = dummy-secret"
})
@TestInstance(PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class RestaurantTableDaoTest {

    static final String TEST_USER = "test-user";
    static final String TRIED_RESTAURANT_NAME = "Subway";
    static final String NOT_TRIED_RESTAURANT_NAME = "Burger King";

    @Container
    static final GenericContainer<?> dynamoDbContainer = new GenericContainer<>("amazon/dynamodb-local:latest")
            .withCommand("-jar DynamoDBLocal.jar -inMemory -sharedDb")
            .withExposedPorts(8000);

    @TestConfiguration
    static class Configuration {

        @Bean
        @Primary
        DynamoDbClient amazonDynamoDB() {
            dynamoDbContainer.start();
            return DynamoDbClient.builder()
                                 .endpointOverride(URI.create("http://localhost:" + dynamoDbContainer.getFirstMappedPort()))
                                 .region(EU_CENTRAL_1)
                                 .build();
        }
    }

    @Autowired
    DynamoDbClient amazonDynamoDb;
    @Autowired
    RestaurantTableDao restaurantTableDao;

    @BeforeAll
    void beforeAll() {
        amazonDynamoDb.createTable(prepareCreateTableRequest());
    }

    @Test
    @Order(10)
    void findByUserIdAndName_emptyTable() {
        var result = restaurantTableDao.findByUserIdAndRestaurantName(TEST_USER, NOT_TRIED_RESTAURANT_NAME);

        assertThat(result)
                .isEmpty();
    }

    @Test
    @Order(20)
    void createFirstRestaurantEntry() {
        var toSave = RestaurantTable.builder()
                                    .userId(TEST_USER)
                                    .restaurantName(NOT_TRIED_RESTAURANT_NAME)
                                    .category(FAST_FOOD)
                                    .category(BURGER)
                                    .build();

        assertThatNoException()
                .isThrownBy(() -> restaurantTableDao.save(toSave));
    }

    @Test
    @Order(20)
    void createSecondRestaurantEntry() {
        var toSave = RestaurantTable.builder()
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
                .isThrownBy(() -> restaurantTableDao.save(toSave));
    }

    @Test
    @Order(30)
    void findNotTriedByUserIdAndName_afterAddingTwo() {
        var result = restaurantTableDao.findByUserIdAndRestaurantName(TEST_USER, NOT_TRIED_RESTAURANT_NAME);

        assertThat(result)
                .get()
                .returns(TEST_USER, RestaurantTable::userId)
                .returns(NOT_TRIED_RESTAURANT_NAME, RestaurantTable::restaurantName)
                .returns(Set.of(FAST_FOOD, BURGER), RestaurantTable::categories)
                .returns(false, RestaurantTable::triedBefore)
                .returns(null, RestaurantTable::rating)
                .returns(null, RestaurantTable::review)
                .returns(List.of(), RestaurantTable::notes);
    }

    @Test
    @Order(30)
    void findTriedByUserIdAndName_afterAddingTwo() {
        var result = restaurantTableDao.findByUserIdAndRestaurantName(TEST_USER, TRIED_RESTAURANT_NAME);

        assertThat(result)
                .get()
                .returns(TEST_USER, RestaurantTable::userId)
                .returns(TRIED_RESTAURANT_NAME, RestaurantTable::restaurantName)
                .returns(Set.of(FAST_FOOD, SANDWICH), RestaurantTable::categories)
                .returns(true, RestaurantTable::triedBefore)
                .returns(6, RestaurantTable::rating)
                .returns("Good enough", RestaurantTable::review)
                .returns(List.of(
                        "Tuna should be ordered cold",
                        "Don't go too heavy on the veggies"
                ), RestaurantTable::notes);
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