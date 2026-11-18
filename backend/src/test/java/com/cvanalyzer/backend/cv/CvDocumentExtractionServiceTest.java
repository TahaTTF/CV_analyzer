package com.cvanalyzer.backend.cv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.io.IOException;
import java.awt.Color;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import com.cvanalyzer.backend.config.CvParserProperties;
import com.cvanalyzer.backend.exception.CvParsingException;

class CvDocumentExtractionServiceTest {
 private final CvDocumentExtractionService service=new CvDocumentExtractionService(
  new CvParserProperties(5L*1024*1024,500_000,100),new CvTextNormalizationService());

 @Test void extractsValidPdfAndPreservesParagraphText() throws Exception {
  byte[] bytes=CvTestFixtures.pdf("Backend Developer","Professional Summary",
   "Builds Java and Spring Boot services.","Skills: Java, Docker");
  var result=service.extract(file("candidate.pdf","application/pdf",bytes));
  assertThat(result.type()).isEqualTo(CvDocumentType.PDF);assertThat(result.pageCount()).isOne();
  assertThat(result.text()).contains("Backend Developer","Java and Spring Boot","Skills: Java, Docker");
 }
 @Test void extractsValidDocx() throws Exception {
  byte[] bytes=CvTestFixtures.docx("Developpeur Backend","Competences","Java, MongoDB, Docker");
  var result=service.extract(file("candidate.docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document",bytes));
  assertThat(result.type()).isEqualTo(CvDocumentType.DOCX);
  assertThat(result.text()).contains("Developpeur Backend","Java, MongoDB, Docker");
 }
 @Test void reconstructsTwoColumnsWithoutRowInterleaving() throws Exception {
  var page=List.of(new CvTestFixtures.PlacedText(50,760,"Backend Developer"),
   new CvTestFixtures.PlacedText(50,710,"Profile"),
   new CvTestFixtures.PlacedText(50,710,11,"Profile",Color.BLUE),
   new CvTestFixtures.PlacedText(50,690,"Builds reliable services."),
   new CvTestFixtures.PlacedText(50,650,"Education"),
   new CvTestFixtures.PlacedText(50,630,"Engineering Degree"),
   new CvTestFixtures.PlacedText(50,610,"Example Institute"),
   new CvTestFixtures.PlacedText(330,710,"Experience"),
   new CvTestFixtures.PlacedText(330,690,"Backend Developer | Example Company"),
   new CvTestFixtures.PlacedText(330,670,"2024 - Present"),
   new CvTestFixtures.PlacedText(330,650,"Built REST APIs."),
   new CvTestFixtures.PlacedText(330,610,"Projects"),
   new CvTestFixtures.PlacedText(330,590,"Service Platform"));
  var result=service.extract(file("columns.pdf","application/pdf",
   CvTestFixtures.positionedPdf(List.of(page),false)));
  assertThat(result.text()).contains("Profile","Education","Experience","Projects");
  assertThat(result.text()).containsOnlyOnce("Profile");
  assertThat(result.text().indexOf("Education")).isLessThan(result.text().indexOf("Experience"));
  assertThat(result.text()).doesNotContain("Profile Experience","Education Built REST APIs");
 }
 @Test void preservesPageBoundariesAccentsColorsAndIgnoresPhoto() throws Exception {
  var first=List.of(new CvTestFixtures.PlacedText(50,740,14,"Profil",Color.BLUE),
   new CvTestFixtures.PlacedText(50,715,11,"Développeuse backend",Color.DARK_GRAY));
  var second=List.of(new CvTestFixtures.PlacedText(50,740,14,"Expérience professionnelle",Color.RED),
   new CvTestFixtures.PlacedText(50,715,11,"Stagiaire développement",Color.BLACK));
  var result=service.extract(file("multipage.pdf","application/pdf",
   CvTestFixtures.positionedPdf(List.of(first,second),true)));
  assertThat(result.pageCount()).isEqualTo(2);
  assertThat(result.text()).contains("Profil","Développeuse backend","Expérience professionnelle",
   "Stagiaire développement");
  assertThat(result.text().indexOf("Profil")).isLessThan(result.text().indexOf("Expérience professionnelle"));
 }
 @Test void preservesAsymmetricColumnContinuityAcrossPages() throws Exception {
  var first=List.of(new CvTestFixtures.PlacedText(35,740,"Profile"),
   new CvTestFixtures.PlacedText(35,720,"Backend student"),new CvTestFixtures.PlacedText(35,700,"Java services"),
   new CvTestFixtures.PlacedText(35,680,"Cloud systems"),new CvTestFixtures.PlacedText(35,660,"Team projects"),
   new CvTestFixtures.PlacedText(250,740,"Education"),new CvTestFixtures.PlacedText(250,720,"Engineering Degree"),
   new CvTestFixtures.PlacedText(250,700,"Example University"),new CvTestFixtures.PlacedText(250,680,"2024 - Present"),
   new CvTestFixtures.PlacedText(250,660,"Data specialization"));
  var second=List.of(new CvTestFixtures.PlacedText(35,740,"Languages"),
   new CvTestFixtures.PlacedText(35,720,"English"),new CvTestFixtures.PlacedText(35,700,"French"),
   new CvTestFixtures.PlacedText(35,680,"Arabic"),new CvTestFixtures.PlacedText(35,660,"Interests"),
   new CvTestFixtures.PlacedText(250,740,"Projects"),new CvTestFixtures.PlacedText(250,720,"Analytics Platform"),
   new CvTestFixtures.PlacedText(250,700,"Built ETL services."),new CvTestFixtures.PlacedText(250,680,"Technologies: Java"),
   new CvTestFixtures.PlacedText(250,660,"Deployment project"));
  var result=service.extract(file("sidebar.pdf","application/pdf",
   CvTestFixtures.positionedPdf(List.of(first,second),false)));
  assertThat(result.text().indexOf("Languages")).isLessThan(result.text().indexOf("Education"));
  assertThat(result.text().indexOf("Education")).isLessThan(result.text().indexOf("Projects"));
 }
 @Test void rejectsUnsupportedMalformedAndExtensionSpoofing(){
  assertCode(file("candidate.txt","text/plain","plain text content".getBytes()),"CV_FORMAT_UNSUPPORTED");
  assertCode(file("candidate.pdf","application/pdf","not a pdf".getBytes()),"CV_FORMAT_UNSUPPORTED");
  assertCode(file("candidate.docx","application/zip",new byte[]{'P','K',3,4,1,2}),"CV_FORMAT_UNSUPPORTED");
 }
 @Test void rejectsEmptyAndOversizedFiles(){
  assertCode(file("empty.pdf","application/pdf",new byte[0]),"CV_FILE_EMPTY");
  assertCode(file("large.pdf","application/pdf",new byte[5*1024*1024+1]),"CV_FILE_TOO_LARGE");
 }
 @Test void rejectsEncryptedAndImageOnlyPdfs() throws IOException {
  assertCode(file("encrypted.pdf","application/pdf",CvTestFixtures.encryptedPdf()),"CV_PDF_ENCRYPTED");
  assertCode(file("scan.pdf","application/pdf",CvTestFixtures.imageOnlyPdf()),"CV_PDF_NO_TEXT");
 }
 @Test void rejectsMacroBearingDocx() throws IOException {
  assertCode(file("candidate.docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document",
   CvTestFixtures.docxWithMacro()),"CV_DOCX_MACROS_NOT_ALLOWED");
 }
 private void assertCode(MockMultipartFile file,String code){assertThatThrownBy(()->service.extract(file))
  .isInstanceOf(CvParsingException.class).extracting(e->((CvParsingException)e).code()).isEqualTo(code);}
 private MockMultipartFile file(String name,String type,byte[] bytes){return new MockMultipartFile(
  "file",name,type,bytes);}
}
