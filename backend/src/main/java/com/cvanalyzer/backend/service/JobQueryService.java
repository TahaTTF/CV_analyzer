package com.cvanalyzer.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import com.cvanalyzer.backend.exception.ResourceNotFoundException;
import com.cvanalyzer.backend.model.JobDocument;
import com.cvanalyzer.backend.model.JobSource;
import com.cvanalyzer.backend.model.JobStatus;
import com.cvanalyzer.backend.model.JobRelevanceFilter;
import com.cvanalyzer.backend.repository.JobRepository;

@Service
public class JobQueryService {
    private final MongoTemplate mongoTemplate;
    private final JobRepository repository;
    public JobQueryService(MongoTemplate mongoTemplate,JobRepository repository){
        this.mongoTemplate=mongoTemplate; this.repository=repository;
    }

    public Page<JobDocument> findJobs(int page,int size,String text,String location,JobSource source){
        return findJobs(page,size,text,location,source,JobRelevanceFilter.DEFAULT);
    }
    public Page<JobDocument> findJobs(int page,int size,String text,String location,JobSource source,
            JobRelevanceFilter relevance){
        Pageable pageable=PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"publishedAt"));
        List<Criteria> criteria=new ArrayList<>();
        criteria.add(Criteria.where("status").is(JobStatus.ACTIVE));
        if(hasText(text)){
            String regex=Pattern.quote(text.trim());
            criteria.add(new Criteria().orOperator(Criteria.where("title").regex(regex,"i"),
                    Criteria.where("company.name").regex(regex,"i"),
                    Criteria.where("description").regex(regex,"i")));
        }
        if(hasText(location)) criteria.add(Criteria.where("location.displayName")
                .regex(Pattern.quote(location.trim()),"i"));
        if(source!=null) criteria.add(Criteria.where("source").is(source));
        JobRelevanceFilter selected=relevance==null?JobRelevanceFilter.DEFAULT:relevance;
        switch(selected){
            case DEFAULT -> criteria.add(new Criteria().orOperator(
                Criteria.where("itClassification.isItRelevant").is(true),
                Criteria.where("itClassification").is(null)));
            case IT_ONLY -> criteria.add(Criteria.where("itClassification.isItRelevant").is(true));
            case NON_IT -> criteria.add(Criteria.where("itClassification.isItRelevant").is(false));
            case UNCLASSIFIED -> criteria.add(Criteria.where("itClassification").is(null));
            case AMBIGUOUS -> criteria.add(new Criteria().andOperator(
                Criteria.where("itClassification").ne(null),
                Criteria.where("itClassification.isItRelevant").is(null)));
            case ALL -> { }
        }
        Query query=new Query(new Criteria().andOperator(criteria.toArray(Criteria[]::new)));
        long total=mongoTemplate.count(Query.of(query).limit(-1).skip(-1),JobDocument.class);
        List<JobDocument> jobs=mongoTemplate.find(query.with(pageable),JobDocument.class);
        return new PageImpl<>(jobs,pageable,total);
    }

    public JobDocument findById(String id){
        return repository.findById(id).orElseThrow(
                ()->new ResourceNotFoundException("Job not found"));
    }
    private boolean hasText(String value){return value!=null&&!value.isBlank();}
}
