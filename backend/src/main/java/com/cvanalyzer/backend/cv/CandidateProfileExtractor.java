package com.cvanalyzer.backend.cv;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import com.cvanalyzer.backend.enrichment.TechnicalSkillExtractor;
import com.cvanalyzer.backend.model.JobDocument;

@Service
public class CandidateProfileExtractor {
 private static final String MONTH="jan(?:uary|vier)?|feb(?:ruary)?|fev(?:rier)?|mar(?:ch|s)?|apr(?:il)?|avr(?:il)?|may|mai|jun(?:e)?|juin|jul(?:y)?|juil(?:let)?|aug(?:ust)?|aou?t|sep(?:t(?:ember|embre)?)?|oct(?:ober|obre)?|nov(?:ember|embre)?|dec(?:ember|embre)?";
 private static final Pattern ROLE=Pattern.compile("(?i)\\b(?:software engineer(?:ing)?|software developer|back[ -]?end developer|front[ -]?end developer|full[ -]?stack developer|devops engineer|data scientist|data engineer(?:ing)?|machine learning engineer|network engineer|cybersecurity analyst|it support|engineering student|computer science student|cycle ingenieur|eleve ingenieur(?:e)?|(?:developpeur|developpeuse) (?:back[ -]?end|front[ -]?end|full[ -]?stack|java|web)|ingenieur(?:e)? (?:logiciel|devops|data|donnees|informatique|reseau)|etudiant(?:e)? (?:en )?ingenierie|administrateur(?:rice)? (?:systeme|reseau))\\b");
 private static final Pattern EXPERIENCE_ROLE=Pattern.compile("(?i)\\b(?:developer|engineer|analyst|consultant|architect|administrator|intern(?:ship)?|trainee|developpeur|developpeuse|ingenieur|ingenieure|stage|stagiaire|alternant|alternante|technicien|technicienne)\\b");
 private static final Pattern EDUCATION_TITLE=Pattern.compile("(?i)\\b(?:diploma|degree|bachelor|master|engineering|engineer|preparatory|computer science|baccalaureate|diplome|licence|ingenieur|ingenierie|mastere|cycle preparatoire|bac(?:calaureat)?)\\b");
 private static final Pattern INSTITUTION=Pattern.compile("(?i)\\b(?:university|institute|school|college|faculty|academy|universite|institut|ecole|faculte|academie)\\b");
 private static final Pattern LANGUAGE_NAME=Pattern.compile("(?i)\\b(?:english|anglais|french|francais|arabic|arabe|german|allemand|spanish|espagnol|italian|italien|portuguese|portugais|dutch|neerlandais|turkish|turc)\\b");
 private static final Pattern CONTACT_OR_LINK=Pattern.compile("(?i)(?:https?://|www\\.|linkedin|github\\.com|mailto:|@)");
 private static final Pattern YEARS=Pattern.compile("(?i)\\b(\\d{1,2})\\s*(?:\\+\\s*)?(?:years?(?: of)? experience|ans? d experience)\\b");
 private static final Pattern PERIOD=Pattern.compile("(?i)(?:\\b\\d{1,2}/(?:19|20)\\d{2}\\b\\s*[-–—]\\s*\\b\\d{1,2}/(?:19|20)\\d{2}\\b|\\b(?:"+MONTH+")\\b\\s+(?:19|20)\\d{2}\\s*[-–—]\\s*(?:\\b(?:"+MONTH+")\\b\\s+)?(?:\\b(?:19|20)\\d{2}\\b|present|current|maintenant|aujourd hui)|\\b(?:"+MONTH+")\\b\\s*[-–—]\\s*\\b(?:"+MONTH+")\\b\\s+(?:19|20)\\d{2}\\b|\\b(?:19|20)\\d{2}\\b\\s*[-–—]\\s*(?:\\b(?:19|20)\\d{2}\\b|present|current|maintenant|aujourd hui)|\\b\\d{1,2}/(?:19|20)\\d{2}\\b|\\b(?:"+MONTH+")\\b\\s+(?:19|20)\\d{2}\\b|\\b(?:19|20)\\d{2}\\b)");
 private static final Pattern PROJECT_ACTION=Pattern.compile("(?i)^(?:built|created|developed|designed|implemented|integrated|deployed|trained|used|analyzed|engineered|construction|creation|developpement|conception|implementation|integration|realisation|mise en place)\\b");
 private static final Pattern TECHNOLOGY_LINE=Pattern.compile("(?i)^(?:technologies?|tech stack|tools?|outils?|stack)\\s*[:|-]");
 private final TechnicalSkillExtractor skills;
 private final PersonalDataRedactor redactor;
 public CandidateProfileExtractor(TechnicalSkillExtractor skills,PersonalDataRedactor redactor){
  this.skills=skills;this.redactor=redactor;
 }
 public CandidateProfile extract(String text){
  Map<Section,List<String>> sections=sections(text);
  String target=targetRole(sections);
  String summary=joined(sections.get(Section.SUMMARY),2_500);
  if(summary==null)summary=generalSummary(sections.get(Section.GENERAL),target);
  summary=redactor.redact(summary);
  List<String> technical=skills.extractFromText(target,text).stream()
   .map(JobDocument.ExtractedSkill::name).toList();
  List<CandidateProfile.ExperienceEntry> experience=experience(sections.get(Section.EXPERIENCE));
  List<CandidateProfile.ProjectEntry> projects=projects(sections.get(Section.PROJECTS),
   sections.get(Section.EXPERIENCE));
  List<CandidateProfile.EducationEntry> education=education(sections.get(Section.EDUCATION));
  List<String> languages=languages(sections.get(Section.LANGUAGES));
  List<CandidateProfile.AdditionalSection> additional=additionalSections(sections);
  Integer years=years(joined(sections.get(Section.EXPERIENCE),20_000)+"\n"+safe(summary));
  return new CandidateProfile(target,summary,technical,years,experience,projects,education,languages,additional);
 }
 private Map<Section,List<String>> sections(String text){
  Map<Section,List<String>> values=new EnumMap<>(Section.class);
  for(Section section:Section.values())values.put(section,new ArrayList<>());
  Section current=Section.GENERAL;
  for(String line:text.split("\n",-1)){
   Header header=header(line);
   if(header==null&&current==Section.EXPERIENCE&&isEmbeddedProjectStart(line)){
    current=Section.PROJECTS;values.get(current).add(line);continue;}
   if(header!=null&&current==Section.PROJECTS&&header.section==Section.SKILLS&&!header.remainder.isBlank())
    values.get(current).add(line);
   else if(header!=null){current=header.section;if(!header.remainder.isBlank())values.get(current).add(header.remainder);}
   else values.get(current).add(line);
  }
  return values;
 }
 private Header header(String line){
  String value=contentText(line).replaceAll("[:|]+$","").strip();
  String compact=value.replaceAll("[^\\p{L}\\p{N}]","");
  if(compact.equals("experienceprofessionnelle")||compact.equals("professionalexperience"))
   return new Header(Section.EXPERIENCE,"");
  if(compact.equals("formationacademique"))return new Header(Section.EDUCATION,"");
  if(value.matches(".*\\b(?:projects|projets)(?: academiques)?$"))return new Header(Section.PROJECTS,"");
  if(value.startsWith("leadership & certifications")||value.startsWith("certifications "))
   return new Header(Section.CERTIFICATIONS,"");
  if(value.startsWith("leadership & awards"))return new Header(Section.ACTIVITIES,"");
  if(value.startsWith("languages & interests")||value.startsWith("langues & interets"))
   return new Header(Section.LANGUAGES,"");
  for(Section section:Section.values())for(String name:section.headers){
   if(value.equals(name))return new Header(section,"");
   if(value.matches(Pattern.quote(name)+"\\s*:.*"))
    return new Header(section,line.substring(line.indexOf(':')+1).strip());
  }
  return null;
 }
 private boolean isEmbeddedProjectStart(String line){String value=contentText(line);return line.length()<=180
  &&value.matches("^(?:personal|academic) project\\b.*|^projet (?:personnel|academique)\\b.*");}
 private String targetRole(Map<Section,List<String>> sections){
  String explicit=firstMeaningful(sections.get(Section.TARGET));
  if(explicit!=null)return redactor.redact(explicit);
  List<String> general=new ArrayList<>(sections.get(Section.GENERAL));
  general.addAll(sections.get(Section.SUMMARY));int inspected=0;
  for(int i=0;i<general.size();i++){
   String line=general.get(i);if(line.isBlank())continue;if(++inspected>20)break;
   String candidate=line.strip();String normalized=normalized(candidate);
   if(normalized.contains("specializ")&&i+1<general.size()){
    String next=nextMeaningful(general,i+1);
    if(next!=null&&header(next)==null&&candidate.length()+next.length()<240)candidate=candidate+" "+next;
   }
   if(candidate.length()<=240&&ROLE.matcher(normalized(candidate)).find())return redactor.redact(candidate);
  }
  return null;
 }
 private List<CandidateProfile.ExperienceEntry> experience(List<String> lines){
  List<CandidateProfile.ExperienceEntry> result=new ArrayList<>();
  for(List<String> block:entryBlocks(lines,EntryKind.EXPERIENCE)){
   List<String> content=block.stream().filter(v->!v.isBlank()).toList();if(content.isEmpty())continue;
   PeriodEvidence dated=period(content);String period=dated==null?null:dated.value();
   String heading=content.stream().filter(this::isExperienceHeading).findFirst().orElse(null);
   if(heading==null)continue;String headingWithoutDate=dated!=null&&heading.equals(dated.line())
    ?heading.replace(dated.value()," ").replaceAll("\\s+"," ").strip():heading;
   HeadingParts parts=experienceHeading(headingWithoutDate);String role=parts.role();String organization=parts.organization();
   if(organization==null)organization=content.stream().filter(v->!v.equals(heading)&&!v.equals(period))
    .filter(v->dated==null||!v.equals(dated.line()))
    .filter(this::isOrganizationCandidate).findFirst().orElse(null);
   List<String> description=new ArrayList<>(content);description.remove(heading);
   if(organization!=null)description.remove(organization);if(dated!=null)description.remove(dated.line());
   result.add(new CandidateProfile.ExperienceEntry(clean(role),clean(organization),clean(period),
    redactor.redact(String.join(" ",description))));
  }
  List<CandidateProfile.ExperienceEntry> consolidated=new ArrayList<>();
  for(CandidateProfile.ExperienceEntry entry:result){
   if(!consolidated.isEmpty()&&entry.organization()==null&&entry.period()!=null&&entry.description()==null
     &&entry.role()!=null&&entry.role().split("\\s+").length<=2){
    CandidateProfile.ExperienceEntry previous=consolidated.getLast();
    if(previous.organization()!=null&&previous.period()==null){consolidated.set(consolidated.size()-1,
      new CandidateProfile.ExperienceEntry(previous.role(),previous.organization(),entry.period(),previous.description()));continue;}}
   if(entry.organization()==null&&entry.period()==null&&entry.description()==null)continue;
   consolidated.add(entry);
  }
  return consolidated;
 }
 private List<CandidateProfile.EducationEntry> education(List<String> lines){
  List<CandidateProfile.EducationEntry> result=new ArrayList<>();
  for(List<String> block:entryBlocks(lines,EntryKind.EDUCATION)){
   List<String> content=block.stream().filter(v->!v.isBlank()).toList();if(content.isEmpty())continue;
   PeriodEvidence dated=period(content);String period=dated==null?null:dated.value();
   String qualificationLine=content.stream().filter(this::isEducationHeading).findFirst().orElse(null);
   EducationParts parts=educationHeading(qualificationLine,content,dated);
   String qualification=parts.qualification();String institution=parts.institution();
   if(qualification==null&&institution==null)continue;
   List<String> description=new ArrayList<>(content);description.remove(qualificationLine);
   if(institution!=null)description.remove(institution);if(dated!=null)description.remove(dated.line());
   result.add(new CandidateProfile.EducationEntry(clean(qualification),clean(institution),clean(period),
    redactor.redact(String.join(" ",description))));
  }
  return result;
 }
 private List<CandidateProfile.ProjectEntry> projects(List<String> lines,List<String> experienceLines){
  List<List<String>> blocks=new ArrayList<>();List<String> current=new ArrayList<>();
  for(String raw:lines==null?List.<String>of():lines){String line=raw.strip();
   if(line.isBlank()){finish(blocks,current);current=new ArrayList<>();continue;}
   if(isProjectHeading(line)&&!current.isEmpty()){finish(blocks,current);current=new ArrayList<>();}
   current.add(line);
  }
  finish(blocks,current);List<ProjectBlock> candidates=new ArrayList<>();
  blocks.forEach(v->candidates.add(new ProjectBlock(v,false)));
  for(List<String> block:entryBlocks(experienceLines,EntryKind.EXPERIENCE))
   if(!block.isEmpty()&&block.stream().noneMatch(this::isExperienceHeading)&&isProjectHeading(block.getFirst()))
    candidates.add(new ProjectBlock(block,true));
  Map<String,CandidateProfile.ProjectEntry> result=new java.util.LinkedHashMap<>();
  for(ProjectBlock candidate:candidates){List<String> block=candidate.lines();
   if(block.size()<2)continue;String name=projectName(block.getFirst());
   if(name==null||!isProjectHeading(block.getFirst()))continue;
   String all=String.join("\n",block);
   List<String> technologies=skills.extractFromText(name,all).stream()
    .map(JobDocument.ExtractedSkill::name).toList();
   PeriodEvidence dated=period(block);String description=block.stream().skip(1)
    .filter(v->dated==null||!v.equals(dated.line())).filter(v->!TECHNOLOGY_LINE.matcher(contentText(v)).find())
    .collect(java.util.stream.Collectors.joining(" "));
   String key=normalized(name);result.putIfAbsent(key,new CandidateProfile.ProjectEntry(name,
    redactor.redact(description),technologies));
  }
  return List.copyOf(result.values());
 }
 private boolean isProjectHeading(String line){
  String value=projectName(line);if(value==null||value.length()>110||value.matches(".*[.!?]$"))return false;
  String normalized=contentText(value);if(isPeriod(value)||TECHNOLOGY_LINE.matcher(normalized).find()
    ||PROJECT_ACTION.matcher(normalized).find())return false;
  String[] words=value.split("\\s+");if(words.length>20)return false;int titleLike=0,letters=0;
  for(String word:words){String token=word.replaceAll("[^\\p{L}\\p{N}]","");if(token.isEmpty())continue;letters++;
   if(Character.isUpperCase(token.codePointAt(0))||token.equals(token.toUpperCase(Locale.ROOT)))titleLike++;}
  return letters>0&&(titleLike*2>=letters||value.matches(".*[a-z][A-Z].*"));
 }
 private String projectName(String line){String value=line.strip()
  .replaceFirst("(?i)^(?:(?:personal|academic) project|projet (?:personnel|academique)|project|projet)\\s*[:|–—-]\\s*","");
  value=redactor.redact(value);return clean(value==null?null:value.replaceAll("[|·•\\s]+$","").strip());}
 private String contentText(String value){return normalized(value).replaceFirst("^[^\\p{L}\\p{N}]+","").strip();}
 private List<List<String>> entryBlocks(List<String> lines,EntryKind kind){
  if(lines==null)return List.of();List<List<String>> result=new ArrayList<>();List<String> current=new ArrayList<>();
  for(String raw:lines){String line=raw.strip();
   if(line.isBlank()){if(hasPeriod(current)){finish(result,current);current=new ArrayList<>();}continue;}
   boolean begins=kind==EntryKind.EXPERIENCE?(isRoleBoundary(line)||isProjectHeading(line)):isEducationHeading(line);
   boolean nextRole=kind==EntryKind.EXPERIENCE&&isRoleBoundary(line)&&current.stream().anyMatch(this::isRoleBoundary);
   if(!current.isEmpty()&&((hasPeriod(current)&&(begins||isPeriod(line)))||nextRole)){
    finish(result,current);current=new ArrayList<>();}
   current.add(line);
  }
  finish(result,current);return result;
 }
 private boolean isExperienceHeading(String line){String value=normalized(line),compact=value.replaceAll("[^a-z0-9]","");
  return line.length()<=180&&(EXPERIENCE_ROLE.matcher(value).find()||compact.startsWith("stagedete")
   ||compact.startsWith("stageprofessionnel"));}
 private boolean isRoleBoundary(String line){if(!isExperienceHeading(line)||line.matches(".*[.!?]$"))return false;
  String value=contentText(line);if(PROJECT_ACTION.matcher(value).find())return false;
  String[] words=line.split("\\s+");if(words.length>16)return false;int titleLike=0,letters=0;
  for(String word:words){String token=word.replaceAll("[^\\p{L}\\p{N}]","");if(token.isEmpty())continue;letters++;
   if(Character.isUpperCase(token.codePointAt(0))||token.equals(token.toUpperCase(Locale.ROOT)))titleLike++;}
  return letters>0&&titleLike*2>=letters;}
 private boolean isEducationHeading(String line){String value=normalized(line),compact=value.replaceAll("[^a-z0-9]","");
  return line.length()<=200&&(EDUCATION_TITLE.matcher(value).find()
   ||compact.matches(".*(?:diplome|ingenieur|baccalaureat|cyclepreparatoire|licence|master).*"));}
 private boolean isOrganizationCandidate(String line){String value=contentText(line);return line.length()<=140
  &&!CONTACT_OR_LINK.matcher(line).find()&&!isPeriod(line)&&!isExperienceHeading(line)
  &&!PROJECT_ACTION.matcher(value).find()&&!line.matches(".*[.!?]$");}
 private boolean isPeriod(String line){return period(line)!=null;}
 private boolean hasPeriod(List<String> lines){return lines.stream().anyMatch(this::isPeriod);}
 private PeriodEvidence period(List<String> lines){for(String line:lines){String value=period(line);
   if(value!=null)return new PeriodEvidence(line,value);}return null;}
 private String period(String line){String original=line.strip(),value=normalized(original);Matcher matcher=PERIOD.matcher(value);
  if(!matcher.find())return null;return value.length()==original.length()
   ?original.substring(matcher.start(),matcher.end()).strip():matcher.group().strip();}
 private HeadingParts experienceHeading(String heading){
  String[] segments=heading.split("\\s+(?:\\||[•·]|–|—)\\s+");int roleIndex=-1;
  for(int i=0;i<segments.length;i++)if(EXPERIENCE_ROLE.matcher(normalized(segments[i])).find()){roleIndex=i;break;}
  if(roleIndex>=0){String role=clean(segments[roleIndex]);String organization=null;
   String[] at=role.split("(?i)\\s+(?:at|chez)\\s+",2);if(at.length==2){role=clean(at[0]);organization=clean(at[1]);}
   int comma=role==null?-1:role.indexOf(',');if(organization==null&&comma>0){organization=clean(role.substring(comma+1));
    role=clean(role.substring(0,comma));}
   if(roleIndex+1<segments.length&&isOrganizationCandidate(segments[roleIndex+1]))organization=clean(segments[roleIndex+1]);
   return new HeadingParts(role,organization);}
  String[] at=heading.split("(?i)\\s+(?:at|chez)\\s+",2);
  if(at.length==2)return new HeadingParts(clean(at[0]),clean(at[1]));
  int comma=heading.indexOf(',');if(comma>0)return new HeadingParts(clean(heading.substring(0,comma)),
   clean(heading.substring(comma+1)));return new HeadingParts(clean(heading),null);
 }
 private EducationParts educationHeading(String qualificationLine,List<String> content,PeriodEvidence dated){
  String qualification=null,institution=null;
  if(qualificationLine!=null){String line=dated!=null&&qualificationLine.equals(dated.line())
    ?qualificationLine.replace(dated.value()," ").replaceAll("\\s+"," ").strip():qualificationLine;
   for(String segment:line.split("\\s+(?:\\||[•·]|–|—)\\s+")){
    if(qualification==null&&EDUCATION_TITLE.matcher(normalized(segment)).find())qualification=clean(segment);
    else if(institution==null&&INSTITUTION.matcher(normalized(segment)).find())institution=clean(segment);}
   if(qualification==null)qualification=clean(line);}
  if(institution==null)institution=content.stream().filter(v->!v.equals(qualificationLine))
   .filter(v->dated==null||!v.equals(dated.line())).filter(v->INSTITUTION.matcher(normalized(v)).find())
   .findFirst().orElse(null);
  return new EducationParts(qualification,institution);
 }
 private void finish(List<List<String>> result,List<String> current){if(!current.isEmpty())result.add(List.copyOf(current));}
 private List<String> languages(List<String> lines){
  if(lines==null)return List.of();Set<String> values=new LinkedHashSet<>();
  for(String line:lines){Matcher matcher=LANGUAGE_NAME.matcher(normalized(line));
   while(matcher.find())values.add(canonicalLanguage(matcher.group()));}
  return List.copyOf(values);
 }
 private String canonicalLanguage(String value){return switch(normalized(value)){
  case "english","anglais"->"English";case "french","francais"->"French";
  case "arabic","arabe"->"Arabic";case "german","allemand"->"German";
  case "spanish","espagnol"->"Spanish";case "italian","italien"->"Italian";
  case "portuguese","portugais"->"Portuguese";case "dutch","neerlandais"->"Dutch";
  case "turkish","turc"->"Turkish";default->value;};}
 private List<CandidateProfile.AdditionalSection> additionalSections(Map<Section,List<String>> sections){
  List<CandidateProfile.AdditionalSection> values=new ArrayList<>();
  addAdditional(values,"Certifications",sections.get(Section.CERTIFICATIONS));
  addAdditional(values,"Volunteering",sections.get(Section.VOLUNTEERING));
  addAdditional(values,"Extracurricular activities",sections.get(Section.ACTIVITIES));
  return List.copyOf(values);
 }
 private void addAdditional(List<CandidateProfile.AdditionalSection> values,String heading,List<String> lines){
  String text=redactor.redact(joined(lines,3_000));if(text!=null)values.add(new CandidateProfile.AdditionalSection(heading,text));
 }
 private Integer years(String text){Matcher matcher=YEARS.matcher(normalized(text));
  if(!matcher.find())return null;int value=Integer.parseInt(matcher.group(1));return value<=60?value:null;}
 private String generalSummary(List<String> lines,String target){if(lines==null)return null;List<String> paragraphs=new ArrayList<>();
  for(String line:lines){String value=line.strip();if(value.length()<120||value.equals(target)
    ||CONTACT_OR_LINK.matcher(value).find()||redactor.redact(value)==null)continue;paragraphs.add(value);}
  return joined(paragraphs,2_500);}
 private String joined(List<String> lines,int max){if(lines==null)return null;String value=String.join("\n",lines).strip();
  if(value.isBlank())return null;return value.length()>max?value.substring(0,max).strip():value;}
 private String firstMeaningful(List<String> values){if(values==null)return null;return values.stream()
  .map(String::strip).filter(v->!v.isBlank()).findFirst().orElse(null);}
 private String nextMeaningful(List<String> values,int start){for(int i=start;i<values.size();i++)
  if(!values.get(i).isBlank())return values.get(i).strip();return null;}
 private String clean(String value){return value==null||value.isBlank()?null:value.strip();}
 private static String normalized(String value){return Normalizer.normalize(safe(value),Normalizer.Form.NFD)
  .replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT).replace('’',' ').replaceAll("\\s+"," ").strip();}
 private static String safe(String value){return value==null?"":value;}
 private record Header(Section section,String remainder){}
 private record PeriodEvidence(String line,String value){}
 private record HeadingParts(String role,String organization){}
 private record EducationParts(String qualification,String institution){}
 private record ProjectBlock(List<String> lines,boolean inferred){}
 private enum EntryKind{EXPERIENCE,EDUCATION}
 private enum Section{
  GENERAL(),TARGET("target role","career objective","objectif professionnel","poste recherche","الهدف المهني"),
  SUMMARY("professional summary","summary","profile","profil","personal profile","resume professionnel","a propos"),
  SKILLS("technical skills","skills","competences techniques","competences","technologies","المهارات"),
  EXPERIENCE("work experience","professional experience","professional experiences","experience professionnelle","experiences professionnelles","internships","stages","experience","الخبرة"),
  PROJECTS("projects","personal projects","academic projects","personal and academic projects","projets","projets personnels","projets academiques"),
  EDUCATION("education","formation","formation academique","formations","diplomes","academic background","التعليم"),
  LANGUAGES("languages","language","spoken languages","langues","langue","اللغات"),
  CERTIFICATIONS("certifications","certification","certificates","certificats","certifications et langues"),
  VOLUNTEERING("volunteering","volunteer experience","volontariat","benevolat","experience benevole"),
  ACTIVITIES("extracurricular activities","extracurricular experience","activities","activites",
   "experience extra scolaire","experience extra-scolaire","experiences extra scolaires",
   "experiences extra-scolaires","vie associative","leadership/clubs experience","leadership",
   "interests","interets","centres d interet","centres d'interet");
  final List<String> headers;
  Section(String...headers){this.headers=List.of(headers);}
 }
}
