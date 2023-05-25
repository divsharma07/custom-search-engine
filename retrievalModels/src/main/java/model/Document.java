package model;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

public class Document implements Serializable {
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Document)) return false;
    Document document = (Document) o;
    return getContent().equals(document.getContent()) && getId().equals(document.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getContent(), getId());
  }

  private String content;
  private String id;
  private Map<String, Term> terms;
  private final int length;

  public Document(String id, String content) {
    this.id = id;
    this.content = content;
    this.length = content.trim().split("\\s+").length;
  }

  public void setTerms(Map<String, Term> terms) {
    this.terms = terms;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getContent() {
    return content;
  }

  public String getId() {
    return id;
  }

  public Map<String, Term> getTerms() {
    return terms;
  }

  public int getLength() {
    return length;
  }
}
