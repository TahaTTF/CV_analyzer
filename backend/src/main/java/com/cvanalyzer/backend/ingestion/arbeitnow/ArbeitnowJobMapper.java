package com.cvanalyzer.backend.ingestion.arbeitnow;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.cvanalyzer.backend.exception.ExternalJobValidationException;
import com.cvanalyzer.backend.model.JobDocument;
import com.cvanalyzer.backend.model.JobSource;
import com.cvanalyzer.backend.model.JobStatus;
import com.cvanalyzer.backend.ingestion.ItJobClassifier;

@Component
public class ArbeitnowJobMapper {
    private final Clock clock;
    private final ItJobClassifier classifier;

    @Autowired
    public ArbeitnowJobMapper(ItJobClassifier classifier) { this(Clock.systemUTC(),classifier); }
    public ArbeitnowJobMapper() { this(Clock.systemUTC(),new ItJobClassifier()); }
    ArbeitnowJobMapper(Clock clock) { this(clock,new ItJobClassifier(clock)); }
    ArbeitnowJobMapper(Clock clock,ItJobClassifier classifier) {
        this.clock = clock; this.classifier=classifier;
    }

    public JobDocument map(ArbeitnowResponse.ArbeitnowJob source) {
        if (source == null) throw invalid("record is null");
        String slug = required(source.slug(), "slug");
        String title = required(source.title(), "title");
        String company = required(source.companyName(), "company_name");
        String url = required(source.url(), "url");
        validateHttpUrl(url);
        Instant now = clock.instant();
        Instant published = source.createdAt() == null || source.createdAt() <= 0
                ? null : Instant.ofEpochSecond(source.createdAt());
        List<String> types = source.jobTypes() == null ? List.of()
                : source.jobTypes().stream().filter(v -> v != null && !v.isBlank())
                        .map(String::trim).distinct().toList();
        String displayLocation = blankToNull(source.location());
        return new JobDocument(null, JobSource.ARBEITNOW, slug, title,
                new JobDocument.Company(company), blankToNull(source.description()),
                new JobDocument.Location(displayLocation, null, null, Boolean.TRUE.equals(source.remote())),
                new JobDocument.Requirements(List.of()), types, url, published, now, now, JobStatus.ACTIVE,
                new JobDocument.SourceMetadata(null,slug,List.of(),clean(source.tags()),List.of()),
                classifier.classify(title,source.description(),source.tags(),List.of()));
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) throw invalid("missing required field: " + field);
        return value.trim();
    }

    private void validateHttpUrl(String value) {
        try {
            URI uri = URI.create(value);
            if (!uri.isAbsolute() || (!"http".equalsIgnoreCase(uri.getScheme())
                    && !"https".equalsIgnoreCase(uri.getScheme()))) throw invalid("url must use HTTP(S)");
        } catch (IllegalArgumentException ex) {
            throw invalid("url is invalid");
        }
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private List<String> clean(List<String> values) {
        return values==null?List.of():values.stream().filter(v->v!=null&&!v.isBlank())
            .map(String::trim).distinct().toList();
    }
    private ExternalJobValidationException invalid(String reason) {
        return new ExternalJobValidationException("Invalid Arbeitnow job: " + reason);
    }
}
