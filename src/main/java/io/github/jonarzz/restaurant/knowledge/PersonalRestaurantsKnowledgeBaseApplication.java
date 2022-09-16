package io.github.jonarzz.restaurant.knowledge;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.*;
import org.springframework.security.config.annotation.web.configuration.*;

import io.github.jonarzz.restaurant.knowledge.dynamodb.*;
import io.github.jonarzz.restaurant.knowledge.entry.*;

@Configuration
@EnableWebSecurity
@EnableAutoConfiguration
@Import({
        DynamoDbConfig.class, RestaurantEntryManagementConfig.class
})
public class PersonalRestaurantsKnowledgeBaseApplication extends WebSecurityConfigurerAdapter {

    public static void main(String[] args) {
        SpringApplication.run(PersonalRestaurantsKnowledgeBaseApplication.class, args);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // TODO secure the app
        http.authorizeRequests()
            .anyRequest().permitAll();
    }

}
