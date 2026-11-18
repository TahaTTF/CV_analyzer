package com.cvanalyzer.backend.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.cvanalyzer.backend.cv.CvParsingService;
import com.cvanalyzer.backend.dto.CvParseResponse;

@RestController
@RequestMapping("/api/cv")
public class CvController {
 private final CvParsingService service;
 public CvController(CvParsingService service){this.service=service;}
 @PostMapping(value="/parse",consumes=MediaType.MULTIPART_FORM_DATA_VALUE,
  produces=MediaType.APPLICATION_JSON_VALUE)
 public CvParseResponse parse(@RequestPart("file") MultipartFile file){return service.parse(file);}
}
