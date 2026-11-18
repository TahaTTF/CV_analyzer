package com.cvanalyzer.backend.cv;

import java.io.IOException;
import java.io.Writer;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

final class LayoutAwarePdfTextExtractor extends PDFTextStripper {
 private static final Set<String> HEADINGS=Set.of("profile","profil","professional summary","summary",
  "education","formation","formation academique","academic background","experience","work experience",
  "professional experience","experience professionnelle","experiences professionnelles","internships",
  "stages","projects","personal projects","academic projects","projets","projets academiques",
  "skills","technical skills","competences","competences techniques","languages","langues",
  "certifications","certificates","volunteering","volunteer experience","benevolat",
  "extracurricular activities","activities","experience extra scolaire","experiences extra scolaires");
 private final List<PageText> pages=new ArrayList<>();
 private PageText current;

 LayoutAwarePdfTextExtractor() throws IOException {setSortByPosition(false);setSuppressDuplicateOverlappingText(true);}

 String extract(PDDocument document) throws IOException {
  pages.clear();writeText(document,Writer.nullWriter());StringBuilder output=new StringBuilder();
  List<PageColumns> columns=pages.stream().map(PageText::columns).toList();
  boolean continuous=columns.size()>1&&columns.stream().allMatch(v->v!=null)
   &&columns.stream().mapToDouble(PageColumns::ratio).max().orElse(1)
     -columns.stream().mapToDouble(PageColumns::ratio).min().orElse(0)<.08
   &&columns.stream().allMatch(v->v.ratio()<=.45||v.ratio()>=.55);
  if(continuous){for(boolean right:new boolean[]{false,true})for(PageColumns page:columns)
    appendBlock(output,renderLines(right?page.right():page.left()));}
  else for(PageText page:pages)appendBlock(output,page.render());
  return output.toString();
 }
 private static void appendBlock(StringBuilder output,String value){if(value==null||value.isBlank())return;
  if(!output.isEmpty())output.append("\n\n");output.append(value);}
 private static String renderLines(List<Line> lines){StringBuilder value=new StringBuilder();lines.stream()
  .sorted(Line.ORDER).forEach(line->append(value,line.text));return value.toString().strip();}

 @Override protected void startPage(PDPage page) throws IOException {
  current=new PageText(page.getCropBox().getWidth());pages.add(current);super.startPage(page);
 }
 @Override protected void processTextPosition(TextPosition text){
  if(current==null||text.getUnicode()==null||text.getUnicode().isBlank())return;
  current.add(new Glyph(text.getUnicode(),text.getXDirAdj(),text.getYDirAdj(),
   Math.max(text.getWidthDirAdj(),0.1f),Math.max(text.getHeightDir(),1f),
   Math.max(text.getFontSizeInPt(),1f)));
 }

 private static final class PageText {
  private final float width;private final List<Glyph> glyphs=new ArrayList<>();private final Set<String> seen=new HashSet<>();
  private PageText(float width){this.width=width;}
  private void add(Glyph glyph){String key=glyph.text+'|'+Math.round(glyph.x*2)+'|'+Math.round(glyph.y*2);
   if(seen.add(key))glyphs.add(glyph);}
  private String render(){List<Line> lines=lines();if(lines.isEmpty())return "";Float split=columnSplit(lines);
   StringBuilder value=new StringBuilder();
   if(split==null){lines.stream().sorted(Line.ORDER).forEach(line->append(value,line.text));return value.toString().strip();}
   List<Line> spanning=lines.stream().filter(v->v.minX<=split+5&&v.maxX>=split-5)
    .sorted(Line.ORDER).toList();float from=Float.NEGATIVE_INFINITY;
   for(Line divider:spanning){appendBand(value,lines,split,from,divider.y);append(value,divider.text);from=divider.y+0.1f;}
   appendBand(value,lines,split,from,Float.POSITIVE_INFINITY);return value.toString().strip();
  }
  private PageColumns columns(){List<Line> lines=lines();Float split=columnSplit(lines);if(split==null)return null;
   List<Line> left=new ArrayList<>(),right=new ArrayList<>();for(Line line:lines){
    if((line.minX+line.maxX)/2<=split)left.add(line);else right.add(line);}
   return new PageColumns(split/width,List.copyOf(left),List.copyOf(right));}
  private void appendBand(StringBuilder value,List<Line> lines,float split,float from,float to){
   List<Line> left=lines.stream().filter(v->v.y>=from&&v.y<to&&v.maxX<split-5).sorted(Line.ORDER).toList();
   List<Line> right=lines.stream().filter(v->v.y>=from&&v.y<to&&v.minX>split+5).sorted(Line.ORDER).toList();
   left.forEach(v->append(value,v.text));if(!left.isEmpty()&&!right.isEmpty())blank(value);right.forEach(v->append(value,v.text));
  }
  private Float columnSplit(List<Line> lines){float bestScore=Float.NEGATIVE_INFINITY,best=0;
   for(float ratio=.20f;ratio<=.82f;ratio+=.02f){float split=width*ratio;int left=0,right=0,cross=0;
    for(Line line:lines){if(line.maxX<split-5)left++;else if(line.minX>split+5)right++;else cross++;}
    int minimum=Math.max(5,(int)Math.ceil(lines.size()*.15));if(left<minimum||right<minimum)continue;
    float score=Math.min(left,right)*5f-Math.abs(left-right)-cross*1.5f;
    if(score>bestScore){bestScore=score;best=split;}}
   return bestScore==Float.NEGATIVE_INFINITY?null:best;
  }
  private List<Line> lines(){List<Glyph> sorted=glyphs.stream().sorted(Comparator.comparingDouble(Glyph::y)
    .thenComparingDouble(Glyph::x)).toList();List<Row> rows=new ArrayList<>();
   for(Glyph glyph:sorted){Row row=rows.isEmpty()?null:rows.getLast();
    if(row==null||Math.abs(row.y-glyph.y)>Math.max(2.2f,glyph.height*.45f)){row=new Row();rows.add(row);}
    row.add(glyph);}
   List<Line> result=new ArrayList<>();for(Row row:rows)result.addAll(row.fragments());return result;
  }
 }

