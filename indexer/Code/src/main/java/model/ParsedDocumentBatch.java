package model;

import java.util.List;
import java.util.Map;

public class ParsedDocumentBatch {
  private List<Document> docs;
  private Map<String, Integer> docIdMap;
  private int totalTokens;
  private Map<String, Integer> docIdLenMap;

  public ParsedDocumentBatch(List<Document> docs, Map<String, Integer> docIdMap, int totalTokens, Map<String, Integer> docIdLenMap) {
    this.docs = docs;
    this.docIdMap = docIdMap;
    this.totalTokens = totalTokens;
    this.docIdLenMap = docIdLenMap;
  }

  public List<Document> getDocs() {
    return docs;
  }

  public Map<String, Integer> getDocIdMap() {
    return docIdMap;
  }

  public int getTotalTokens() {
    return totalTokens;
  }

  public Map<String, Integer> getDocIdLenMap() {
    return docIdLenMap;
  }
}
