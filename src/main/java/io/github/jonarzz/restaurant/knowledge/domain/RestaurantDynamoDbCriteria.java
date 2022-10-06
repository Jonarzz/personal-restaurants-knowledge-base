package io.github.jonarzz.restaurant.knowledge.domain;

import static io.github.jonarzz.restaurant.knowledge.domain.RestaurantItem.Attributes.*;
import static io.github.jonarzz.restaurant.knowledge.domain.RestaurantKey.*;
import static io.github.jonarzz.restaurant.knowledge.technical.dynamodb.AttributesCreator.*;
import static java.lang.Boolean.*;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.*;
import static software.amazon.awssdk.services.dynamodb.model.ComparisonOperator.*;

import lombok.*;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

import io.github.jonarzz.restaurant.knowledge.technical.dynamodb.*;

@AllArgsConstructor
class RestaurantDynamoDbCriteria implements DynamoDbQueryCriteria {

    private RestaurantQueryCriteria criteria;

    @Override
    public Map<String, Condition> keyConditions() {
        Map<String, Condition> conditions = new HashMap<>();
        conditions.put(USER_ID, Condition.builder()
                                         .comparisonOperator(EQ)
                                         .attributeValueList(fromS(contextUserId()))
                                         .build());
        var nameBeginning = criteria.nameBeginsWith();
        if (nameBeginning != null) {
            conditions.put(NAME_LOWERCASE, Condition.builder()
                                                    .comparisonOperator(BEGINS_WITH)
                                                    .attributeValueList(fromS(nameBeginning.toLowerCase()))
                                                    .build());
        }
        return conditions;
    }

    @Override
    public Map<String, Condition> queryConditions() {
        Map<String, Condition> conditions = new HashMap<>();
        if (criteria.category() != null) {
            conditions.put(CATEGORIES, Condition.builder()
                                                .comparisonOperator(CONTAINS)
                                                .attributeValueList(fromS(criteria.category()
                                                                                  .getValue()))
                                                .build());
        }
        if (criteria.triedBefore() != null) {
            conditions.put(TRIED_BEFORE, Condition.builder()
                                                  .comparisonOperator(EQ)
                                                  .attributeValueList(fromBool(criteria.triedBefore()))
                                                  .build());
        }
        // rating only set for restaurants tried before
        if (criteria.ratingAtLeast() != null && !FALSE.equals(criteria.triedBefore())) {
            conditions.put(RATING, Condition.builder()
                                            .comparisonOperator(GE)
                                            .attributeValueList(numberAttribute(criteria.ratingAtLeast()))
                                            .build());
        }
        return conditions;
    }

    @Override
    public boolean isEmpty() {
        return criteria.isEmpty();
    }
}
