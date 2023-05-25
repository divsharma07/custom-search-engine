package parse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import index.Indexer;
import model.Document;
import model.ParsedDocumentBatch;

public class DocParser {
  private Indexer indexer;
  private String indexName;
  private boolean isStemmed;

  public DocParser(String indexName, Indexer indexer, boolean isStemmed) {
    this.indexName = indexName;
    this.indexer = indexer;
    this.isStemmed = isStemmed;
  }

  public ParsedDocumentBatch parseDocsPartially(List<File> docFiles) throws IOException {
    int totalTokens = 0;
    List<Document> docs = new ArrayList<>();
    Map<String, Integer> docIdLenMap = new HashMap<>();
    Preprocessor preprocessor = new Preprocessor(isStemmed);
    Map<String, Integer> docIdMap = new HashMap<>();
    for (File file : docFiles) {
      BufferedReader fileReader = new BufferedReader(new FileReader(file));
      String line;
      String docId = "";
      StringBuilder content = new StringBuilder();
      while ((line = fileReader.readLine()) != null) {
        if (line.contains("<DOCNO>")) {
          if (!docId.equals("")) {
            String processedString = preprocessor.customPreprocessing(content.toString());
            Document currDoc = new Document(docId, processedString);
            docIdMap.put(docId, currDoc.getShortId());
            TreeMap<String, List<Integer>> docTerms = getTermsFromString(processedString);
            currDoc.setTerms(docTerms);
            int tokensAdded = processedString.split(" ").length;
            totalTokens += tokensAdded;
            docIdLenMap.put(docId, tokensAdded);
            docs.add(currDoc);
          }
          content.setLength(0);
          docId = line.split(" ")[1];
        }
        if (line.contains("<TEXT>")) {
          while ((line = fileReader.readLine()) != null && !line.contains("</TEXT>")) {
            content.append(" ").append(line.trim());
          }
        }
      }
      if (!docId.equals("") && content.length() > 0) {
        docs.add(new Document(docId, content.toString()));
      }
    }

    return new ParsedDocumentBatch(docs, docIdMap, totalTokens, docIdLenMap);
  }

  private TreeMap<String, List<Integer>> getTermsFromString(String processedString) {
    TreeMap<String, List<Integer>> termsMap = new TreeMap<>();
    String[] splitString = processedString.split(" ");
    for (int i = 0; i < splitString.length; i++) {
      String token = splitString[i];
      if(token.equals("")) {
        continue;
      }
      if (!termsMap.containsKey(token)) {
        termsMap.put(token, new ArrayList<>());
      }
      termsMap.get(token).add(i + 1);
    }
    return termsMap;
  }

  public int getAvgDocLength(List<Document> docs) {
    int totalLengths = 0;

    for (Document doc : docs) {
      totalLengths += doc.getLength();
    }

    return totalLengths / docs.size();
  }

  public Set<String> getVocabulary(List<Document> docs) {
    Set<String> result = new HashSet<>();
    for (Document doc : docs) {
      result.addAll(doc.getTerms().keySet());
    }
    return result;
  }
}