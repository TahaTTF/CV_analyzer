package com.cvanalyzer.backend.enrichment;

import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class DescriptionCleaningService {
 public String clean(String value){
  if(value==null)return null;
  if(value.isBlank())return "";
  Document document=Jsoup.parseBodyFragment(value);
  document.select("script,style,noscript,template").remove();
  document.outputSettings(new Document.OutputSettings().prettyPrint(false));
  for(Element element:document.select("br"))element.after("\n");
  for(Element element:document.select("li")){element.before("\n- ");element.after("\n");}
  for(Element element:document.select("p,div,section,article,h1,h2,h3,h4,h5,h6,ul,ol,table,tr"))
   element.after("\n");
  String text=document.body().wholeText().replace('\u00a0',' ')
   .replace("\r\n","\n").replace('\r','\n');
  List<String> lines=new ArrayList<>();
  boolean blank=false;
  for(String raw:text.split("\n",-1)){
   String line=raw.replaceAll("[\\t\\x0B\\f ]+"," ").strip();
   if(line.isEmpty()){
    if(!blank&&!lines.isEmpty())lines.add("");
    blank=true;
   }else{lines.add(line);blank=false;}
  }
  while(!lines.isEmpty()&&lines.getLast().isEmpty())lines.removeLast();
  return String.join("\n",lines);
 }
}
