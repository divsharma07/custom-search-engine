package dto;

import java.util.List;

public class TrainingData {
  private String queryId;
  private List<String> docWithScoreList;
  private double scores[];

  private int relevance;

  public TrainingData(String queryId, List<String> docWithScoreList) {
    this.queryId = queryId;
    this.docWithScoreList = docWithScoreList;
  }

  public String getQueryId() {
    return queryId;
  }

  public List<String> getDocWithScoreList() {
    return docWithScoreList;
  }
}
