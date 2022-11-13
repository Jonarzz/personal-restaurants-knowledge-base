package io.github.jonarzz.restaurant.knowledge.technical.dynamodb;

import static java.util.stream.Collectors.*;

import lombok.extern.slf4j.*;
import software.amazon.awssdk.services.dynamodb.*;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.annotation.*;
import java.util.*;

@Slf4j
public abstract class DynamoDbRepository<T extends DynamoDbTable<K>, K extends DynamoDbKey> {

    private String tableName;
    private ItemMapper<T> itemMapper;
    private DynamoDbClient client;

    protected DynamoDbRepository(String tableName, ItemMapper<T> itemMapper, DynamoDbClient client) {
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

    public List<T> query(DynamoDbQueryCriteria criteria) {
        if (criteria.isEmpty()) {
            throw new IllegalArgumentException("Query criteria cannot be empty");
        }
        var request = QueryRequest.builder()
                                  .tableName(tableName)
                                  .keyConditions(criteria.keyConditions())
                                  .queryFilter(criteria.queryConditions())
                                  .build();
        return client.query(request)
                     .items()
                     .stream()
                     .map(item -> {
                         var extractor = new ItemExtractor(item);
                         return itemMapper.createItem(extractor);
                     })
                     .collect(toList());
    }

    public void create(T item) {
        var request = PutItemRequest.builder()
                                    .tableName(tableName)
                                    .item(itemMapper.attributesCreator(item)
                                                    .toAttributes())
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

    public void update(T item, AttributesCreator attributesCreator) {
        update(item, attributesCreator.toUpdateAttributes());
    }

    public void delete(T item) {
        var request = DeleteItemRequest.builder()
                                       .tableName(tableName)
                                       .key(item.getKey()
                                                .asAttributes())
                                       .build();
        client.deleteItem(request);
    }

    protected abstract CreateTableRequest prepareCreateTableRequest();

    @PostConstruct
    void createTable() {
        try {
            client.createTable(prepareCreateTableRequest());
        } catch (ResourceInUseException exception) {
            log.info("Tried to create table {}, but it already exists", tableName);
        }
    }
}
