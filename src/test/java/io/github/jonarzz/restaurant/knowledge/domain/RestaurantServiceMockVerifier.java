package io.github.jonarzz.restaurant.knowledge.domain;

import static io.github.jonarzz.restaurant.knowledge.domain.MockVerificationForContract.*;
import static io.github.jonarzz.restaurant.knowledge.domain.RestaurantServiceMock.*;
import static io.github.jonarzz.restaurant.knowledge.model.Category.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;

class RestaurantServiceMockVerifier {

    private RestaurantServiceMockVerifier() {
    }

    // TODO other verifications

    static void verifyCategoryInteractions() {
        verify(MOCK_INSTANCE,
               asPartOfContract("add-category")
                       .shouldBeCalled(once()))
                .replaceCategories(KFC_CITY_CENTRE, Set.of(FAST_FOOD, SANDWICH, CHICKEN));
        verify(MOCK_INSTANCE,
               asPartOfContract("try-to-add-category-for-non-existent")
                       .shouldBeCalled(never()))
                .replaceCategories(any(), eq(Set.of(FAST_FOOD, CHICKEN)));
        verify(MOCK_INSTANCE,
               asPartOfContract("replace-categories")
                       .shouldBeCalled(once()))
                .replaceCategories(KFC_CITY_CENTRE, Set.of(FAST_FOOD, SANDWICH, CHICKEN, BURGER, OTHER));
        verify(MOCK_INSTANCE,
               asPartOfContract("try-to-replace-categories-for-non-existent")
                       .shouldBeCalled(never()))
                .replaceCategories(any(), eq(Set.of(FAST_FOOD, BURGER, CHICKEN)));
        verify(MOCK_INSTANCE,
               asPartOfContract("replace-categories")
                       .shouldBeCalled(once()))
                .replaceCategories(KFC_CITY_CENTRE, Set.of(FAST_FOOD, SANDWICH, CHICKEN, BURGER, OTHER));
        verify(MOCK_INSTANCE,
               asPartOfContract("try-to-replace-categories-for-non-existent")
                       .shouldBeCalled(never()))
                .replaceCategories(any(), eq(Set.of(FAST_FOOD, BURGER, CHICKEN)));
    }

}
