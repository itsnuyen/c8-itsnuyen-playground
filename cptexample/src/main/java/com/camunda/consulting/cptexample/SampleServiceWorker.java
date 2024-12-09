package com.camunda.consulting.cptexample;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class SampleServiceWorker {

    @JobWorker(type = "fooWolkel")
    public void handleJobFoo(final ActivatedJob job) {
        // do whatever you need to do
        log.info("handleJobFoo {}", job);
    }
}
