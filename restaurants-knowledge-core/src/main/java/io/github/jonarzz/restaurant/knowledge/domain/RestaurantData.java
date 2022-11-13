package io.github.jonarzz.restaurant.knowledge.domain;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.*;

import java.util.*;

@Valid
@Setter
@Getter
@Accessors(fluent = true)
@ToString
public class RestaurantData {

    @JsonProperty("name")
    private String name;
    @JsonProperty("categories")
    private Set<Category> categories;
    @JsonProperty("triedBefore")
    private Boolean triedBefore;
    @Min(1)
    @Max(10)
    @JsonProperty("rating")
    private Integer rating;
    @JsonProperty("review")
    private String review;
    @JsonProperty("notes")
    private List<String> notes;

}
