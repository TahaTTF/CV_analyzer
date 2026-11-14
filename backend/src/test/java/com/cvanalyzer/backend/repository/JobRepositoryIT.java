package com.cvanalyzer.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.cvanalyzer.backend.model.*;

@DataMongoTest(properties="spring.data.mongodb.auto-index-creation=true")
@Testcontainers(disabledWithoutDocker=true)
class JobRepositoryIT {
 @Container static final MongoDBContainer mongo=new MongoDBContainer("mongo:8.0");
 @DynamicPropertySource static void mongoProperties(DynamicPropertyRegistry registry){
  registry.add("spring.data.mongodb.uri",mongo::getReplicaSetUrl);
 }
 @Autowired JobRepository repository;

 @Test void storesFindsAndEnforcesSourceExternalIdUniqueness(){
  JobDocument first=repository.save(job(null,"external-1"));
  assertThat(repository.findBySourceAndExternalId(JobSource.ARBEITNOW,"external-1"))
    .get().extracting(JobDocument::id).isEqualTo(first.id());
  assertThatThrownBy(()->repository.save(job(null,"external-1")))
    .isInstanceOf(DuplicateKeyException.class);
 }

 private JobDocument job(String id,String externalId){
  Instant now=Instant.parse("2026-10-20T10:00:00Z");
  return new JobDocument(id,JobSource.ARBEITNOW,externalId,"Engineer",
    new JobDocument.Company("Example"),null,
    new JobDocument.Location("Berlin",null,null,false),new JobDocument.Requirements(List.of()),
    List.of(),"https://example.test/job",now,now,now,JobStatus.ACTIVE);
 }
}
