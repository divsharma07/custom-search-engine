package parse;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.tartarus.snowball.ext.PorterStemmer;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Preprocessor {
  boolean stem;

  public Preprocessor(boolean stem) {
    this.stem = stem;
  }

  public String customPreprocessing(String inputString) {
    //inputString = "This is bob and Bob. that is my aunt's This is 192.168.1.1. and these should be different is 123,121 . Also USA and U.S.A . Aunt's and Aunts. haha lol haha lol";
    inputString = inputString.toLowerCase();
    String regex = "\\w+(?:\\.?\\w)*";
    StringBuilder inputWithoutStopWords = new StringBuilder();
    StringBuilder result = new StringBuilder();
    Set<String> stopWords = getStopWordSet();
    for (String each : inputString.split(" ")) {
      if (stopWords.contains(each)) continue;
      // this makes sure u.s.a becomes usa
      if (isStringOnlyAlphabet(each.replaceAll("\\.", ""))) {
        inputWithoutStopWords.append(each.replaceAll("\\.", "")).append(" ");
      } else {
        inputWithoutStopWords.append(each).append(" ");
      }
    }

    Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(inputWithoutStopWords.toString());
    while (matcher.find()) {
      String matchedToken = matcher.group();
      PorterStemmer stemmer = new PorterStemmer();
      stemmer.setCurrent(matchedToken);
      if (stem && stemmer.stem()) {
        String stemmedWord = stemmer.getCurrent();
        result.append(stemmedWord).append(" ");
      } else {
        result.append(matchedToken).append(" ");
      }
    }
    return result.toString();
  }

  private boolean isStringOnlyAlphabet(String str) {
    return !str.equals("") && str.matches("^[a-zA-Z]*$");
  }

  private Set<String> getStopWordSet() {
    List<String> stopWords = new ArrayList<>();
    try {
      String filePath = new File("").getAbsolutePath() + "\\IR_data\\AP_DATA\\stoplist.txt";
      stopWords = Files.readAllLines(Paths.get(filePath));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return new HashSet<String>(stopWords);
  }

  /**
   * Method tokenizer, stems and removes stop words.
   *
   * @param inputString the string that needs to be preprocessed
   * @return the processed output
   * @throws IOException
   */
  // inspired from this example: https://thekandyancode.wordpress.com/2013/02/04/tokenizing-stopping-and-stemming-using-apache-lucene/
  public String stemmingAndStopWords(String inputString) throws IOException {
    Tokenizer tokenizer = new StandardTokenizer();
    tokenizer.setReader(new StringReader(inputString));
    TokenStream tokenStream = tokenizer;
    tokenStream = new LowerCaseFilter(tokenStream);
    tokenStream = new StopFilter(tokenStream, new CharArraySet(getStopWordSet(), false));
    tokenStream = new PorterStemFilter(tokenStream);
    CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
    tokenStream.reset();
    StringBuilder sb = new StringBuilder();
    while (tokenStream.incrementToken()) {
      sb.append(" ").append(charTermAttribute.toString());
    }
    tokenStream.close();
    return sb.toString();
  }
}
