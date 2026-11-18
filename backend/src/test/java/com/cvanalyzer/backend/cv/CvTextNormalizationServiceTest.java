package com.cvanalyzer.backend.cv;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class CvTextNormalizationServiceTest {
 @Test void preservesSectionsUnicodeAndTechnicalPunctuation(){
  String result=new CvTextNormalizationService().normalize(
   "R\u00e9sum\u00e9\r\n\r\n  Java   C++  C#  .NET  Node.js  CI/CD\r\nM\u00fcnchen");
  assertThat(result).isEqualTo("R\u00e9sum\u00e9\n\nJava C++ C# .NET Node.js CI/CD\nM\u00fcnchen");
 }
}
