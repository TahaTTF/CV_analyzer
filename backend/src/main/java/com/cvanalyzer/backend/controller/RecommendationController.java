package com.cvanalyzer.backend.controller;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.cvanalyzer.backend.dto.RecommendationDtos;
import com.cvanalyzer.backend.recommendation.RecommendationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {
 private final RecommendationService service;
 public RecommendationController(RecommendationService service){this.service=service;}
 @PostMapping(value="/from-cv",consumes=MediaType.MULTIPART_FORM_DATA_VALUE,
  produces=MediaType.APPLICATION_JSON_VALUE)
 public RecommendationDtos.Response fromCv(@RequestPart("file") MultipartFile file,
  @RequestParam(defaultValue="10") @Min(1) @Max(50) int topK){return service.recommend(file,topK);}
}
