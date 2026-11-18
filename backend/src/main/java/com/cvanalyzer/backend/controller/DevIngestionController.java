package com.cvanalyzer.backend.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cvanalyzer.backend.ingestion.IngestionSummary;
import com.cvanalyzer.backend.ingestion.JobIngestionService;
import com.cvanalyzer.backend.ingestion.ClassificationSummary;
import com.cvanalyzer.backend.ingestion.JobClassificationService;
import com.cvanalyzer.backend.ingestion.adzuna.AdzunaIngestionService;
import com.cvanalyzer.backend.ingestion.labonnealternance.LaBonneAlternanceIngestionService;
import com.cvanalyzer.backend.config.IngestionProperties;
import com.cvanalyzer.backend.exception.IngestionDisabledException;

@Profile("local")
@RestController
@RequestMapping("/api/dev/ingestion")
public class DevIngestionController {
    private final JobIngestionService service;
    private final AdzunaIngestionService adzuna;
    private final LaBonneAlternanceIngestionService laBonneAlternance;
    private final JobClassificationService classification;
    private final IngestionProperties properties;
    public DevIngestionController(JobIngestionService service,AdzunaIngestionService adzuna,
            LaBonneAlternanceIngestionService laBonneAlternance,
            JobClassificationService classification,IngestionProperties properties){
        this.service=service;this.adzuna=adzuna;this.laBonneAlternance=laBonneAlternance;
        this.classification=classification;this.properties=properties;
    }
    @PostMapping("/arbeitnow")
    public IngestionSummary ingestArbeitnow(){requireEnabled();return service.ingestArbeitnow();}
    @PostMapping("/adzuna")
    public IngestionSummary ingestAdzuna(){requireEnabled();return adzuna.ingest();}
    @PostMapping("/la-bonne-alternance")
    public IngestionSummary ingestLaBonneAlternance(){requireEnabled();return laBonneAlternance.ingest();}
    @PostMapping("/classify-existing")
    public ClassificationSummary classifyExisting(){return classification.classifyOutdated();}
    private void requireEnabled(){
        if(!properties.enabled())throw new IngestionDisabledException(
            "External job ingestion is disabled; set JOB_INGESTION_ENABLED=true for an intentional local run");
    }
}
