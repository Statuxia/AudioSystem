package com.audiosystem.api.config;

import com.audiosystem.api.annotations.RateLimit;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@OpenAPIDefinition(
    info = @Info(
        title = "AudioSystem API",
        description = "Documentation for Rest API part of audiosystem"
    )
)
@Configuration
public class SwaggerConfiguration {

    @Bean
    public OperationCustomizer rateLimitOperationCustomizer() {
        return (operation, handlerMethod) -> {
            final RateLimit annotation = handlerMethod.getMethod().getAnnotation(RateLimit.class);
            if (annotation == null) {
                return operation;
            }

            final String description = operation.getDescription();
            final StringBuilder builder = new StringBuilder(description);
            if (StringUtils.hasText(description)) {
                builder.append("\n<h2>Limits\n");
            } else {
                builder.append("<h2>Limits\n");
            }

            builder.append("Request per minute: ").append(annotation.requestsPerMinute()).append("\n");
            builder.append("Burst: ").append(annotation.requestsPerSecond());

            operation.setDescription(builder.toString());
            return operation;
        };
    }
}
