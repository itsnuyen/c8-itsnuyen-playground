package com.camunda.consulting.cptexample;

import io.camunda.zeebe.spring.client.annotation.Deployment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Deployment(resources = {"classpath*:/bpmn/**/*.bpmn", "classpath*:/bpmn/**/*.form"})
public class CptexampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(CptexampleApplication.class, args);
    }

}
