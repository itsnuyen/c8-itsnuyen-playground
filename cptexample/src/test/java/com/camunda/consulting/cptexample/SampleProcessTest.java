package com.camunda.consulting.cptexample;

import io.camunda.process.test.api.CamundaAssert;
import io.camunda.process.test.api.CamundaProcessTestContext;
import io.camunda.process.test.api.CamundaSpringProcessTest;
import io.camunda.process.test.impl.assertions.CamundaDataSource;
import io.camunda.process.test.impl.assertions.ProcessInstanceAssertj;
import io.camunda.process.test.impl.client.CamundaApiClient;
import io.camunda.process.test.impl.client.FlowNodeInstanceDto;
import io.camunda.process.test.impl.client.ProcessInstanceDto;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@CamundaSpringProcessTest
public class SampleProcessTest {
    private static final int TIMEOUT_SECONDS = 10;

    @Autowired
    private ZeebeClient client;
    @Autowired
    private CamundaProcessTestContext processTestContext;

    private static final ThreadLocal<CamundaDataSource> camundaDataSource = new ThreadLocal<>();

    @BeforeEach
    void setUp() {
        camundaDataSource.set(new CamundaDataSource(processTestContext.getCamundaRestAddress().toString()));
    }


    @Test
    void shouldCompleteProcessInstance() {
        // given: the processes are deployed

        // when
        final ProcessInstanceEvent processInstance =
                client.newCreateInstanceCommand().bpmnProcessId("Process_SimpleTestCase").latestVersion().send().join();

        // then
        CamundaAssert.assertThat(processInstance).isCompleted();
    }

    @Test
    void shouldCompleteProcessInstanceWithMessage() throws IOException {
        // given: the processes are deployed
        client.newPublishMessageCommand()
                .messageName("Message_tmp")
                .correlationKey(UUID.randomUUID().toString())
                .send()
                .join();

        // then
        assertTrue(waitForProcessInstanceCompletion(),
                "Process instance did not complete within the timeout period.");

        ProcessInstanceDto processInstanceDto = camundaDataSource.get().findProcessInstances().get(0);
        new ProcessInstanceAssertj(camundaDataSource.get(), processInstanceDto.getKey())
                .isCompleted()
                .hasCompletedElements("Received Test Message", "Get Message", "Got Message");

        List<FlowNodeInstanceDto> flowNodes = camundaDataSource.get().getFlowNodeInstancesByProcessInstanceKey(processInstanceDto.getKey());

        assertEquals(3, flowNodes.size(), "Executed not enough elements");
    }

    @Test
    void shouldCompleteProcessInstanceWithServiceWorker() throws IOException {
        // given: the processes are deployed
        client.newPublishMessageCommand()
                .messageName("Message_sampleWorker")
                .correlationKey(UUID.randomUUID().toString())
                .send()
                .join();

        // then
        assertTrue(waitForProcessInstanceCompletion(),
                "Process instance did not complete within the timeout period.");

        ProcessInstanceDto processInstanceDto = camundaDataSource.get().findProcessInstances().get(0);
        new ProcessInstanceAssertj(camundaDataSource.get(), processInstanceDto.getKey())
                .isCompleted()
                .hasCompletedElements("MessageStart", "Foo Worker", "User Task Example");

        List<FlowNodeInstanceDto> flowNodes = camundaDataSource.get().getFlowNodeInstancesByProcessInstanceKey(processInstanceDto.getKey());

        assertEquals(5, flowNodes.size(), "Executed not enough elements");
    }

    private boolean waitForProcessInstanceCompletion() throws IOException {
        long timeout = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS);
        while (System.currentTimeMillis() < timeout) {
            if (isProcessInstanceAvailable()) {
                return true;
            }
            sleepBriefly();
        }
        return false;
    }

    private boolean isProcessInstanceAvailable() throws IOException {
        List<ProcessInstanceDto> processInstances = camundaDataSource.get().findProcessInstances();
        return !processInstances.isEmpty();
    }

    private void sleepBriefly() {
        try {
            TimeUnit.MILLISECONDS.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
