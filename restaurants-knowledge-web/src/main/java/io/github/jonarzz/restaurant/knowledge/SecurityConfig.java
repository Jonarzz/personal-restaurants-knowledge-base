package io.github.jonarzz.restaurant.knowledge;

import static org.springframework.http.HttpMethod.*;

import org.springframework.boot.web.servlet.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.*;
import org.springframework.security.config.annotation.web.configuration.*;
import org.springframework.security.core.context.*;
import org.springframework.security.web.*;
import org.springframework.web.cors.*;

import io.github.jonarzz.restaurant.knowledge.technical.auth.SecurityContext;

@EnableWebSecurity
class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeRequests()
                   .anyRequest().permitAll()
                   .and()
                   .cors().configurationSource(getCorsConfigurationSource())
                   .and()
                   .csrf().ignoringAntMatchers("/**")
                   .and()
                   .build();
    }

    @Bean
    CorsConfigurationSource getCorsConfigurationSource() {
        return request -> {
            var corsConfig = new CorsConfiguration()
                    .applyPermitDefaultValues();
            corsConfig.addAllowedMethod(PUT);
            return corsConfig;
        };
    }

    @Bean
    FilterRegistrationBean<?> securityContextEnrichingFilter() {
        var filterRegistrationBean = new FilterRegistrationBean<>();
        filterRegistrationBean.setFilter((request, response, chain) -> {
            SecurityContext.setUserId(SecurityContextHolder.getContext()
                                                           .getAuthentication()
                                                           .getName());
            chain.doFilter(request, response);
        });
        filterRegistrationBean.setOrder(Integer.MAX_VALUE);
        return filterRegistrationBean;
    }
}
