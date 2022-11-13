package io.github.jonarzz.restaurant.knowledge.technical.dynamodb;

public interface DynamoDbTable<K extends DynamoDbKey> {

    K getKey();

}
