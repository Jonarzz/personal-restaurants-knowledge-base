package io.github.jonarzz.restaurant.knowledge.technical.dynamodb;

import static java.util.function.Function.*;
import static java.util.stream.Collectors.*;

import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;
import java.util.function.*;

public class ItemExtractor {

    private final Map<String, AttributeValue> item;

    public ItemExtractor(Map<String, AttributeValue> item) {
        this.item = item;
    }

    public String string(String attributeName) {
        return get(attributeName, AttributeValue::s);
    }

    public Integer integer(String attributeName) {
        return get(attributeName, AttributeValue::n, Integer::valueOf);
    }

    public boolean bool(String attributeName) {
        return getOrDefault(attributeName, AttributeValue::bool, false);
    }

    public <M> Set<M> set(String attributeName, Function<String, M> mapper) {
        return Optional.ofNullable(item.get(attributeName))
                       .map(AttributeValue::ss)
                       .stream()
                       .flatMap(Collection::stream)
                       .map(mapper)
                       .collect(toSet());
    }

    public List<String> list(String attributeName) {
        return Optional.ofNullable(item.get(attributeName))
                       .map(AttributeValue::l)
                       .stream()
                       .flatMap(Collection::stream)
                       .map(AttributeValue::s)
                       .collect(toList());
    }

    private <A> A get(String attributeName, Function<AttributeValue, A> extractor) {
        return getOrDefault(attributeName, extractor, (A) null);
    }

    private <A, M> M get(String attributeName, Function<AttributeValue, A> extractor, Function<A, M> mapper) {
        return getOrDefault(attributeName, extractor, mapper, null);
    }

    private <A> A getOrDefault(String attributeName, Function<AttributeValue, A> extractor, A defaultValue) {
        return getOrDefault(attributeName, extractor, identity(), defaultValue);
    }

    private <A, M> M getOrDefault(String attributeName, Function<AttributeValue, A> extractor,
                                  Function<A, M> mapper, M defaultValue) {
        return Optional.ofNullable(item.get(attributeName))
                       .map(extractor)
                       .map(mapper)
                       .orElse(defaultValue);
    }
}
