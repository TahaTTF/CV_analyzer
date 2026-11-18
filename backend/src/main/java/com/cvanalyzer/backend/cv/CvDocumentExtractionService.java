package com.cvanalyzer.backend.cv;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.cvanalyzer.backend.config.CvParserProperties;
import com.cvanalyzer.backend.exception.CvParsingException;

@Service
public class CvDocumentExtractionService {
 private static final long MAX_DOCX_UNCOMPRESSED_BYTES=50L*1024*1024;
 private final CvParserProperties properties;
 private final CvTextNormalizationService normalizer;
 public CvDocumentExtractionService(CvParserProperties properties,CvTextNormalizationService normalizer){
  this.properties=properties;this.normalizer=normalizer;
 }
 public ExtractedCvDocument extract(MultipartFile file){
  if(file==null||file.isEmpty())throw error(HttpStatus.BAD_REQUEST,"CV_FILE_EMPTY","CV file is empty");
  if(file.getSize()>properties.maxFileBytes())
   throw error(HttpStatus.PAYLOAD_TOO_LARGE,"CV_FILE_TOO_LARGE","CV file exceeds the 5 MB limit");
  byte[] bytes;
  try{bytes=file.getBytes();}catch(IOException ex){throw error(HttpStatus.BAD_REQUEST,
   "CV_FILE_UNREADABLE","CV file could not be read",ex);}
  String name=file.getOriginalFilename()==null?"":file.getOriginalFilename().toLowerCase(Locale.ROOT);
  boolean pdfMagic=startsWith(bytes,"%PDF-".getBytes(StandardCharsets.US_ASCII));
  boolean zipMagic=bytes.length>=4&&bytes[0]=='P'&&bytes[1]=='K';
  if(name.endsWith(".pdf")){
   if(!pdfMagic)throw unsupported("File extension does not match PDF content");
   return extractPdf(bytes);
  }
  if(name.endsWith(".docx")){
   if(!zipMagic)throw unsupported("File extension does not match DOCX content");
   return extractDocx(bytes);
  }
  throw unsupported("Only PDF and DOCX CV files are supported");
 }
 private ExtractedCvDocument extractPdf(byte[] bytes){
  try(PDDocument document=Loader.loadPDF(bytes)){
   if(document.isEncrypted())throw error(HttpStatus.UNPROCESSABLE_ENTITY,"CV_PDF_ENCRYPTED",
    "Encrypted PDF files are not supported");
   int pages=document.getNumberOfPages();
   if(pages<1)throw error(HttpStatus.UNPROCESSABLE_ENTITY,"CV_PDF_EMPTY","PDF contains no pages");
   if(pages>properties.maxPdfPages())throw error(HttpStatus.PAYLOAD_TOO_LARGE,"CV_PDF_TOO_MANY_PAGES",
    "PDF exceeds the page limit");
   String text=checkedText(new LayoutAwarePdfTextExtractor().extract(document));
   if(text.length()<20)throw error(HttpStatus.UNPROCESSABLE_ENTITY,"CV_PDF_NO_TEXT",
    "PDF has no extractable text; scanned PDFs require OCR, which is not supported yet");
   return new ExtractedCvDocument(CvDocumentType.PDF,text,pages);
  }catch(InvalidPasswordException ex){throw error(HttpStatus.UNPROCESSABLE_ENTITY,"CV_PDF_ENCRYPTED",
   "Encrypted PDF files are not supported",ex);
  }catch(CvParsingException ex){throw ex;
  }catch(IOException|RuntimeException ex){throw error(HttpStatus.UNPROCESSABLE_ENTITY,"CV_PDF_CORRUPTED",
   "PDF file is corrupted or unreadable",ex);}
 }
 private ExtractedCvDocument extractDocx(byte[] bytes){
  inspectDocxPackage(bytes);
  try{
   ZipSecureFile.setMinInflateRatio(0.01);ZipSecureFile.setMaxEntrySize(10L*1024*1024);
   try(XWPFDocument document=new XWPFDocument(new ByteArrayInputStream(bytes));
       XWPFWordExtractor extractor=new XWPFWordExtractor(document)){
    String text=checkedText(extractor.getText());
    if(text.length()<20)throw error(HttpStatus.UNPROCESSABLE_ENTITY,"CV_DOCX_NO_TEXT",
     "DOCX has no extractable text");
    return new ExtractedCvDocument(CvDocumentType.DOCX,text,null);
   }
  }catch(CvParsingException ex){throw ex;
  }catch(IOException|RuntimeException ex){throw error(HttpStatus.UNPROCESSABLE_ENTITY,"CV_DOCX_CORRUPTED",
   "DOCX file is corrupted or unreadable",ex);}
 }
 private void inspectDocxPackage(byte[] bytes){
  boolean contentTypes=false,document=false,macro=false;long expanded=0;int entries=0;
  try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(bytes))){
   ZipEntry entry;
   while((entry=zip.getNextEntry())!=null){
    if(++entries>10_000)throw unsupported("DOCX package contains too many entries");
    String name=entry.getName();
    if("[Content_Types].xml".equals(name))contentTypes=true;
    if("word/document.xml".equals(name))document=true;
    if(name.toLowerCase(Locale.ROOT).endsWith("vbaproject.bin"))macro=true;
    ByteArrayOutputStream sink=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int read;
    while((read=zip.read(buffer))!=-1){expanded+=read;if(expanded>MAX_DOCX_UNCOMPRESSED_BYTES)
      throw unsupported("DOCX package expands beyond the processing limit");
     if("[Content_Types].xml".equals(name)&&sink.size()<1_000_000)sink.write(buffer,0,read);
    }
    if("[Content_Types].xml".equals(name)){
     String types=sink.toString(StandardCharsets.UTF_8);
     if(types.contains("macroEnabled"))macro=true;
    }
   }
  }catch(CvParsingException ex){throw ex;
  }catch(IOException ex){throw error(HttpStatus.UNPROCESSABLE_ENTITY,"CV_DOCX_CORRUPTED",
   "DOCX file is corrupted or unreadable",ex);}
  if(!contentTypes||!document)throw unsupported("ZIP content is not a valid DOCX document");
  if(macro)throw error(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"CV_DOCX_MACROS_NOT_ALLOWED",
   "Macro-enabled Word documents are not supported");
 }
 private String checkedText(String value){
  String text=normalizer.normalize(value);
  if(text.length()>properties.maxExtractedCharacters())throw error(HttpStatus.PAYLOAD_TOO_LARGE,
   "CV_TEXT_TOO_LARGE","Extracted CV text exceeds the processing limit");
  return text;
 }
 private CvParsingException unsupported(String message){return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
  "CV_FORMAT_UNSUPPORTED",message);}
 private CvParsingException error(HttpStatus status,String code,String message){
  return new CvParsingException(status,code,message);}
 private CvParsingException error(HttpStatus status,String code,String message,Throwable cause){
  return new CvParsingException(status,code,message,cause);}
 private boolean startsWith(byte[] value,byte[] prefix){if(value.length<prefix.length)return false;
  for(int i=0;i<prefix.length;i++)if(value[i]!=prefix[i])return false;return true;}
}
