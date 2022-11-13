package io.github.jonarzz.restaurant.knowledge.domain;

public enum Category {

    ASIAN("ASIAN"),
    BEER("BEER"),
    BURGER("BURGER"),
    CHICKEN("CHICKEN"),
    FAST_FOOD("FAST_FOOD"),
    INDIAN("INDIAN"),
    KEBAB("KEBAB"),
    LUNCH("LUNCH"),
    OTHER("OTHER"),
    PASTA("PASTA"),
    PIZZA("PIZZA"),
    RAMEN("RAMEN"),
    SANDWICH("SANDWICH"),
    SUSHI("SUSHI"),
    VEGAN("VEGAN");

    private final String value;

    Category(String value) {
        this.value = value;
    }

    String getValue() {
        return value;
    }
}
