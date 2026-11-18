package com.cvanalyzer.backend.controller;

import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.cvanalyzer.backend.dto.JobDtos;
import com.cvanalyzer.backend.model.JobDocument;
import com.cvanalyzer.backend.model.JobSource;
import com.cvanalyzer.backend.model.JobRelevanceFilter;
import com.cvanalyzer.backend.service.JobQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@Validated
@RestController
@RequestMapping("/api/jobs")
public class JobController {
    private final JobQueryService service;
    public JobController(JobQueryService service){this.service=service;}

    @GetMapping
    public JobDtos.PageResponse<JobDtos.Summary> list(
            @RequestParam(defaultValue="0") @Min(0) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int size,
            @RequestParam(required=false) @Size(max=200) String query,
            @RequestParam(required=false) @Size(max=120) String location,
            @RequestParam(required=false) JobSource source,
            @RequestParam(defaultValue="DEFAULT") JobRelevanceFilter relevance){
        Page<JobDocument> result=service.findJobs(page,size,query,location,source,relevance);
        return new JobDtos.PageResponse<>(result.getContent().stream().map(JobDtos::summary).toList(),
                result.getNumber(),result.getSize(),result.getTotalElements(),result.getTotalPages());
    }

    @GetMapping("/{jobId}")
    public JobDtos.Details get(@PathVariable @Size(min=1,max=100) String jobId){
        return JobDtos.details(service.findById(jobId));
    }
}
