package io.github.jonarzz.restaurant.knowledge;

import lombok.extern.slf4j.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.*;

import io.github.jonarzz.restaurant.knowledge.technical.dynamodb.*;

@Configuration
@EnableAutoConfiguration
@Import({
        DynamoDbClientFactory.class, ApiConfig.class,
        CacheConfig.class, SecurityConfig.class
})
@Slf4j
public class PersonalRestaurantsKnowledgeBaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(PersonalRestaurantsKnowledgeBaseApplication.class, args);
    }

}
