package dataobjects;

import java.io.Serializable;
import java.util.Objects;

public class Term implements Serializable {
  private final String title;
  private final int docFreq;
  private final int termFreq;
  private final long totalTermFreq;
  private double relavenceScore;
  private double idf;

  public Term(String title, int docFreq, int termFreq, long totalTermFreq, double idf) {
    this.title = title;
    this.docFreq = docFreq;
    this.termFreq = termFreq;
    this.totalTermFreq = totalTermFreq;
    this.idf = idf;
  }

//  public Term(String title, double relavenceScore) {
//    this.title = title;
//    this.relavenceScore = relavenceScore;
//  }

  public double getIdf() {
    return idf;
  }

  public String getTitle() {
    return title;
  }

  public int getDocFreq() {
    return docFreq;
  }

  public void setRelavenceScore(double relavenceScore) {
    this.relavenceScore = relavenceScore;
  }

  public double getRelavenceScore() {
    return relavenceScore;
  }

  public int getTermFreq() {
    return termFreq;
  }

  public long getTotalTermFreq() {
    return totalTermFreq;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Term)) return false;
    Term term = (Term) o;
    return Objects.equals(getTitle(), term.getTitle());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getTitle());
  }
}
