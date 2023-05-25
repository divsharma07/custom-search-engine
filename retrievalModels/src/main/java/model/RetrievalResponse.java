package model;

import java.io.Serializable;

public class RetrievalResponse {
  private Document document;
  private String docId;
  private String queryId;
  private double score;

  public RetrievalResponse(Document document, String queryId, double score) {
    this.document = document;
    this.docId = document.getId();
    this.queryId = queryId;
    this.score = score;
  }

  public RetrievalResponse(String docId, String queryId, double score) {
    this.docId = docId;
    this.queryId = queryId;
    this.score = score;
  }

  //returning copy
  public Document getDocument() {
    return new Document(document.getId(), document.getContent());
  }
  public String getDocId() {
    return docId;
  }

  public String getQueryId() {
    return queryId;
  }

  public double getScore() {
    return score;
  }
}
