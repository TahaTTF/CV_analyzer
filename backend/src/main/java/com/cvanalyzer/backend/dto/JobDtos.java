package com.cvanalyzer.backend.dto;

import java.time.Instant;
import java.util.List;
import com.cvanalyzer.backend.model.JobDocument;
import com.cvanalyzer.backend.model.JobSource;

public final class JobDtos {
    private JobDtos() {}
    public record Summary(String id,String title,String company,String location,boolean remote,
            JobSource source,List<String> skills,String url,Boolean isItRelevant,
            List<String> matchedCategories,String classificationVersion) {}
    public record Details(String id,String title,String company,String location,boolean remote,
            JobSource source,List<String> skills,String url,String description,
            Requirements requirements,List<String> employmentTypes,Instant publishedAt,
            Boolean isItRelevant,List<String> matchedCategories,String relevanceReason,
            String classificationVersion,String descriptionText,
            List<Skill> extractedSkills,List<Skill> toBeAcquiredSkills,
            NormalizedLocation normalizedLocation,String enrichmentVersion) {}
    public record Requirements(List<String> skills) {}
    public record Skill(String name,List<String> evidenceSources) {}
    public record NormalizedLocation(String original,String city,String region,String countryCode,
            String workMode,Double confidence) {}
    public record PageResponse<T>(List<T> items,int page,int size,long totalElements,int totalPages) {}

    public static Summary summary(JobDocument job) {
        return new Summary(job.id(),job.title(),company(job),location(job),remote(job),job.source(),
                displaySkills(job),job.url(),relevant(job),categories(job),version(job));
    }
    public static Details details(JobDocument job) {
        return new Details(job.id(),job.title(),company(job),location(job),remote(job),job.source(),
                displaySkills(job),job.url(),job.description(),
                new Requirements(job.requirements().skills()),job.employmentTypes(),job.publishedAt(),
                relevant(job),categories(job),reason(job),version(job),descriptionText(job),
                skills(job,false),skills(job,true),normalizedLocation(job),enrichmentVersion(job));
    }
    private static String company(JobDocument job){return job.company()==null?null:job.company().name();}
    private static String location(JobDocument job){return job.location()==null?null:job.location().displayName();}
    private static boolean remote(JobDocument job){return job.location()!=null&&job.location().remote();}
    private static Boolean relevant(JobDocument job){return job.itClassification()==null?null:
        job.itClassification().isItRelevant();}
    private static List<String> categories(JobDocument job){return job.itClassification()==null?List.of():
        job.itClassification().matchedCategories();}
    private static String version(JobDocument job){return job.itClassification()==null?null:
        job.itClassification().version();}
    private static String reason(JobDocument job){return job.itClassification()==null?null:
        job.itClassification().relevanceReason();}
    private static List<String> displaySkills(JobDocument job){
        if(job.enrichment()!=null&&!job.enrichment().requiredSkills().isEmpty())
            return job.enrichment().requiredSkills().stream().map(JobDocument.ExtractedSkill::name).toList();
        return job.requirements().skills();
    }
    private static String descriptionText(JobDocument job){return job.enrichment()==null?null:
        job.enrichment().descriptionText();}
    private static List<Skill> skills(JobDocument job,boolean training){
        if(job.enrichment()==null)return List.of();
        var values=training?job.enrichment().toBeAcquiredSkills():job.enrichment().requiredSkills();
        return values.stream().map(v->new Skill(v.name(),v.evidenceSources())).toList();
    }
    private static NormalizedLocation normalizedLocation(JobDocument job){
        if(job.enrichment()==null||job.enrichment().normalizedLocation()==null)return null;
        var v=job.enrichment().normalizedLocation();
        return new NormalizedLocation(v.original(),v.city(),v.region(),v.countryCode(),
            v.workMode()==null?null:v.workMode().name(),v.confidence());
    }
    private static String enrichmentVersion(JobDocument job){return job.enrichment()==null?null:
        job.enrichment().version();}
}
