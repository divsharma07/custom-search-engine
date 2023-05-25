package model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Document implements Serializable {
  private String content;
  private static final AtomicInteger count = new AtomicInteger(0);
  private String longId;
  private int shortId;
  private TreeMap<String, List<Integer>> terms;
  private int length;

  public Document(String longId, String content) {
    this.longId = longId;
    this.content = content;
    this.shortId = count.getAndIncrement();
    this.length = content.trim().split("\\s+").length;
    terms = new TreeMap<>();
  }

  public Document(String longId, int shortId) {
    this.longId = longId;
    this.shortId = shortId;
    terms = new TreeMap<>();
  }

  public void setLength(int length) {
    this.length = length;
  }

  public void addTerm(String term, List<Integer> value) {
    terms.put(term, value);
  }

  public void setTerms(TreeMap<String, List<Integer>> terms) {
    this.terms = terms;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getContent() {
    return content;
  }

  public String getLongId() {
    return longId;
  }

  public int getShortId() {
    return shortId;
  }

  public TreeMap<String, List<Integer>> getTerms() {
    return terms;
  }

  public int getLength() {
    return length;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Document)) return false;
    Document document = (Document) o;
    return getContent().equals(document.getContent()) && getLongId().equals(document.getLongId()) && getShortId() == (document.getShortId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getContent(), getLongId(), getShortId());
  }
}
