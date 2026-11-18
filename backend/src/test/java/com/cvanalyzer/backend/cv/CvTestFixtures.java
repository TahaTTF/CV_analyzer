package com.cvanalyzer.backend.cv;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

final class CvTestFixtures {
 private CvTestFixtures(){}
 static byte[] pdf(String...lines) throws IOException {
  try(PDDocument document=new PDDocument();ByteArrayOutputStream output=new ByteArrayOutputStream()){
   PDPage page=new PDPage();document.addPage(page);
   try(PDPageContentStream content=new PDPageContentStream(document,page)){
    content.beginText();content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),11);
    content.newLineAtOffset(50,740);
    for(String line:lines){content.showText(ascii(line));content.newLineAtOffset(0,-16);}
    content.endText();
   }
   document.save(output);return output.toByteArray();
  }
 }
 static byte[] positionedPdf(List<List<PlacedText>> pages,boolean withPhoto) throws IOException {
  try(PDDocument document=new PDDocument();ByteArrayOutputStream output=new ByteArrayOutputStream()){
   for(List<PlacedText> values:pages){PDPage page=new PDPage();document.addPage(page);
    try(PDPageContentStream content=new PDPageContentStream(document,page)){
     if(withPhoto){BufferedImage image=new BufferedImage(40,40,BufferedImage.TYPE_INT_RGB);
      var graphics=image.createGraphics();graphics.setColor(Color.GRAY);graphics.fillOval(0,0,40,40);graphics.dispose();
      content.drawImage(LosslessFactory.createFromImage(document,image),520,720,40,40);}
     for(PlacedText value:values){content.beginText();content.setNonStrokingColor(value.color());
      content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),value.size());
      content.newLineAtOffset(value.x(),value.y());content.showText(value.text());content.endText();}
    }
   }
   document.save(output);return output.toByteArray();
  }
 }
 static byte[] encryptedPdf() throws IOException {
  try(PDDocument document=new PDDocument();ByteArrayOutputStream output=new ByteArrayOutputStream()){
   document.addPage(new PDPage());
   var policy=new StandardProtectionPolicy("owner-password","user-password",new AccessPermission());
   policy.setEncryptionKeyLength(128);document.protect(policy);document.save(output);return output.toByteArray();
  }
 }
 static byte[] imageOnlyPdf() throws IOException {
  try(PDDocument document=new PDDocument();ByteArrayOutputStream output=new ByteArrayOutputStream()){
   PDPage page=new PDPage();document.addPage(page);BufferedImage image=new BufferedImage(20,20,BufferedImage.TYPE_INT_RGB);
   var graphics=image.createGraphics();graphics.setColor(Color.BLACK);graphics.fillRect(0,0,20,20);graphics.dispose();
   try(PDPageContentStream content=new PDPageContentStream(document,page)){
    content.drawImage(LosslessFactory.createFromImage(document,image),50,700,20,20);
   }
   document.save(output);return output.toByteArray();
  }
 }
 static byte[] docx(String...paragraphs) throws IOException {
  try(XWPFDocument document=new XWPFDocument();ByteArrayOutputStream output=new ByteArrayOutputStream()){
   for(String value:paragraphs)document.createParagraph().createRun().setText(value);
   document.write(output);return output.toByteArray();
  }
 }
 static byte[] docxWithMacro() throws IOException {
  byte[] original=docx("Macro bearing CV content");
  try(ZipInputStream input=new ZipInputStream(new ByteArrayInputStream(original));
      ByteArrayOutputStream bytes=new ByteArrayOutputStream();ZipOutputStream output=new ZipOutputStream(bytes)){
   ZipEntry entry;
   while((entry=input.getNextEntry())!=null){output.putNextEntry(new ZipEntry(entry.getName()));input.transferTo(output);
    output.closeEntry();}
   output.putNextEntry(new ZipEntry("word/vbaProject.bin"));output.write(new byte[]{1,2,3});output.closeEntry();
   output.finish();return bytes.toByteArray();
  }
 }
 private static String ascii(String value){return value.replace('–','-').replace('—','-');}
 record PlacedText(float x,float y,float size,String text,Color color){
  PlacedText(float x,float y,String text){this(x,y,11,text,Color.BLACK);}
 }
}
