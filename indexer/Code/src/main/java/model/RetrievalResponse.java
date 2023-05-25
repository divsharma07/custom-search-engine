package model;

import java.util.Objects;

public class RetrievalResponse {
  private Document document;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof RetrievalResponse)) return false;
    RetrievalResponse that = (RetrievalResponse) o;
    return getDocId().equals(that.getDocId()) && getQueryId().equals(that.getQueryId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getDocId(), getQueryId());
  }

  private String docId;
  private String queryId;
  private double score;

  public RetrievalResponse(Document document, String queryId, double score) {
    this.document = document;
    this.docId = document.getLongId();
    this.queryId = queryId;
    this.score = score;
  }

  public RetrievalResponse(String docId, String queryId, double score) {
    this.docId = docId;
    this.queryId = queryId;
    this.score = score;
  }

  public void addScore(double score) {
    this.score += score;
  }

  //returning copy
  public Document getDocument() {
    return new Document(document.getLongId(), document.getContent());
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
