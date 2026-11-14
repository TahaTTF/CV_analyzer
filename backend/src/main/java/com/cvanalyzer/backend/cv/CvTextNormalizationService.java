package com.cvanalyzer.backend.cv;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CvTextNormalizationService {
 public String normalize(String value){
  if(value==null)return "";
  String text=Normalizer.normalize(value,Normalizer.Form.NFC)
   .replace("\r\n","\n").replace('\r','\n').replace('\u00a0',' ')
   .replaceAll("[\\p{Cc}&&[^\\n\\t]]","");
  List<String> lines=new ArrayList<>();boolean previousBlank=false;
  for(String raw:text.split("\n",-1)){
   String line=raw.replaceAll("[\\t ]+"," ").strip();
   if(line.isEmpty()){
    if(!previousBlank&&!lines.isEmpty())lines.add("");
    previousBlank=true;
   }else{lines.add(line);previousBlank=false;}
  }
  while(!lines.isEmpty()&&lines.getLast().isEmpty())lines.removeLast();
  return String.join("\n",lines);
 }
}
