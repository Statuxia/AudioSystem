package com.audiosystem.wsgateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

@SpringBootApplication
@PropertySources({
    @PropertySource("classpath:/application.properties"),
    @PropertySource(value = "file:/etc/ws-gateway/application.properties", ignoreResourceNotFound = true)
})
public class WsGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(WsGatewayApplication.class, args);
    }

}
