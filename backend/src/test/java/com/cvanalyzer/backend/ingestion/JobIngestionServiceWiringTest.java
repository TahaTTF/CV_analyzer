package com.cvanalyzer.backend.ingestion;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import com.cvanalyzer.backend.config.ArbeitnowProperties;
import com.cvanalyzer.backend.ingestion.arbeitnow.ArbeitnowClient;
import com.cvanalyzer.backend.ingestion.arbeitnow.ArbeitnowJobMapper;
import com.cvanalyzer.backend.repository.JobRepository;

class JobIngestionServiceWiringTest {

    @Test
    void usesTheExplicitlyAutowiredProductionConstructor() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(ArbeitnowClient.class, () -> mock(ArbeitnowClient.class));
            context.registerBean(ArbeitnowJobMapper.class, () -> mock(ArbeitnowJobMapper.class));
            context.registerBean(JobRepository.class, () -> mock(JobRepository.class));
            context.registerBean(ArbeitnowProperties.class, () -> mock(ArbeitnowProperties.class));
            context.register(JobIngestionService.class);

            context.refresh();

            assertNotNull(context.getBean(JobIngestionService.class));
        }
    }
}
