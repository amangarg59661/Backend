package com.edss.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edss.admin")
public record AdminSeedProperties(String email, String password) {}
