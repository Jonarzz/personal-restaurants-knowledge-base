package io.github.jonarzz.restaurant.knowledge.common;

import static io.github.jonarzz.restaurant.knowledge.common.ModificationResultType.*;
import static lombok.AccessLevel.*;

import lombok.*;
import lombok.experimental.*;

@RequiredArgsConstructor(access = PRIVATE)
@Accessors(fluent = true)
public class ModificationResult<T> {

    @Getter
    private final ModificationResultType resultType;
    private final T content;

    public static <T> ModificationResult<T> success(T content) {
        return new ModificationResult<>(SUCCESS, content);
    }

    public static ModificationResult noChanges() {
        return new ModificationResult<>(NO_CHANGES, null);
    }

    public static ModificationResult alreadyExists() {
        return new ModificationResult<>(ALREADY_EXISTS, null);
    }

    public static ModificationResult notFound() {
        return new ModificationResult<>(NOT_FOUND, null);
    }

    public T content() {
        return content;
    }

}
