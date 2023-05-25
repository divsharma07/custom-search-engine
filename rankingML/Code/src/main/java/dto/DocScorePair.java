package dto;

import java.util.Comparator;
import java.util.Objects;

public class DocScorePair implements Comparable<DocScorePair> {
  private String docId;
  private double score;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DocScorePair)) return false;
    DocScorePair that = (DocScorePair) o;
    return Double.compare(that.getScore(), getScore()) == 0 && Objects.equals(getDocId(), that.getDocId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getDocId(), getScore());
  }

  public DocScorePair(String docId, double score) {
    this.docId = docId;
    this.score = score;
  }

  public String getDocId() {
    return docId;
  }

  public double getScore() {
    return score;
  }

  @Override
  public int compareTo(DocScorePair o) {
    return Double.compare(o.getScore(), getScore());
  }
}
