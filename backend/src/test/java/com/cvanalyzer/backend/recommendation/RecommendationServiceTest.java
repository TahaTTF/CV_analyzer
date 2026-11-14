package com.cvanalyzer.backend.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import com.cvanalyzer.backend.cv.CvParsingService;
import com.cvanalyzer.backend.dto.CvParseResponse;
import com.cvanalyzer.backend.dto.RecommendationDtos;
import com.cvanalyzer.backend.exception.RecommendationException;
import com.cvanalyzer.backend.model.*;
import com.cvanalyzer.backend.repository.JobRepository;

class RecommendationServiceTest {
 private final CvParsingService parser=mock(CvParsingService.class);
 private final JobRepository repository=mock(JobRepository.class);
 private final AiMatchingClient ai=mock(AiMatchingClient.class);
 private final RecommendationService service=new RecommendationService(parser,repository,ai);
 private final MockMultipartFile file=new MockMultipartFile("file","cv.pdf","application/pdf","pdf".getBytes());

 @Test void mapsParsedCvJobsAndAiResponse(){
  when(parser.parse(file)).thenReturn(parsed());when(repository.findAll()).thenReturn(List.of(job()));
  when(ai.match(any())).thenAnswer(invocation->{RecommendationDtos.AiMatchRequest request=invocation.getArgument(0);
   assertThat(request.semanticProfile()).isEqualTo("sanitized semantic profile");
   assertThat(request.skills()).containsExactly("Python");assertThat(request.jobs()).singleElement().satisfies(value->{
    assertThat(value.description()).isEqualTo("clean job text");assertThat(value.skills()).containsExactly("Python","SQL");});
   return new RecommendationDtos.AiMatchResponse("model-v1",List.of(new RecommendationDtos.AiMatchResult(
    "job-1",91,90,100,80,List.of("python"),List.of("sql"),"Evidence-based explanation.")));});
  var response=service.recommend(file,10);
  assertThat(response.parserVersion()).isEqualTo("cv-parser-1.3");
  assertThat(response.recommendations()).singleElement().satisfies(value->{
   assertThat(value.jobTitle()).isEqualTo("Data Engineer");assertThat(value.matchScore()).isEqualTo(91);});
 }
 @Test void rejectsEmptyCandidateSetWithoutCallingAi(){
  when(parser.parse(file)).thenReturn(parsed());when(repository.findAll()).thenReturn(List.of());
  assertThatThrownBy(()->service.recommend(file,10)).isInstanceOf(RecommendationException.class)
   .hasMessage("No active IT job candidates are available");verifyNoInteractions(ai);
 }
 private CvParseResponse parsed(){return new CvParseResponse("SUCCESS","cv-parser-1.3",
  new CvParseResponse.Profile("Data Engineer",null,List.of("Python"),null,List.of(),List.of(),List.of(),List.of()),
  null,null,null,null,null,"sanitized semantic profile",List.of());}
 private JobDocument job(){return new JobDocument("job-1",JobSource.ADZUNA,"ext","Data Engineer",
  new JobDocument.Company("Example"),"raw",new JobDocument.Location("Paris","Paris","FR",false),
  new JobDocument.Requirements(List.of("fallback")),List.of(),"https://example.test/job",Instant.now(),
  Instant.now(),Instant.now(),JobStatus.ACTIVE,null,new JobDocument.ItClassification(true,List.of(),"IT","v1",Instant.now()),
  new JobDocument.JobEnrichment("clean job text",List.of(new JobDocument.ExtractedSkill("Python",List.of()),
   new JobDocument.ExtractedSkill("SQL",List.of())),List.of(),null,"v1",Instant.now()));}
}
