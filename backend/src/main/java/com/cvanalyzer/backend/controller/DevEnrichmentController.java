package com.cvanalyzer.backend.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.cvanalyzer.backend.enrichment.DataQualityAudit;
import com.cvanalyzer.backend.enrichment.EnrichmentSummary;
import com.cvanalyzer.backend.enrichment.JobDataQualityAuditService;
import com.cvanalyzer.backend.enrichment.JobEnrichmentBatchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Validated
@Profile("local")
@RestController
@RequestMapping("/api/dev/enrichment")
public class DevEnrichmentController {
 private final JobEnrichmentBatchService enrichment;
 private final JobDataQualityAuditService audit;
 public DevEnrichmentController(JobEnrichmentBatchService enrichment,JobDataQualityAuditService audit){
  this.enrichment=enrichment;this.audit=audit;
 }
 @PostMapping("/jobs")
 public EnrichmentSummary enrichJobs(@RequestParam(defaultValue="true") boolean dryRun,
   @RequestParam(defaultValue="100") @Min(1) @Max(500) int batchSize){
  return enrichment.run(dryRun,batchSize);
 }
 @GetMapping("/audit")
 public DataQualityAudit audit(){return audit.audit();}
}
