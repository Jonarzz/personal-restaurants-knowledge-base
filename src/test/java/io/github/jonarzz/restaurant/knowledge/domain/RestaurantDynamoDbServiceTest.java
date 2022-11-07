package io.github.jonarzz.restaurant.knowledge.domain;

import static io.github.jonarzz.restaurant.knowledge.model.Category.*;
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
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.mock.mockito.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.*;
import org.springframework.test.context.*;
import org.testcontainers.containers.*;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.*;
import software.amazon.awssdk.services.dynamodb.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import io.github.jonarzz.restaurant.knowledge.model.*;
import io.github.jonarzz.restaurant.knowledge.technical.cache.*;
import io.github.jonarzz.restaurant.knowledge.technical.dynamodb.*;

@Testcontainers
@SpringBootTest(
        webEnvironment = NONE,
        classes = {
                DynamoDbConfig.class, RestaurantEntryManagementConfig.class, CacheConfig.class
        }
)
@TestPropertySource(properties = {
        "amazon.aws.accesskey = dummy-access",
        "amazon.aws.secretkey = dummy-secret"
})
@TestInstance(PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class RestaurantDynamoDbServiceTest {

    static final String TEST_USER = "test-user";
    static final String TRIED_RESTAURANT_NAME = "Subway";
    static final String TRIED_RESTAURANT_RENAMED = "Subway Nowy Świat";
    static final String NOT_TRIED_RESTAURANT_NAME = "Burger King";
    static final int FILLER_ENTRIES_COUNT = 50;

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
        setUpSecurityContext(TEST_USER);
    }

    @AfterAll
    static void afterAll() {
        dynamoDbContainer.close();
    }

    @Test
    @Order(10)
    void findByUserIdAndName_emptyTable() {
        assertThat(restaurantService.fetch(NOT_TRIED_RESTAURANT_NAME))
                .isEmpty();
        assertThat(restaurantService.fetch(TRIED_RESTAURANT_NAME))
                .isEmpty();
        assertThat(restaurantService.fetch(TRIED_RESTAURANT_RENAMED))
                .isEmpty();
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
                                   // item with rating and/or review will be marked as tried before automatically
                                   .triedBefore(false)
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
    void queryByNameStartingWithNotMatchingCase_triedRestaurant() {
        var criteria = RestaurantQueryCriteria.builder()
                                              .nameBeginsWith(TRIED_RESTAURANT_NAME.substring(0, 3)
                                                                                   .toUpperCase())
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
    void queryByNameStartingWithNotMatchingCase_notTriedRestaurant() {
        var criteria = RestaurantQueryCriteria.builder()
                                              .nameBeginsWith(NOT_TRIED_RESTAURANT_NAME.substring(0, 5)
                                                                                       .toUpperCase())
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
    @Order(30)
    void findNotTriedByUserIdAndName_afterAddingTwo() {
        var result = assertRestaurantFound(NOT_TRIED_RESTAURANT_NAME);

        assertNotTriedRestaurantInitial(result);
        // verify that cache was not used
        verify(repositorySpy)
                .findByKey(argThat(key -> NOT_TRIED_RESTAURANT_NAME.toLowerCase()
                                                                   .equals(key.nameLowercase())));
    }

    @Test
    @Order(30)
    void findTriedByUserIdAndName_afterAddingTwo() {
        var restaurant = assertRestaurantFound(TRIED_RESTAURANT_NAME);

        assertTriedRestaurantInitial(restaurant);
        // verify that cache was not used
        verify(repositorySpy)
                .findByKey(argThat(key -> TRIED_RESTAURANT_NAME.toLowerCase()
                                                               .equals(key.nameLowercase())));
    }

    @RepeatedTest(5)
    @Order(35)
    @SuppressWarnings("unchecked")
    void findNotTriedByUserIdAndName_cacheShouldBeUsed() {
        clearInvocations(repositorySpy);

        assertRestaurantFound(NOT_TRIED_RESTAURANT_NAME);

        verifyNoInteractions(repositorySpy);
    }

    @RepeatedTest(5)
    @Order(35)
    @SuppressWarnings("unchecked")
    void findTriedByUserIdAndName_cacheShouldBeUsed() {
        clearInvocations(repositorySpy);

        assertRestaurantFound(TRIED_RESTAURANT_NAME);

        verifyNoInteractions(repositorySpy);
    }

    @Test
    @Order(35)
    void tryToFindRestaurantWithDifferentUsed_cacheShouldBeOmitted() {
        var otherUserName = "other-user";
        setUpSecurityContext(otherUserName);

        var result = restaurantService.fetch(TRIED_RESTAURANT_NAME);

        assertThat(result)
                .isEmpty();
        verify(repositorySpy)
                .findByKey(argThat(key -> TRIED_RESTAURANT_NAME.toLowerCase()
                                                               .equals(key.nameLowercase())
                                          && otherUserName.equals(key.userId())));
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
                .isEmpty();
    }

    @Test
    @Order(40)
    void renameRestaurant() {
        var oldName = TRIED_RESTAURANT_NAME;
        var newName = TRIED_RESTAURANT_RENAMED;
        var updateData = new RestaurantData()
                .name(newName);

        actOn(oldName,
              restaurant -> restaurantService.update(restaurant, updateData));

        assertThat(restaurantService.fetch(oldName))
                .as("Restaurant fetched by old name")
                .isEmpty();
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
    @Order(50)
    void replaceCategories() {
        var newCategories = Set.of(VEGAN, FAST_FOOD, SANDWICH, CHICKEN);
        var restaurantName = TRIED_RESTAURANT_RENAMED;

        actOn(restaurantName,
              restaurant -> restaurantService.replaceCategories(restaurant, newCategories));

        assertRestaurantFound(restaurantName)
                .returns(newCategories, RestaurantItem::categories);
    }

    @Test
    @Order(50)
    void replaceNotes() {
        var newNotes = List.of("No hot tuna!", "4-5 veggies is enough");
        var restaurantName = TRIED_RESTAURANT_RENAMED;

        actOn(restaurantName,
              restaurant -> restaurantService.replaceNotes(restaurant, newNotes));

        assertRestaurantFound(restaurantName)
                .returns(newNotes, RestaurantItem::notes);
    }

    @Test
    @Order(50)
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

    @Test
    @Order(100)
    void updateRestaurant_newValues() {
        var categories = Set.of(PIZZA, PASTA, OTHER);
        var rating = 9;
        var review = "New review";
        var notes = List.of("First new note", "Second new note");
        var updateData = new RestaurantData()
                .categories(categories)
                .rating(rating)
                .review(review)
                .notes(notes);

        actOn(NOT_TRIED_RESTAURANT_NAME,
              restaurant -> restaurantService.update(restaurant, updateData));

        assertRestaurantFound(NOT_TRIED_RESTAURANT_NAME)
                .returns(TEST_USER, RestaurantItem::userId)
                .returns(categories, RestaurantItem::categories)
                .returns(true, RestaurantItem::triedBefore)
                .returns(rating, RestaurantItem::rating)
                .returns(review, RestaurantItem::review)
                .returns(notes, RestaurantItem::notes);
    }

    @Test
    @Order(100)
    void updateRestaurant_removeValues() {
        var categories = Set.of(BURGER, FAST_FOOD);
        var updateData = new RestaurantData()
                .categories(categories)
                .rating(null)
                .review(null)
                .notes(null);

        actOn(NOT_TRIED_RESTAURANT_NAME,
              restaurant -> restaurantService.update(restaurant, updateData));

        assertRestaurantFound(NOT_TRIED_RESTAURANT_NAME)
                .returns(TEST_USER, RestaurantItem::userId)
                .returns(categories, RestaurantItem::categories)
                .returns(true, RestaurantItem::triedBefore)
                .returns(null, RestaurantItem::rating)
                .returns(null, RestaurantItem::review)
                .returns(List.of(), RestaurantItem::notes);
    }

    private void actOn(String restaurantName, Consumer<RestaurantItem> action) {
        var restaurant = restaurantService.fetch(restaurantName)
                                          .orElseThrow(() -> new IllegalStateException("Not found restaurant with name "
                                                                                       + restaurantName));
        action.accept(restaurant);
    }

    private ObjectAssert<RestaurantItem> assertRestaurantFound(String restaurantName) {
        return assertThat(restaurantService.fetch(restaurantName))
                .as("Restaurant with name: " + restaurantName)
                .get(type(RestaurantItem.class));
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

    private static void setUpSecurityContext(String user) {
        SecurityContextHolder.getContext()
                             .setAuthentication(new TestingAuthenticationToken(user, null));
    }
}