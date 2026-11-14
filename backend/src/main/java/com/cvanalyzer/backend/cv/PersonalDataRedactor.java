package com.cvanalyzer.backend.cv;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class PersonalDataRedactor {
 private static final Pattern EMAIL=Pattern.compile("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");
 private static final Pattern PHONE=Pattern.compile("(?<!\\d)(?:\\+?\\d[\\d .()/-]{7,}\\d)(?!\\d)");
 private static final Pattern URL=Pattern.compile("(?i)\\b(?:https?://|www\\.|(?:[A-Z0-9-]+\\.)+"
  +"(?:com|org|net|io|dev|fr|de|tn)/)\\S+");
 private static final Pattern CONTACT_LINE=Pattern.compile("(?i)^(?:name|nom|email|e-mail|phone|telephone|tel|adresse|address|linkedin)\\s*[:|-]");
 public String redact(String value){
  if(value==null)return null;
  List<String> kept=new ArrayList<>();
  for(String line:value.split("\\n")){
   if(CONTACT_LINE.matcher(line.strip()).find())continue;
   String cleaned=EMAIL.matcher(line).replaceAll(" ");
   cleaned=PHONE.matcher(cleaned).replaceAll(" ");
   cleaned=URL.matcher(cleaned).replaceAll(" ").replaceAll("[ \\t]+"," ").strip();
   if(!cleaned.isEmpty())kept.add(cleaned);
  }
  String result=String.join("\n",kept).strip();
  return result.isEmpty()?null:result;
 }
}
