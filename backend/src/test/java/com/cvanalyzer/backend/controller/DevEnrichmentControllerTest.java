package com.cvanalyzer.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.cvanalyzer.backend.enrichment.*;

@ActiveProfiles("local")
@WebMvcTest(DevEnrichmentController.class)
class DevEnrichmentControllerTest {
 @Autowired MockMvc mvc;
 @MockitoBean JobEnrichmentBatchService enrichment;
 @MockitoBean JobDataQualityAuditService audit;

 @Test void defaultsToDryRun() throws Exception {
  when(enrichment.run(true,100)).thenReturn(new EnrichmentSummary(true,391,391,0,0,0,390,200,300,
   LocalJobEnrichmentService.VERSION));
  mvc.perform(post("/api/dev/enrichment/jobs"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.dryRun").value(true))
   .andExpect(jsonPath("$.updated").value(0));
  verify(enrichment).run(true,100);
 }
 @Test void requiresLocalProfileAnnotation(){
  Profile profile=DevEnrichmentController.class.getAnnotation(Profile.class);
  assertThat(profile).isNotNull();assertThat(profile.value()).containsExactly("local");
 }
}
