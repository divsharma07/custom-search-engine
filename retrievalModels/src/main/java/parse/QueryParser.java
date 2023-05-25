package parse;

import org.apache.lucene.analysis.TokenStream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import model.Query;

import static java.awt.SystemColor.text;

public class QueryParser {
    private String fileName;

    public QueryParser(String fileName) {
      this.fileName = fileName;
    }

    public List<Query> parseQueries() {
      List<Query> queries = new ArrayList<>();

      try {
        String filePath = new File("").getAbsolutePath();
        File file = new File(filePath+ "\\IR_data\\AP_DATA\\" + fileName);
        Scanner sc = new Scanner(file);
        Preprocessor p = new Preprocessor();

        while (sc.hasNextLine()) {
          String currLine = sc.nextLine();
          String[] split = currLine.split("\\.");
          if(currLine.equals("")) continue;
          String query = p.stemmingAndStopWords(split[1].trim()).trim();
          Map<String, Integer> wordCount = getWordCount(query);
          Query currQuery = new Query(split[0].trim(), query, wordCount);
          queries.add(currQuery);
        }
        sc.close();
      } catch (FileNotFoundException e) {
        System.out.println("File not found");
      } catch (IOException e) {
        e.printStackTrace();
      }

      return queries;
    }

  private Map<String,Integer> getWordCount(String query) {
      Map<String, Integer> map = new HashMap<>();

      String[] split = query.split(" ");
      for(String each: split) {
        map.put(each, map.getOrDefault(each, 0) + 1);
      }
      return map;
  }

}
