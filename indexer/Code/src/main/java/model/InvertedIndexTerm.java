
package model;

import java.util.List;
import java.util.Objects;
import java.util.TreeMap;


public class InvertedIndexTerm {
  private String termText;
  private int termId;
  private TreeMap<Integer, List<Integer>> docIdPositionMap;

  public InvertedIndexTerm(String termText, int termId) {
    this.termText = termText;
    this.termId = termId;
    docIdPositionMap = new TreeMap<>();
  }

  public InvertedIndexTerm(InvertedIndexTerm copy) {
    this.termText = copy.termText;
    this.termId = copy.termId;
    this.docIdPositionMap = copy.docIdPositionMap;
  }

  public InvertedIndexTerm(String termText, int termId, TreeMap<Integer, List<Integer>> docIdPositionMap) {
    this.termId = termId;
    this.docIdPositionMap = docIdPositionMap;
    this.termText = termText;
  }

  public int getTotalTernFreq() {
    return docIdPositionMap.values().size();
  }

  public int getDocFreq() {
    return docIdPositionMap.size();
  }

  public String getTerm() {
    return termText;
  }

  public int getTermId() {
    return termId;
  }

  public TreeMap<Integer, List<Integer>> getDocIdPositionMap() {
    return docIdPositionMap;
  }

  public void addDoc(int docId, List<Integer> positions) {
    docIdPositionMap.put(docId, positions);
  }

  @Override
  public String toString() {
    return "InvertedIndexTerm{" +
            "termText='" + termText + '\'' +
            ", termId=" + termId +
            ", docIdPositionMap=" + docIdPositionMap +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof InvertedIndexTerm)) return false;
    InvertedIndexTerm that = (InvertedIndexTerm) o;
    return getTermId() == that.getTermId();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getTermId());
  }
}
