package com.personal.ipvalidator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class IpValidatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(IpValidatorApplication.class, args);
    }
}
