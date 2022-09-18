package io.github.jonarzz.restaurant.knowledge.technical.dynamodb;

import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

public interface DynamoDbKey {

    Map<String, AttributeValue> asAttributes();

}
