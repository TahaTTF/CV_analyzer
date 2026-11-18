package com.cvanalyzer.backend.enrichment;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class DescriptionCleaningServiceTest {
 private final DescriptionCleaningService cleaner=new DescriptionCleaningService();

 @Test void removesUnsafeHtmlDecodesEntitiesAndPreservesBoundaries(){
  String result=cleaner.clean("<style>x{}</style><script>alert(1)</script><p>R&amp;D&nbsp;team</p>"
   +"<ul><li>C++ &amp; C#</li><li>.NET, Node.js and CI/CD</li></ul><p>&quot;Done&quot;</p>");
  assertThat(result).doesNotContain("alert","x{}","<p>")
   .contains("R&D team","- C++ & C#","- .NET, Node.js and CI/CD","\"Done\"");
 }
 @Test void preservesUnicodeAndHandlesPlainMalformedAndEmptyText(){
  assertThat(cleaner.clean("D\u00e9veloppeur \u00e0 M\u00fcnchen &amp; K\u00f6ln"))
   .isEqualTo("D\u00e9veloppeur \u00e0 M\u00fcnchen & K\u00f6ln");
  assertThat(cleaner.clean("<p>Unclosed <b>HTML")).contains("Unclosed HTML");
  assertThat(cleaner.clean(null)).isNull();
  assertThat(cleaner.clean("  ")).isEmpty();
 }
}
