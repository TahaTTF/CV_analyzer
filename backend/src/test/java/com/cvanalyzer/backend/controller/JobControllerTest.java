package com.cvanalyzer.backend.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.cvanalyzer.backend.exception.ResourceNotFoundException;
import com.cvanalyzer.backend.model.*;
import com.cvanalyzer.backend.service.JobQueryService;

@WebMvcTest(JobController.class)
class JobControllerTest {
 @Autowired MockMvc mvc;
 @MockitoBean JobQueryService service;

 @Test void listsJobsUsingContractShape() throws Exception {
  when(service.findJobs(0,20,"java","Paris",JobSource.ARBEITNOW,JobRelevanceFilter.DEFAULT))
    .thenReturn(new PageImpl<>(List.of(job())));
  mvc.perform(get("/api/jobs").param("query","java").param("location","Paris")
    .param("source","ARBEITNOW"))
    .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
    .andExpect(jsonPath("$.items[0].id").value("job-1"))
    .andExpect(jsonPath("$.items[0].company").value("Example"))
    .andExpect(jsonPath("$.items[0].skills").isArray())
    .andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.totalElements").value(1));
 }

 @Test void getsJobById() throws Exception {
  when(service.findById("job-1")).thenReturn(job());
  mvc.perform(get("/api/jobs/job-1")).andExpect(status().isOk())
    .andExpect(jsonPath("$.id").value("job-1"))
    .andExpect(jsonPath("$.description").value("Description"));
 }

 @Test void returnsDocumentedNotFoundError() throws Exception {
  when(service.findById("missing")).thenThrow(new ResourceNotFoundException("Job not found"));
  mvc.perform(get("/api/jobs/missing")).andExpect(status().isNotFound())
    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
    .andExpect(jsonPath("$.path").value("/api/jobs/missing"));
 }

 @Test void returnsNotFoundForRootWithoutAUi() throws Exception {
  mvc.perform(get("/"))
    .andExpect(status().isNotFound())
    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
    .andExpect(jsonPath("$.message").value("Resource not found"))
    .andExpect(jsonPath("$.path").value("/"));
 }

 @Test void returnsNotFoundForUnknownRoute() throws Exception {
  mvc.perform(get("/not-a-real-route"))
    .andExpect(status().isNotFound())
    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
    .andExpect(jsonPath("$.message").value("Resource not found"))
    .andExpect(jsonPath("$.path").value("/not-a-real-route"));
 }

 private JobDocument job(){
  Instant now=Instant.parse("2026-10-20T10:00:00Z");
  return new JobDocument("job-1",JobSource.ARBEITNOW,"source-1","Java Developer",
    new JobDocument.Company("Example"),"Description",
    new JobDocument.Location("Paris, France","Paris","FR",true),
    new JobDocument.Requirements(List.of()),List.of("Full Time"),"https://example.test/job",
    now,now,now,JobStatus.ACTIVE);
 }
}