 private static final class Row {
  private final List<Glyph> glyphs=new ArrayList<>();private float y;
  private void add(Glyph value){glyphs.add(value);y=glyphs.stream().map(Glyph::y)
    .reduce(0f,Float::sum)/glyphs.size();}
  private List<Line> fragments(){List<Glyph> ordered=glyphs.stream().sorted(Comparator.comparingDouble(Glyph::x)).toList();
   List<List<Glyph>> groups=new ArrayList<>();List<Glyph> current=new ArrayList<>();float previousEnd=-1;
   for(Glyph glyph:ordered){float threshold=Math.max(18f,glyph.fontSize*1.8f);
    if(!current.isEmpty()&&glyph.x-previousEnd>threshold){groups.add(current);current=new ArrayList<>();}
    current.add(glyph);previousEnd=Math.max(previousEnd,glyph.x+glyph.width);}
   if(!current.isEmpty())groups.add(current);return groups.stream().map(Line::from).filter(v->!v.text.isBlank()).toList();}
 }

 private record PageColumns(float ratio,List<Line> left,List<Line> right){}
 private record Glyph(String text,float x,float y,float width,float height,float fontSize){}
 private record Line(String text,float minX,float maxX,float y){
  private static final Comparator<Line> ORDER=Comparator.comparingDouble(Line::y).thenComparingDouble(Line::minX);
  private static Line from(List<Glyph> glyphs){StringBuilder text=new StringBuilder();float min=Float.MAX_VALUE,max=0,y=0,end=-1;
   for(Glyph glyph:glyphs){String token=glyph.text;float gap=end<0?0:glyph.x-end;
    if(!text.isEmpty()&&!Character.isWhitespace(text.charAt(text.length()-1))
      &&!token.isBlank()&&gap>Math.max(1.4f,glyph.fontSize*.18f))text.append(' ');
    text.append(token);min=Math.min(min,glyph.x);max=Math.max(max,glyph.x+glyph.width);end=Math.max(end,glyph.x+glyph.width);y+=glyph.y;}
   return new Line(text.toString().replaceAll("\\s+"," ").strip(),min,max,y/glyphs.size());}
 }
 private static void append(StringBuilder value,String line){if(line==null||line.isBlank())return;
  if(isHeading(line))blank(value);if(!value.isEmpty()&&value.charAt(value.length()-1)!='\n')value.append('\n');
  value.append(line.strip());}
 private static void blank(StringBuilder value){if(value.isEmpty())return;while(value.length()>0&&value.charAt(value.length()-1)=='\n')value.setLength(value.length()-1);
  value.append("\n\n");}
 private static boolean isHeading(String value){String normalized=Normalizer.normalize(value,Normalizer.Form.NFD)
  .replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT).replaceAll("[:|]+$","").strip();return HEADINGS.contains(normalized);}
}
