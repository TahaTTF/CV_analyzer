package com.cvanalyzer.backend.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import com.cvanalyzer.backend.config.IngestionProperties;
import com.cvanalyzer.backend.exception.IngestionDisabledException;
import com.cvanalyzer.backend.ingestion.JobClassificationService;
import com.cvanalyzer.backend.ingestion.JobIngestionService;
import com.cvanalyzer.backend.ingestion.adzuna.AdzunaIngestionService;
import com.cvanalyzer.backend.ingestion.labonnealternance.LaBonneAlternanceIngestionService;

class DevIngestionControllerTest {
 @Test void disabledFlagPreventsEveryExternalIngestionCall(){
  JobIngestionService arbeitnow=mock(JobIngestionService.class);
  AdzunaIngestionService adzuna=mock(AdzunaIngestionService.class);
  LaBonneAlternanceIngestionService lba=mock(LaBonneAlternanceIngestionService.class);
  var controller=new DevIngestionController(arbeitnow,adzuna,lba,
   mock(JobClassificationService.class),new IngestionProperties(false));
  assertThatThrownBy(controller::ingestArbeitnow).isInstanceOf(IngestionDisabledException.class);
  assertThatThrownBy(controller::ingestAdzuna).isInstanceOf(IngestionDisabledException.class);
  assertThatThrownBy(controller::ingestLaBonneAlternance).isInstanceOf(IngestionDisabledException.class);
  verifyNoInteractions(arbeitnow,adzuna,lba);
 }
}
