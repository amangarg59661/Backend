package com.edss.shared.config;

import com.edss.shared.security.OwnershipPermissionEvaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Register the ownership-aware {@link OwnershipPermissionEvaluator} with
 * Spring Security's method-level expression handler so that
 * {@code @PreAuthorize("hasPermission(#target, 'projects:project:read:own')")}
 * consults it.
 */
@Configuration
@EnableMethodSecurity
class MethodSecurityConfig {

    @Bean
    MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            OwnershipPermissionEvaluator evaluator) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(evaluator);
        return handler;
    }
}
