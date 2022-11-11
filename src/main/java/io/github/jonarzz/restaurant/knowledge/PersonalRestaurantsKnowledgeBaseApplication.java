package io.github.jonarzz.restaurant.knowledge;

import static org.springframework.http.HttpMethod.*;

import lombok.extern.slf4j.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.web.servlet.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.*;
import org.springframework.security.config.annotation.web.configuration.*;
import org.springframework.security.core.context.*;
import org.springframework.web.cors.*;

import io.github.jonarzz.restaurant.knowledge.technical.auth.SecurityContext;
import io.github.jonarzz.restaurant.knowledge.technical.dynamodb.*;

@Configuration
@EnableWebSecurity
@EnableAutoConfiguration
@Import({
        DynamoDbClientFactory.class, RestaurantDomainConfig.class, CacheConfig.class
})
@Slf4j
public class PersonalRestaurantsKnowledgeBaseApplication extends WebSecurityConfigurerAdapter {

    public static void main(String[] args) {
        SpringApplication.run(PersonalRestaurantsKnowledgeBaseApplication.class, args);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // TODO secure the app
        http.authorizeRequests()
            .anyRequest().permitAll()
            .and()
            .cors().configurationSource(request -> {
                var corsConfig = new CorsConfiguration().applyPermitDefaultValues();
                corsConfig.addAllowedMethod(PUT);
                return corsConfig;
            })
            .and()
            .csrf().ignoringAntMatchers("/**");
    }

    @Bean
    FilterRegistrationBean<?> securityContextEnrichingFilter() {
        var filterRegistrationBean = new FilterRegistrationBean<>();
        filterRegistrationBean.setFilter((request, response, chain) ->
                                                 SecurityContext.setUserId(SecurityContextHolder.getContext()
                                                                                                .getAuthentication()
                                                                                                .getName()));
        filterRegistrationBean.setOrder(Integer.MAX_VALUE);
        return filterRegistrationBean;
    }

}
