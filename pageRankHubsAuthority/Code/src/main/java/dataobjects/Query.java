package dataobjects;

import java.util.Map;

public class Query {
  private String id;
  private String text;
  private Map<String, Integer> termCountMap;

  public Query(String id, String text, Map<String, Integer> termCountMap) {
    this.id = id;
    this.text = text;
    this.termCountMap = termCountMap;
  }

  public String getId() {
    return id;
  }

  public String getText() {
    return text;
  }

  public Map<String, Integer> getTermCountMap() {
    return termCountMap;
  }
}
