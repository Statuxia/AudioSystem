package com.soundservice.api.config.properties;

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
    private Integer port;
    private Map<String, S3ProfileProperties> profile;
    private Map<String, S3BucketProperties> bucket;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class S3ProfileProperties {
        private String user;
        private String password;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class S3BucketProperties {
        private String name;
    }
}
