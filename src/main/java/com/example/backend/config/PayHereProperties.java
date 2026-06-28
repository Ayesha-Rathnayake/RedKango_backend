package com.example.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "payhere")
public class PayHereProperties {

    private String merchantId;
    private String merchantSecret;
    private String currency = "LKR";
    private Boolean sandbox = true;
    private String checkoutUrl;
}