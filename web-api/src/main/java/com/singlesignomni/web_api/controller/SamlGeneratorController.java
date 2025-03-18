package com.singlesignomni.web_api.controller;

import com.singlesignomni.web_api.service.SamlResponseGeneratorV2;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SamlGeneratorController {
    @PostMapping("/generate")
    public String generateSamlResponse(
            @RequestBody SamlResponseGeneratorV2.Arguments arguments) {
        try {
            return SamlResponseGeneratorV2.generateSAMLResponse(arguments);
        } catch (Exception e) {
            System.out.println(e);
            return "some error occured";
        }
    }
}
