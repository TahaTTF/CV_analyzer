package com.cvanalyzer.backend.repository;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.cvanalyzer.backend.model.JobDocument;
import com.cvanalyzer.backend.model.JobSource;
public interface JobRepository extends MongoRepository<JobDocument,String>{
 Optional<JobDocument> findBySourceAndExternalId(JobSource source,String externalId);
}
