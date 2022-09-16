package io.github.jonarzz.restaurant.knowledge.dynamodb;

public interface ItemMapper<T extends DynamoDbTable<? extends DynamoDbKey>> {

    T createItem(ItemExtractor extractor);

    AttributesCreator attributesCreator(T item);

}
