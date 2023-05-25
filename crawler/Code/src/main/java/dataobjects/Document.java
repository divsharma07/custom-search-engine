package dataobjects;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class Document implements Serializable {
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Document)) return false;
    Document document = (Document) o;
    return getContent().equals(document.getContent()) && getDocid().equals(document.getDocid());
  }

  public Document() {
  }

  @Override
  public int hashCode() {
    return Objects.hash(getContent(), getDocid());
  }

  private String content;
  private String docid;
  private String title;

  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<String> outLinks;

  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<String> inLinks;

  private String author = "Divyanshu Sharma";

  public Document(String docid, String content, String title) {
    this.docid = docid;
    this.content = content;
    this.title = title;
  }

  public Document(String docid, String content, String title, List<String> inLinks, List<String> outLinks,  String author) {
    this.docid = docid;
    this.content = content;
    this.title = title;
    this.author = author;
    this.inLinks = inLinks;
    this.outLinks = outLinks;
  }



  public void setContent(String content) {
    this.content = content;
  }

  public String getContent() {
    return content;
  }

  public String getDocid() {
    return docid;
  }

  public void setDocid(String docid) {
    this.docid = docid;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getTitle() {
    return title;
  }

  public List<String> getOutLinks() {
    return outLinks;
  }

  public void setOutLinks(List<String> outLinks) {
    this.outLinks = outLinks;
  }

  public List<String> getInLinks() {
    return inLinks;
  }

  public void setInLinks(List<String> inLinks) {
    this.inLinks = inLinks;
  }

  public String getAuthor() {
    return author;
  }
}
