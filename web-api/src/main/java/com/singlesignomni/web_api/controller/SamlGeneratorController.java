package com.singlesignomni.web_api.controller;

import com.singlesignomni.web_api.service.SamlResponseGeneratorV2;
// import com.singlesignomni.web_api.service.SamlResponseGenerator.Arguments;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SamlGeneratorController {
    // @Autowired
    // private SamlResponseGeneratorV2 generator;

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
