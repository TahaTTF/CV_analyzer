package com.cvanalyzer.backend.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.cvanalyzer.backend.dto.RecommendationDtos;
import com.cvanalyzer.backend.exception.RecommendationException;
import com.cvanalyzer.backend.recommendation.RecommendationService;

@WebMvcTest(RecommendationController.class)
class RecommendationControllerTest {
 @Autowired MockMvc mvc;@MockitoBean RecommendationService service;
 private final MockMultipartFile file=new MockMultipartFile("file","cv.pdf","application/pdf","pdf".getBytes());
 @Test void acceptsMultipartAndMapsResponse() throws Exception{
  when(service.recommend(any(),eq(5))).thenReturn(new RecommendationDtos.Response("cv-parser-1.3","model-v1",
   List.of(),List.of(new RecommendationDtos.Recommendation("job-1","Developer","Example","Paris","ADZUNA",
    "https://example.test",88,90,80,70,List.of("java"),List.of(),"Strong match."))));
  mvc.perform(multipart("/api/recommendations/from-cv").file(file).param("topK","5"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.recommendations[0].jobId").value("job-1"))
   .andExpect(jsonPath("$.modelVersion").value("model-v1"));
 }
 @Test void validatesTopK() throws Exception{
  mvc.perform(multipart("/api/recommendations/from-cv").file(file).param("topK","51"))
   .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
 }
 @Test void mapsAiTimeout() throws Exception{
  when(service.recommend(any(),anyInt())).thenThrow(new RecommendationException(HttpStatus.GATEWAY_TIMEOUT,
   "AI_SERVICE_TIMEOUT","AI matching service timed out"));
  mvc.perform(multipart("/api/recommendations/from-cv").file(file))
   .andExpect(status().isGatewayTimeout()).andExpect(jsonPath("$.code").value("AI_SERVICE_TIMEOUT"));
 }
}
