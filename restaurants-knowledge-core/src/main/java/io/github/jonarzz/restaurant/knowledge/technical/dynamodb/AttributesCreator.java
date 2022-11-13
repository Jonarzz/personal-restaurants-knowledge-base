package io.github.jonarzz.restaurant.knowledge.technical.dynamodb;

import static java.util.stream.Collectors.*;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.*;

import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;
import java.util.function.*;

public class AttributesCreator {

    private Map<String, AttributeValue> attributes = new HashMap<>();

    public static AttributeValue numberAttribute(Integer value) {
        return Optional.ofNullable(value)
                       .map(String::valueOf)
                       .map(AttributeValue::fromN)
                       .orElse(null);
    }

    public static AttributeValue listAttribute(List<String> values) {
        return fromL(values.stream()
                           .map(AttributeValue::fromS)
                           .collect(toList()));
    }

    public static <S> AttributeValue setAttribute(Set<S> values, Function<S, String> mapper) {
        return AttributeValue.builder()
                             .ss(values.stream()
                                       .map(mapper)
                                       .collect(toSet()))
                             .build();
    }

    public static AttributeValueUpdate asUpdateAttribute(AttributeValue attributeValue) {
        return AttributeValueUpdate.builder()
                                   .value(attributeValue)
                                   .build();
    }

    public static AttributeValueUpdate asNumberUpdateAttribute(Integer value) {
        return AttributeValueUpdate.builder()
                                   .value(numberAttribute(value))
                                   .build();
    }

    public AttributesCreator put(String attributeName, AttributeValue attributeValue) {
        attributes.put(attributeName, attributeValue);
        return this;
    }

    public <T> AttributesCreator putIfPresent(String attributeName, T nullable,
                                              Function<T, AttributeValue> attributeCreator) {
        Optional.ofNullable(nullable)
                .map(attributeCreator)
                .ifPresent(attribute -> attributes.put(attributeName, attribute));
        return this;
    }

    public AttributesCreator putIfNotEmpty(String attributeName, List<String> values) {
        if (values != null && !values.isEmpty()) {
            attributes.put(attributeName, listAttribute(values));
        }
        return this;
    }

    public AttributesCreator putOrEmpty(String attributeName, List<String> values) {
        values = Optional.ofNullable(values)
                         .orElse(List.of());
        attributes.put(attributeName, listAttribute(values));
        return this;
    }

    public <S> AttributesCreator putIfNotEmpty(String attributeName, Set<S> values, Function<S, String> mapper) {
        if (values != null && !values.isEmpty()) {
            attributes.put(attributeName, setAttribute(values, mapper));
        }
        return this;
    }

    Map<String, AttributeValue> toAttributes() {
        return attributes;
    }

    Map<String, AttributeValueUpdate> toUpdateAttributes() {
        return attributes.entrySet()
                         .stream()
                         .collect(toMap(Map.Entry::getKey,
                                        entry -> AttributeValueUpdate.builder()
                                                                     .value(entry.getValue())
                                                                     .build()));
    }

}
