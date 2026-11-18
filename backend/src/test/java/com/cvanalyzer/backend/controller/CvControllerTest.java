package com.cvanalyzer.backend.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.cvanalyzer.backend.cv.CvParsingService;
import com.cvanalyzer.backend.dto.CvParseResponse;

@WebMvcTest(CvController.class)
class CvControllerTest {
 @Autowired MockMvc mvc;
 @MockitoBean CvParsingService service;
 @Test void acceptsMultipartFileAndReturnsContract() throws Exception {
  var file=new MockMultipartFile("file","candidate.pdf","application/pdf","pdf".getBytes());
  var project=new CvParseResponse.Project("Sample Project","Built a Java service.",List.of("Java"));
  var profile=new CvParseResponse.Profile("Backend Developer",null,List.of("Java"),null,List.of(),List.of(project),List.of(),List.of());
  when(service.parse(any())).thenReturn(new CvParseResponse("SUCCESS","cv-parser-1.3",profile,null,
   "Technical skills: Java.",null,null,null,"Backend Developer. Technical skills: Java.",List.of()));
  mvc.perform(multipart("/api/cv/parse").file(file)).andExpect(status().isOk())
   .andExpect(jsonPath("$.parsingStatus").value("SUCCESS"))
   .andExpect(jsonPath("$.parserVersion").value("cv-parser-1.3"))
   .andExpect(jsonPath("$.profile.technicalSkills[0]").value("Java"))
   .andExpect(jsonPath("$.profile.projects[0].name").value("Sample Project"))
   .andExpect(jsonPath("$.profile.additionalSections").doesNotExist())
   .andExpect(jsonPath("$.semanticProfileText").value("Backend Developer. Technical skills: Java."))
   .andExpect(jsonPath("$.rawText").doesNotExist());
  verify(service).parse(any());
 }
 @Test void missingFileReturnsSanitizedBadRequest() throws Exception {
  mvc.perform(multipart("/api/cv/parse")).andExpect(status().isBadRequest())
   .andExpect(jsonPath("$.code").value("CV_FILE_REQUIRED"));
  verifyNoInteractions(service);
 }
}
