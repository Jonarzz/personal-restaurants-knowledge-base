package io.github.jonarzz.restaurant.knowledge;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.*;

import io.github.jonarzz.restaurant.knowledge.dynamodb.*;
import io.github.jonarzz.restaurant.knowledge.entry.*;

@EnableAutoConfiguration
@Import({
        DynamoDbConfig.class, RestaurantEntryManagementConfig.class
})
public class PersonalRestaurantsKnowledgeBaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(PersonalRestaurantsKnowledgeBaseApplication.class, args);
    }

}
