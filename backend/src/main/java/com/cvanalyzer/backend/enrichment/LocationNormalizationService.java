package com.cvanalyzer.backend.enrichment;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import com.cvanalyzer.backend.model.JobDocument;

@Component
public class LocationNormalizationService {
 private static final Pattern FRENCH_POSTAL=Pattern.compile("\\b\\d{5}\\s+([\\p{L}][\\p{L} '-]{1,60})");
 private static final Pattern HYBRID=terms("hybrid","hybride","teletravail partiel","teilweise homeoffice");
 private static final Pattern REMOTE=terms("remote","fully remote","teletravail","homeoffice","fernarbeit");
 private static final Pattern ON_SITE=terms("on site","onsite","presentiel","vor ort");

 public JobDocument.NormalizedLocation normalize(JobDocument job,String descriptionText){
  JobDocument.Location location=job.location();
  String original=location==null?null:blank(location.displayName());
  String city=location==null?null:blank(location.city());
  String country=location==null?null:normalizeCountry(blank(location.countryCode()));
  double confidence=city!=null||country!=null?1.0:0.0;
  if(original!=null){
   String region=null;
   if(country==null){
    String n=normalized(original);
    if(has(n,"france")){country="FR";confidence=Math.max(confidence,0.9);}
    else if(has(n,"germany")||has(n,"deutschland")){country="DE";confidence=Math.max(confidence,0.9);}
    else if(has(n,"united kingdom")||has(n,"uk")){country="GB";confidence=Math.max(confidence,0.9);}
   }
   if(city==null){
    Matcher postal=FRENCH_POSTAL.matcher(original);
    if(postal.find()&&(country==null||"FR".equals(country))){
     city=cleanCity(postal.group(1));country="FR";confidence=Math.max(confidence,0.9);
    }else if(original.contains(",")){
     String[] parts=original.split(",");
     city=blank(parts[0]);
     if(parts.length>=2&&!isCountryLabel(parts[1]))region=blank(parts[1]);
     confidence=Math.max(confidence,country==null?0.65:0.8);
    }else if(original.contains("/")&&containsWorkMode(original)){
     city=blank(original.substring(0,original.indexOf('/')));confidence=Math.max(confidence,0.65);
    }else if(isSimpleCity(original)){
     city=original;confidence=Math.max(confidence,0.6);
    }
   }
   if(original!=null)return new JobDocument.NormalizedLocation(original,city,region,country,
    workMode(String.join(" ",safe(original),safe(job.title()),safe(descriptionText)),location),confidence);
  }
  String workText=String.join(" ",safe(original),safe(job.title()),safe(descriptionText));
  JobDocument.WorkMode workMode=workMode(workText,location);
  if(original==null&&city==null&&country==null&&workMode==JobDocument.WorkMode.UNKNOWN)return
   new JobDocument.NormalizedLocation(null,null,null,null,workMode,0.0);
  return new JobDocument.NormalizedLocation(original,city,null,country,workMode,confidence);
 }
 private boolean containsWorkMode(String value){String n=normalized(value);return HYBRID.matcher(n).find()
  ||REMOTE.matcher(n).find()||ON_SITE.matcher(n).find();}
 private boolean isSimpleCity(String value){String n=normalized(value).strip();return n.matches("[\\p{L} .'-]{2,60}")
  &&!isCountryLabel(n)&&!containsWorkMode(n)&&!n.equals("undefined")&&!n.equals("anywhere");}
 private boolean isCountryLabel(String value){String n=normalized(value).strip();return n.equals("france")
  ||n.equals("germany")||n.equals("deutschland")||n.equals("united kingdom")||n.equals("uk")
  ||n.equals("switzerland")||n.equals("schweiz")||n.equals("suisse")||n.equals("netherlands");}
 private JobDocument.WorkMode workMode(String value,JobDocument.Location location){
  String normalized=normalized(value);
  if(HYBRID.matcher(normalized).find())return JobDocument.WorkMode.HYBRID;
  if(location!=null&&location.remote())return JobDocument.WorkMode.REMOTE;
  if(REMOTE.matcher(normalized).find())return JobDocument.WorkMode.REMOTE;
  if(ON_SITE.matcher(normalized).find())return JobDocument.WorkMode.ON_SITE;
  return JobDocument.WorkMode.UNKNOWN;
 }
 private static Pattern terms(String...values){
  String joined=java.util.Arrays.stream(values).map(LocationNormalizationService::normalized)
   .map(Pattern::quote).map(v->"(?<![\\p{L}\\p{N}])"+v+"(?![\\p{L}\\p{N}])")
   .collect(java.util.stream.Collectors.joining("|"));
  return Pattern.compile(joined,Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE);
 }
 private static String normalized(String value){return Normalizer.normalize(safe(value),Normalizer.Form.NFD)
  .replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT).replace('-',' ');}
 private static boolean has(String value,String term){return Pattern.compile("(?<![\\p{L}\\p{N}])"+
  Pattern.quote(term)+"(?![\\p{L}\\p{N}])").matcher(value).find();}
 private static String cleanCity(String value){return blank(value.replaceAll("[,;].*$","").strip());}
 private static String blank(String value){return value==null||value.isBlank()?null:value.strip();}
 private static String upper(String value){return value==null?null:value.toUpperCase(Locale.ROOT);}
 private static String normalizeCountry(String value){
  if(value==null)return null;
  return switch(normalized(value).strip()){
   case "fr","france"->"FR";
   case "de","germany","deutschland"->"DE";
   case "gb","uk","united kingdom"->"GB";
   case "ch","switzerland","schweiz","suisse"->"CH";
   case "nl","netherlands","nederland"->"NL";
   case "at","austria","osterreich"->"AT";
   default->value.length()==2?upper(value):null;
  };
 }
 private static String safe(String value){return value==null?"":value;}
}
