package com.cvanalyzer.backend.ingestion;

import java.time.Clock;
import java.util.List;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.cvanalyzer.backend.exception.ExternalJobValidationException;
import com.cvanalyzer.backend.model.JobDocument;
import com.cvanalyzer.backend.repository.JobRepository;

@Service
public class JobPersistenceService {
    private static final Logger log=LoggerFactory.getLogger(JobPersistenceService.class);
    private final JobRepository repository;
    private final Clock clock;
    @Autowired
    public JobPersistenceService(JobRepository repository){this(repository,Clock.systemUTC());}
    JobPersistenceService(JobRepository repository,Clock clock){
        this.repository=repository;this.clock=clock;
    }
    public <T> IngestionSummary persist(String provider,List<T> records,
            Function<T,JobDocument> mapper){
        MutableCounts counts=new MutableCounts();
        List<T> safe=records==null?List.of():records;
        counts.fetched=safe.size();
        for(T source:safe){
            try{
                JobDocument incoming=mapper.apply(source);
                repository.findBySourceAndExternalId(incoming.source(),incoming.externalId())
                    .ifPresentOrElse(existing->{
                        if(incoming.hasSameSourceContent(existing))counts.skipped++;
                        else{
                            repository.save(incoming.asUpdateOf(existing,clock.instant()));
                            counts.updated++;
                        }
                    },()->{
                        repository.save(incoming);
                        counts.inserted++;
                    });
            }catch(ExternalJobValidationException ex){
                counts.failed++;
                log.warn("{}_job_rejected reason={}",provider,ex.getMessage());
            }
        }
        return counts.result();
    }
    private static final class MutableCounts{
        int fetched,inserted,updated,skipped,failed;
        IngestionSummary result(){return new IngestionSummary(fetched,inserted,updated,skipped,failed);}
    }
}
