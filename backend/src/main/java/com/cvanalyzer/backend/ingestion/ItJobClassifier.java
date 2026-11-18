package com.cvanalyzer.backend.ingestion;

import java.text.Normalizer;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import com.cvanalyzer.backend.model.JobDocument;

@Component
public class ItJobClassifier {
    public static final String VERSION="it-rules-2.0";

    private static final List<Rule> STRONG_TITLE_RULES=List.of(
        rule("software-development","software developer","software engineer","softwareentwickler",
            "anwendungsentwickler","web developer","webentwickler","frontend developer",
            "front end developer","backend developer","back end developer","full stack developer",
            "fullstack developer","full stack","ingenieur logiciel","ingenieure logiciel",
            "developer","developpeur","developpeuse","programmierer","tech lead","lead tech",
            "technical lead"),
        rule("data-ai","data engineer","data scientist","machine learning engineer",
            "machine learning engineering","machine learning","data analyst","ai engineer",
            "ai deployment","ingenieur ia","ingenieure ia","projet ia","qualite ia","ki entwickler",
            "ingenieur data","ingenieure data","dateningenieur","datenwissenschaftler",
            "business intelligence developer"),
        rule("cybersecurity","cybersecurity","cyber security","cybersecurite",
            "securite informatique","it sicherheit","informationssicherheit"),
        rule("cloud-devops","devops","site reliability engineer","cloud engineer",
            "ingenieur cloud","cloud architekt","platform engineer","sysops"),
        rule("systems-networks","system administrator","systems administrator","network engineer",
            "network administrator","administrateur systeme","administratrice systeme",
            "administrateur reseau","administratrice reseau","ingenieur reseau",
            "ingenieure reseau","systemadministrator","netzwerkadministrator"),
        rule("mobile","mobile developer","android developer","ios developer",
            "developpeur mobile","developpeuse mobile","mobile entwickler"),
        rule("it-support","it support","help desk technician","service desk technician",
            "support informatique","technicien informatique","technicienne informatique",
            "fachinformatiker systemintegration"),
        rule("qa-it","qa engineer","test automation engineer","software tester",
            "ingenieur test logiciel","ingenieure test logiciel","softwaretester"),
        rule("embedded-iot","embedded software engineer","firmware engineer","iot engineer",
            "ingenieur logiciel embarque","ingenieure logiciel embarque","embedded entwickler"),
        rule("computer-vision","computer vision engineer","ingenieur vision par ordinateur"),
        rule("architecture","software architect","cloud architect","solutions architect",
            "architecte logiciel","architecte informatique","softwarearchitekt"),
        rule("database","database administrator","data base administrator",
            "administrateur base de donnees","administratrice base de donnees",
            "datenbankadministrator","dba postgresql","dba oracle"),
        rule("enterprise-applications","consultant sap","consultante sap","consultant fonctionnel sap",
            "consultante fonctionnelle sap","consultant technico fonctionnel sap","erp consultant",
            "consultant erp","consultante erp","expert cft","cross file transfer"),
        rule("it-role","it consultant","it project manager","chef de projet informatique",
            "consultant informatique","consultante informatique","ingenieur informatique",
            "ingenieure informatique","business analyst informatique","business analyste informatique",
            "systemes infrastructures",
            "fachinformatiker"));

    private static final List<Rule> STRONG_STRUCTURED_RULES=List.of(
        rule("software-development","software development","software engineering",
            "developpement logiciel","developpement web","softwareentwicklung","programmation"),
        rule("data-ai","machine learning","artificial intelligence","intelligence artificielle"),
        rule("cybersecurity","cybersecurity","cyber security","cybersecurite",
            "securite informatique","it sicherheit"),
        rule("cloud-devops","devops","site reliability engineering","cloud engineering"),
        rule("systems-networks","network administration","administration systeme",
            "administration reseau","systemadministration","netzwerkadministration"));

    private static final Rule BROAD_IT_CATEGORY=rule("it-category","it jobs","emplois informatique",
        "information technology","informatique","digital tech");

    private static final Pattern NON_IT_TITLE=phrasePattern(List.of(
        "livreur de journaux","distributeur de journaux","vendeur colporteur de presse",
        "newspaper delivery","conseiller multimarques","conseillere multimarques",
        "conseiller de vente","conseillere de vente","sales advisor","sales engineer",
        "business developer","property developer","expert en gestion d entreprise",
        "radioprotection","charge d etudes en electricite","chargee d etudes en electricite",
        "electrical engineer","electricien","electricienne","chauffeur","truck driver",
        "comptable","accountant","cuisinier","cuisiniere","cook","pflegefachkraft",
        "infirmier","infirmiere","nurse","recruteur","recruteuse","recruiter"));

    private static final Pattern TECHNICAL_CONTEXT=phrasePattern(List.of(
        "software development","application development","web development","developpement logiciel",
        "developpement informatique","developpement web","entwicklung von software","source code",
        "code review","revue de code","frontend","backend","microservices","rest api",
        "base de donnees","database","datenbank","cloud infrastructure","infrastructure cloud",
        "network administration","administration reseau","cybersecurity","cybersecurite"));

