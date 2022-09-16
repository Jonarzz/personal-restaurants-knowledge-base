package io.github.jonarzz.restaurant.knowledge.dynamodb;

import software.amazon.awssdk.services.dynamodb.*;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

public class DynamoDbRepository<T extends DynamoDbTable<K>, K extends DynamoDbKey> {

    private String tableName;
    private ItemMapper<T> itemMapper;
    private DynamoDbClient client;

    public DynamoDbRepository(String tableName, ItemMapper<T> itemMapper, DynamoDbClient client) {
        this.tableName = tableName;
        this.itemMapper = itemMapper;
        this.client = client;
    }

    public Optional<T> findByKey(K key) {
        var request = GetItemRequest.builder()
                                    .tableName(tableName)
                                    .key(key.asAttributes())
                                    .build();
        var response = client.getItem(request);
        if (!response.hasItem()) {
            return Optional.empty();
        }
        var extractor = new ItemExtractor(response.item());
        return Optional.of(itemMapper.createItem(extractor));
    }

    public void create(T item) {
        var request = PutItemRequest.builder()
                                    .tableName(tableName)
                                    .item(itemMapper.createAttributes(item))
                                    .build();
        client.putItem(request);
    }

    public void update(T item, Map<String, AttributeValueUpdate> updates) {
        var request = UpdateItemRequest.builder()
                                       .tableName(tableName)
                                       .key(item.getKey()
                                                .asAttributes())
                                       .attributeUpdates(updates)
                                       .build();
        client.updateItem(request);
    }

    public void delete(T item) {
        var request = DeleteItemRequest.builder()
                                       .tableName(tableName)
                                       .key(item.getKey()
                                                .asAttributes())
                                       .build();
        client.deleteItem(request);
    }

}