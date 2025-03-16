package com.singlesignomni.web_api.config;

import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class OpenSamlConfig {
    @PostConstruct
    public void initOpenSAML() throws InitializationException {
        System.setProperty("org.apache.xml.security.ignoreLineBreaks", "true");
        InitializationService.initialize();
    }
}