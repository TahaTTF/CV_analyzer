package com.cvanalyzer.backend.enrichment;

import java.util.List;
import com.cvanalyzer.backend.model.JobDocument;

/**
 * Future offline-only extension point. Implementations may clean HTML, normalize locations,
 * deduplicate skills, or prepare embeddings from already stored jobs. They must never call
 * an external job-board API.
 */
public interface LocalJobEnrichmentPort {
    JobDocument enrich(JobDocument job);
    default List<JobDocument> enrichBatch(List<JobDocument> jobs){
        return jobs.stream().map(this::enrich).toList();
    }
}
