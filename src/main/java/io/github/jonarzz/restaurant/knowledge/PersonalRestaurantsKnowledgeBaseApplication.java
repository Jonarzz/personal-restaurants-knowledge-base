package io.github.jonarzz.restaurant.knowledge;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;

@SpringBootApplication(scanBasePackageClasses = DynamoDbConfig.class)
public class PersonalRestaurantsKnowledgeBaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(PersonalRestaurantsKnowledgeBaseApplication.class, args);
    }

}
