package com.cvanalyzer.backend.ingestion;

import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cvanalyzer.backend.config.ArbeitnowProperties;
import com.cvanalyzer.backend.exception.ExternalJobValidationException;
import com.cvanalyzer.backend.ingestion.arbeitnow.ArbeitnowClient;
import com.cvanalyzer.backend.ingestion.arbeitnow.ArbeitnowJobMapper;
import com.cvanalyzer.backend.ingestion.arbeitnow.ArbeitnowResponse;
import com.cvanalyzer.backend.model.JobDocument;
import com.cvanalyzer.backend.model.JobSource;
import com.cvanalyzer.backend.repository.JobRepository;

@Service
public class JobIngestionService {
    private static final Logger log = LoggerFactory.getLogger(JobIngestionService.class);
    private final ArbeitnowClient client;
    private final ArbeitnowJobMapper mapper;
    private final JobRepository repository;
    private final ArbeitnowProperties properties;
    private final Clock clock;

    @Autowired
    public JobIngestionService(ArbeitnowClient client, ArbeitnowJobMapper mapper,
            JobRepository repository, ArbeitnowProperties properties) {
        this(client, mapper, repository, properties, Clock.systemUTC());
    }

    JobIngestionService(ArbeitnowClient client, ArbeitnowJobMapper mapper,
            JobRepository repository, ArbeitnowProperties properties, Clock clock) {
        this.client = client; this.mapper = mapper; this.repository = repository;
        this.properties = properties; this.clock = clock;
    }

    public IngestionSummary ingestArbeitnow() {
        Counters counts = new Counters();
        int pages=Math.min(properties.maxPages(),properties.requestBudget());
        for (int page = 1; page <= pages; page++) {
            ArbeitnowResponse response = client.fetchPage(page);
            List<ArbeitnowResponse.ArbeitnowJob> jobs = response.data() == null ? List.of() : response.data();
            counts.fetched += jobs.size();
            jobs.forEach(source -> ingestOne(source, counts));
            if (response.links() == null || response.links().next() == null
                    || response.links().next().isBlank()) break;
        }
        IngestionSummary result = counts.result();
        log.info("arbeitnow_ingestion_complete fetched={} inserted={} updated={} skipped={} failed={}",
                result.fetched(), result.inserted(), result.updated(), result.skipped(), result.failed());
        return result;
    }

    private void ingestOne(ArbeitnowResponse.ArbeitnowJob source, Counters counts) {
        try {
            JobDocument incoming = mapper.map(source);
            repository.findBySourceAndExternalId(JobSource.ARBEITNOW, incoming.externalId())
                    .ifPresentOrElse(existing -> {
                        if (incoming.hasSameSourceContent(existing)) counts.skipped++;
                        else {
                            repository.save(incoming.asUpdateOf(existing, clock.instant()));
                            counts.updated++;
                        }
                    }, () -> {
                        repository.save(incoming);
                        counts.inserted++;
                    });
        } catch (ExternalJobValidationException ex) {
            counts.failed++;
            log.warn("arbeitnow_job_rejected reason={}", ex.getMessage());
        }
    }

    private static final class Counters {
        int fetched, inserted, updated, skipped, failed;
        IngestionSummary result() { return new IngestionSummary(fetched, inserted, updated, skipped, failed); }
    }
}
