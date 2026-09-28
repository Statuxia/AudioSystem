package com.soundservice.processor.config.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties("s3")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class S3ConfigurationProperties {

    private String server;
    private Map<String, S3PropertiesProfile> profile;
    private Map<String, S3PropertiesBucket> bucket;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class S3PropertiesProfile {
        private String user;
        private String password;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class S3PropertiesBucket {
        private String name;
    }
}
