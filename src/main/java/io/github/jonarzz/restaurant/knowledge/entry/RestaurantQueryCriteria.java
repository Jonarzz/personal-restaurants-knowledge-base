package io.github.jonarzz.restaurant.knowledge.entry;

import static io.github.jonarzz.restaurant.knowledge.dynamodb.AttributesCreator.*;
import static io.github.jonarzz.restaurant.knowledge.entry.RestaurantItem.Fields.*;
import static io.github.jonarzz.restaurant.knowledge.entry.RestaurantKey.*;
import static java.lang.Boolean.*;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.*;
import static software.amazon.awssdk.services.dynamodb.model.ComparisonOperator.*;

import lombok.Builder;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

import io.github.jonarzz.restaurant.knowledge.dynamodb.*;
import io.github.jonarzz.restaurant.knowledge.model.*;

@Builder
class RestaurantQueryCriteria implements QueryCriteria {

    private String nameBeginsWith;
    private Category category;
    private Boolean triedBefore;
    private Integer ratingAtLeast;

    @Override
    public Map<String, Condition> keyConditions() {
        Map<String, Condition> conditions = new HashMap<>();
        conditions.put(USER_ID, Condition.builder()
                                         .comparisonOperator(EQ)
                                         .attributeValueList(fromS(contextUserId()))
                                         .build());
        if (nameBeginsWith != null) {
            conditions.put(RESTAURANT_NAME, Condition.builder()
                                                     .comparisonOperator(BEGINS_WITH)
                                                     .attributeValueList(fromS(nameBeginsWith))
                                                     .build());
        }
        return conditions;
    }

    @Override
    public Map<String, Condition> queryConditions() {
        Map<String, Condition> conditions = new HashMap<>();
        if (category != null) {
            conditions.put(CATEGORIES, Condition.builder()
                                                .comparisonOperator(CONTAINS)
                                                .attributeValueList(fromS(category.getValue()))
                                                .build());
        }
        if (triedBefore != null) {
            conditions.put(TRIED_BEFORE, Condition.builder()
                                                  .comparisonOperator(EQ)
                                                  .attributeValueList(fromBool(triedBefore))
                                                  .build());
        }
        // rating only set for restaurants tried before
        if (ratingAtLeast != null && !FALSE.equals(triedBefore)) {
            conditions.put(RATING, Condition.builder()
                                            .comparisonOperator(GE)
                                            .attributeValueList(numberAttribute(ratingAtLeast))
                                            .build());
        }
        return conditions;
    }

    @Override
    public boolean isEmpty() {
        return nameBeginsWith == null
               && category == null
               && triedBefore == null
               && ratingAtLeast == null;
    }
}