    private static final List<Pattern> TECHNICAL_TERMS=List.of(
        phrases("java"),phrases("kotlin"),phrases("c#"),phrases(".net"),phrases("python"),
        phrases("javascript"),phrases("typescript"),phrases("spring","spring boot"),
        phrases("angular"),phrases("react"),phrases("node.js","nodejs"),phrases("sql"),
        phrases("nosql"),phrases("mongodb"),phrases("postgresql"),phrases("docker"),
        phrases("kubernetes"),phrases("aws"),phrases("azure"),phrases("gcp"),phrases("git"),
        phrases("linux"),phrases("terraform"),phrases("ansible"),phrases("jenkins"),
        phrases("ci cd"));

    private final Clock clock;

    public ItJobClassifier(){this(Clock.systemUTC());}
    public ItJobClassifier(Clock clock){this.clock=clock;}

    public JobDocument.ItClassification classify(String title,String description,
            List<String> structuredTerms,List<String> romeCodes){
        String normalizedTitle=normalize(title);
        String normalizedDescription=normalize(description);
        String structured=normalize(String.join(" ",safe(structuredTerms)));
        Set<String> matches=new LinkedHashSet<>();

        STRONG_TITLE_RULES.stream().filter(r->r.pattern.matcher(normalizedTitle).find())
            .map(Rule::category).forEach(matches::add);
        boolean strongTitle=!matches.isEmpty();

        boolean broadCategory=BROAD_IT_CATEGORY.pattern.matcher(structured).find();
        if(broadCategory)matches.add(BROAD_IT_CATEGORY.category);

        int beforeStructured=matches.size();
        STRONG_STRUCTURED_RULES.stream().filter(r->r.pattern.matcher(structured).find())
            .map(Rule::category).forEach(matches::add);
        boolean strongStructured=matches.size()>beforeStructured;

        boolean romeMatch=safe(romeCodes).stream().filter(java.util.Objects::nonNull)
            .map(String::strip).map(String::toUpperCase).anyMatch(code->code.matches("M18\\d{2}"));
        if(romeMatch)matches.add("rome-it");

        boolean nonItTitle=NON_IT_TITLE.matcher(normalizedTitle).find();
        if(nonItTitle){
            matches.add("non-it-title");
            return result(false,matches,"Explicit non-IT occupation in the title overrides weak provider metadata");
        }
        if(strongTitle)
            return result(true,matches,"Explicit IT role in the job title");
        if(romeMatch)
            return result(true,matches,"Authoritative M18xx IT occupation code");
        if(strongStructured)
            return result(true,matches,"Specific IT role in structured provider metadata");

        int structuredSkills=countTechnicalTerms(structured);
        int descriptionSkills=countTechnicalTerms(normalizedDescription);
        boolean technicalContext=TECHNICAL_CONTEXT.matcher(normalizedDescription).find();
        if(structuredSkills>=2||(technicalContext&&descriptionSkills>=2)){
            matches.add("technical-skills");
            if(technicalContext)matches.add("technical-context");
            return result(true,matches,"Multiple technical skills corroborated by structured data or IT work context");
        }

        if(broadCategory||structuredSkills>0||descriptionSkills>0||technicalContext){
            if(structuredSkills>0||descriptionSkills>0)matches.add("technical-hint");
            if(technicalContext)matches.add("technical-context");
            return result(null,matches,"IT evidence is present but too weak or too generic for a deterministic decision");
        }
        return result(false,matches,"No specific IT role, occupation code, or corroborated technical evidence");
    }

    private JobDocument.ItClassification result(Boolean relevant,Set<String> categories,String reason){
        return new JobDocument.ItClassification(relevant,List.copyOf(categories),reason,VERSION,clock.instant());
    }
    private static int countTechnicalTerms(String value){
        int count=0;
        for(Pattern pattern:TECHNICAL_TERMS)if(pattern.matcher(value).find())count++;
        return count;
    }
    private static Rule rule(String category,String... phrases){
        return new Rule(category,phrasePattern(List.of(phrases)));
    }
    private static Pattern phrases(String... values){return phrasePattern(List.of(values));}
    private static Pattern phrasePattern(List<String> phrases){
        List<String> escaped=new ArrayList<>();
        for(String phrase:phrases)escaped.add("(?<![\\p{L}\\p{N}])"+Pattern.quote(normalize(phrase))
            +"(?![\\p{L}\\p{N}])");
        return Pattern.compile(String.join("|",escaped),Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE);
    }
    private static String normalize(String value){
        if(value==null)return "";
        return Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}+","")
            .toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}+#.]+"," ").trim();
    }
    private static List<String> safe(List<String> values){return values==null?List.of():values;}
    private record Rule(String category,Pattern pattern){}
}
