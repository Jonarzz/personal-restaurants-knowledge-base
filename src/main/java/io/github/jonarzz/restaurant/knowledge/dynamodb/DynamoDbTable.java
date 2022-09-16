package io.github.jonarzz.restaurant.knowledge.dynamodb;

public interface DynamoDbTable<K extends DynamoDbKey> {

    K getKey();

}
