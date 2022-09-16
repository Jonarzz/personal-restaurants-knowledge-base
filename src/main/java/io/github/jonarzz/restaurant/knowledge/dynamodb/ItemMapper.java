package io.github.jonarzz.restaurant.knowledge.dynamodb;

import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

public interface ItemMapper<T extends DynamoDbTable<? extends DynamoDbKey>> {

    T createItem(ItemExtractor extractor);

    Map<String, AttributeValue> createAttributes(T item);

}
