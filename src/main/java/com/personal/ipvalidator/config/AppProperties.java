package com.personal.ipvalidator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        int numberOfRequests,
        String proxyUrl,
        String proxyUsername,
        String proxyPassword
) {}
