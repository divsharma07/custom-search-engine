package dataobjects;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Document implements Serializable {

  private String content = "";
  private String docid;
  private String title;
  private double pageRank = 0.0;

  private double hubScore = 1.0;

  private double authorityScore = 1.0;

  private double newAuthorityScore = 1.0;

  private double newHubScore = 1.0;
  private int outLinksCount = 0;
  private double newPageRank = 0.0;

  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<String> outLinks = new ArrayList<>();

  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<String> inLinks = new ArrayList<>();

  private String author = "Divyanshu Sharma";


  public Document(String docId, Set<String> inLinks, int outLinksCount) {
    this.docid = docId;
    this.inLinks = new ArrayList<>(inLinks);
    this.outLinksCount = outLinksCount;
  }

  public Document(String docId, Set<String> inLinks) {
    this.docid = docId;
    this.inLinks = new ArrayList<>(inLinks);
  }

  public Document(String docid, Set<String> inLinks, Set<String> outlinks) {
    this.docid = docid;
    this.inLinks = new ArrayList<>(inLinks);
    this.outLinks = new ArrayList<>(outlinks);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Document)) return false;
    Document document = (Document) o;
    return getDocid().equals(document.getDocid());
  }


  public Document(String docid, String content, String title) {
    this.docid = docid;
    this.content = content;
    this.title = title;
  }

  public Document(String docid, String content, String title, List<String> inLinks, List<String> outLinks, String author) {
    this.docid = docid;
    this.content = content;
    this.title = title;
    this.author = author;
    this.inLinks = inLinks;
    this.outLinks = outLinks;
  }

  public Document(String docId) {
    this.docid = docId;
  }

  public Document(String docId, List<String> inLinks) {
    this.docid = docId;
    this.inLinks = inLinks;
  }


  public Document() {
  }

  @Override
  public int hashCode() {
    return Objects.hash(getDocid());
  }


  public double getNewPageRank() {
    return newPageRank;
  }

  public void setNewPageRank(double newPageRank) {
    this.newPageRank = newPageRank;
  }

  public double getPageRank() {
    return pageRank;
  }

  public void setPageRank(double pageRank) {
    this.pageRank = pageRank;
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

  public void addOutLink(String outLink) {
    outLinks.add(outLink);
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

  public int getOutLinksCount() {
    if (outLinks == null || outLinksCount!= 0) return outLinksCount;
    else return outLinks.size();
  }

  public double getHubScore() {
    return hubScore;
  }

  public void setHubScore(double hubScore) {
    this.hubScore = hubScore;
  }

  public void addInLinks(String inLink) {
    inLinks.add(inLink);
  }

  public double getAuthorityScore() {
    return authorityScore;
  }

  public void setAuthorityScore(double authorityScore) {
    this.authorityScore = authorityScore;
  }

  public double getNewAuthorityScore() {
    return newAuthorityScore;
  }

  public void setNewAuthorityScore(double newAuthorityScore) {
    this.newAuthorityScore = newAuthorityScore;
  }

  public double getNewHubScore() {
    return newHubScore;
  }

  public void setNewHubScore(double newHubScore) {
    this.newHubScore = newHubScore;
  }
}
