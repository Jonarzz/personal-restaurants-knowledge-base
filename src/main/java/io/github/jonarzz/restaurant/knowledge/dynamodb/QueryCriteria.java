package io.github.jonarzz.restaurant.knowledge.dynamodb;

import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

public interface QueryCriteria {

    Map<String, Condition> keyConditions();

    Map<String, Condition> queryConditions();

    boolean isEmpty();
}
