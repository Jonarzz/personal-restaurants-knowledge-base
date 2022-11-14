package io.github.jonarzz.restaurant.knowledge.domain;

import static io.github.jonarzz.restaurant.knowledge.domain.Category.*;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.InstanceOfAssertFactories.*;
import static org.junit.jupiter.api.TestInstance.Lifecycle.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.assertj.core.api.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.testcontainers.containers.*;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import io.github.jonarzz.restaurant.knowledge.technical.auth.*;
import io.github.jonarzz.restaurant.knowledge.technical.dynamodb.*;

@Testcontainers
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

    RestaurantRepository repositorySpy;
    RestaurantService restaurantService;

    @BeforeAll
    void beforeAll() {
        var amazonDynamoDb = DynamoDbTestUtil.createClient(
                "http://localhost:" + dynamoDbContainer.getFirstMappedPort()
        );
        repositorySpy = spy(new RestaurantRepository(amazonDynamoDb));
        restaurantService = new RestaurantDynamoDbService(repositorySpy);
        DynamoDbTestUtil.createTableFor(repositorySpy);
    }

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
        var toSave = new RestaurantData()
                .name(NOT_TRIED_RESTAURANT_NAME)
                .categories(Set.of(FAST_FOOD, BURGER));

        assertThatNoException()
                .isThrownBy(() -> restaurantService.create(toSave));
    }

    @Test
    @Order(20)
    void createSecondRestaurantEntry() {
        var toSave = new RestaurantData()
                .name(TRIED_RESTAURANT_NAME)
                .categories(Set.of(FAST_FOOD, SANDWICH))
                // item with rating and/or review will be marked as tried before automatically
                .triedBefore(false)
                .rating(6)
                .review("Good enough")
                .notes(List.of("Tuna should be ordered cold",
                               "Don't go too heavy on the veggies"));

        assertThatNoException()
                .isThrownBy(() -> restaurantService.create(toSave));
    }

    @ParameterizedTest
    @MethodSource("intRange")
    @Order(20)
    void createManyRestaurantEntries(int number) {
        var toSave = new RestaurantData()
                .name("one-of-many-" + number)
                .categories(Set.of(OTHER));

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
    void queryByTriedBefore_triedRestaurant_otherUser() {
        var criteria = RestaurantQueryCriteria.builder()
                                              .triedBefore(true)
                                              .build();
        setUpSecurityContext("some other user");

        var result = restaurantService.query(criteria);

        assertThat(result)
                .isEmpty();
    }

    @Test
    @Order(30)
    void queryByTriedBefore_notTriedRestaurant_otherUser() {
        var criteria = RestaurantQueryCriteria.builder()
                                              .triedBefore(false)
                                              .build();
        setUpSecurityContext("some other user");

        var result = restaurantService.query(criteria);

        assertThat(result)
                .isEmpty();
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
        var toSave = new RestaurantData().name(restaurantName)
                                         .categories(Set.of(PIZZA));
        restaurantService.create(toSave);

        actOnName(restaurantName, restaurantService::delete);

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

        actOnName(oldName,
                  name -> restaurantService.update(name, updateData));

        assertThat(restaurantService.fetch(oldName))
                .as("Restaurant fetched by old name")
                .isEmpty();
        assertRestaurantFound(newName)
                .returns(newName, RestaurantData::name)
                .returns(Set.of(FAST_FOOD, SANDWICH), RestaurantData::categories)
                .returns(true, RestaurantData::triedBefore)
                .returns(6, RestaurantData::rating)
                .returns("Good enough", RestaurantData::review)
                .returns(List.of(
                        "Tuna should be ordered cold",
                        "Don't go too heavy on the veggies"
                ), RestaurantData::notes);
    }

    @Test
    @Order(50)
    void replaceCategories() {
        var newCategories = Set.of(VEGAN, FAST_FOOD, SANDWICH, CHICKEN);
        var restaurantName = TRIED_RESTAURANT_RENAMED;

        actOnName(restaurantName,
                  name -> restaurantService.replaceCategories(name, newCategories));

        assertRestaurantFound(restaurantName)
                .returns(newCategories, RestaurantData::categories);
    }

    @Test
    @Order(50)
    void replaceNotes() {
        var newNotes = List.of("No hot tuna!", "4-5 veggies is enough");
        var restaurantName = TRIED_RESTAURANT_RENAMED;

        actOnName(restaurantName,
                  name -> restaurantService.replaceNotes(name, newNotes));

        assertRestaurantFound(restaurantName)
                .returns(newNotes, RestaurantData::notes);
    }

    @Test
    @Order(50)
    void setRating() {
        var newRating = 7;
        var restaurantName = NOT_TRIED_RESTAURANT_NAME;

        actOnName(restaurantName,
                  name -> restaurantService.setRating(name, newRating));

        assertRestaurantFound(restaurantName)
                .returns(newRating, RestaurantData::rating)
                .returns(true, RestaurantData::triedBefore);
    }

    @Test
    @Order(60)
    void setNotVisited_afterSettingRating() {
        var restaurantName = NOT_TRIED_RESTAURANT_NAME;

        actOnName(restaurantName,
                  name -> restaurantService.setTriedBefore(name, false));

        assertRestaurantFound(restaurantName)
                .returns(false, RestaurantData::triedBefore)
                .returns(null, RestaurantData::rating);
    }

    @Test
    @Order(70)
    void setReview() {
        var newReview = "It's all right";
        var restaurantName = NOT_TRIED_RESTAURANT_NAME;

        actOnName(restaurantName,
                  name -> restaurantService.setReview(name, newReview));

        assertRestaurantFound(restaurantName)
                .returns(newReview, RestaurantData::review)
                .returns(true, RestaurantData::triedBefore);
    }

    @Test
    @Order(80)
    void setNotVisited_afterSettingReview() {
        var restaurantName = NOT_TRIED_RESTAURANT_NAME;

        actOnName(restaurantName,
                  name -> restaurantService.setTriedBefore(name, false));

        assertRestaurantFound(restaurantName)
                .returns(false, RestaurantData::triedBefore)
                .returns(null, RestaurantData::review);
    }

    @Test
    @Order(90)
    void setVisited() {
        var restaurantName = NOT_TRIED_RESTAURANT_NAME;

        actOnName(restaurantName,
                  name -> restaurantService.setTriedBefore(name, true));

        assertRestaurantFound(restaurantName)
                .returns(true, RestaurantData::triedBefore)
                .returns(null, RestaurantData::rating)
                .returns(null, RestaurantData::review);
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

        actOnName(NOT_TRIED_RESTAURANT_NAME,
                  name -> restaurantService.update(name, updateData));

        assertRestaurantFound(NOT_TRIED_RESTAURANT_NAME)
                .returns(categories, RestaurantData::categories)
                .returns(true, RestaurantData::triedBefore)
                .returns(null, RestaurantData::rating)
                .returns(null, RestaurantData::review)
                .returns(List.of(), RestaurantData::notes);
    }

    @Test
    @Order(101)
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

        actOnName(NOT_TRIED_RESTAURANT_NAME,
                  name -> restaurantService.update(name, updateData));

        assertRestaurantFound(NOT_TRIED_RESTAURANT_NAME)
                .returns(categories, RestaurantData::categories)
                .returns(true, RestaurantData::triedBefore)
                .returns(rating, RestaurantData::rating)
                .returns(review, RestaurantData::review)
                .returns(notes, RestaurantData::notes);
    }

    @Test
    @Order(102)
    void updateRestaurant_markAsNotTriedBefore() {
        var updateData = new RestaurantData()
                .triedBefore(false);

        actOnName(NOT_TRIED_RESTAURANT_NAME,
                  name -> restaurantService.update(name, updateData));

        assertRestaurantFound(NOT_TRIED_RESTAURANT_NAME)
                .returns(Set.of(PIZZA, PASTA, OTHER), RestaurantData::categories)
                .returns(false, RestaurantData::triedBefore)
                .returns(null, RestaurantData::rating)
                .returns(null, RestaurantData::review)
                // no notes on request => cleared
                .returns(List.of(), RestaurantData::notes);
    }

    @Test
    @Order(103)
    void updateRestaurant_markAsTriedBeforeWithReviewAndRating() {
        var rating = 7;
        var review = "Another test review";
        var updateData = new RestaurantData()
                .triedBefore(true)
                .rating(rating)
                .review(review);

        actOnName(NOT_TRIED_RESTAURANT_NAME,
                  name -> restaurantService.update(name, updateData));

        assertRestaurantFound(NOT_TRIED_RESTAURANT_NAME)
                .returns(true, RestaurantData::triedBefore)
                .returns(rating, RestaurantData::rating)
                .returns(review, RestaurantData::review);
    }

    @Test
    @Order(104)
    void updateRestaurant_changeRatingAndReviewForTriedBefore() {
        var rating = 3;
        var review = "Changed review text";
        var updateData = new RestaurantData()
                .rating(rating)
                .review(review);

        actOnName(NOT_TRIED_RESTAURANT_NAME,
                  name -> restaurantService.update(name, updateData));

        assertRestaurantFound(NOT_TRIED_RESTAURANT_NAME)
                .returns(true, RestaurantData::triedBefore)
                .returns(rating, RestaurantData::rating)
                .returns(review, RestaurantData::review);
    }

    private void actOn(String restaurantName, Consumer<RestaurantData> action) {
        var restaurant = restaurantService.fetch(restaurantName)
                                          .orElseThrow(() -> new IllegalStateException("Not found restaurant with name "
                                                                                       + restaurantName));
        action.accept(restaurant);
    }

    private void actOnName(String restaurantName, Consumer<String> action) {
        actOn(restaurantName, restaurant -> action.accept(restaurant.name()));
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
                  .returns(6, RestaurantData::rating)
                  .returns("Good enough", RestaurantData::review)
                  .returns(List.of(
                          "Tuna should be ordered cold",
                          "Don't go too heavy on the veggies"
                  ), RestaurantData::notes);
    }

    private static void setUpSecurityContext(String user) {
        SecurityContext.setUserId(user);
    }
}