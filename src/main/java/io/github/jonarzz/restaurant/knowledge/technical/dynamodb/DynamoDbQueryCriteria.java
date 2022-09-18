package io.github.jonarzz.restaurant.knowledge.technical.dynamodb;

import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

public interface DynamoDbQueryCriteria {

    Map<String, Condition> keyConditions();

    Map<String, Condition> queryConditions();

    boolean isEmpty();
}
