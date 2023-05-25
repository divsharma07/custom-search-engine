package frontier;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static config.Configuration.FRONT_QUEUE_COUNT;
import static config.Configuration.RELEVANCE_THRESHOLDS;
import static config.Configuration.RELEVANT_KEYWORDS;

/**
 * This is essentially nothing but each URL that is a part of the Frontier.
 */
public class FrontierElement {
  private int waveNumber;
  private String anchorText = "";

  private double nextPoliteTimestamp;

  @Expose
  private String url;

  @Expose
  private List<String> inlinks = new ArrayList<>();
  @Expose
  private List<String> outlinks = new ArrayList<>();

  private String currentParent;

  public FrontierElement(int waveNumber, String url) {
    this.url = url;
    this.waveNumber = waveNumber;
    this.nextPoliteTimestamp = new Date().getTime();
  }

  public FrontierElement(int waveNumber, String url, String anchorText) {
    this.url = url;
    this.waveNumber = waveNumber;
    this.anchorText = anchorText;
    this.nextPoliteTimestamp = new Date().getTime();
  }

  public FrontierElement(int waveNumber, String url, String anchorText, String currentParent) {
    this.url = url;
    this.waveNumber = waveNumber;
    this.anchorText = anchorText;
    this.nextPoliteTimestamp = new Date().getTime();
    this.currentParent = currentParent;
  }

  public String getUrl() {
    return url;
  }

  /**
   * This method calculated a score and then spits out the value of the FrontFrontierQueue this
   * URL should go to.
   *
   * @return the queue number, from 0 to FRONT_QUEUE_COUNT
   */
  public int findFrontQueueBasedOnRelevance() {
    int queueNumber = Arrays.binarySearch(RELEVANCE_THRESHOLDS, computeScore());

    if(queueNumber < 0)  {
      queueNumber = ~queueNumber;
      if(queueNumber >= FRONT_QUEUE_COUNT) {
        queueNumber--;
      }
    }

    return queueNumber;
  }

  public int getWaveNumber() {
    return waveNumber;
  }

  public double computeScore() {
    int keyWordUrlMatches = 0;

    for(String keyWord: RELEVANT_KEYWORDS) {
      if(url.toLowerCase().contains(keyWord)) {
        keyWordUrlMatches++;
      }
    }

    return 15*keyWordUrlMatches + 3* inlinks.size() - 5 * waveNumber;
  }

  /**
   * Calculated the next time this element can be crawled.
   *
   * @param msToBeAdded milliseconds to be add to the existing timestamp
   */
  public void setNextPoliteTimestamp(int msToBeAdded) {
    double currentTime = System.currentTimeMillis();
    this.nextPoliteTimestamp = currentTime + msToBeAdded;
  }

  public void addOutLink(String outLink) {
    this.outlinks.add(outLink);
  }

  public void addInLink(String inLink) {
    this.inlinks.add(inLink);
  }

  @Override
  public String toString() {
    return "FrontierElement{" +
            ", waveNumber=" + waveNumber +
            ", url='" + url + '\'' +
            ", inlinks=" + inlinks +
            ", outlinks=" + outlinks +
            '}';
  }


  public double getNextPoliteTimestamp() {
    return nextPoliteTimestamp;
  }


  public List<String> getInlinks() {
    return inlinks;
  }

  public List<String> getOutlinks() {
    return outlinks;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof FrontierElement element)) return false;
    return anchorText.equals(element.anchorText) && getUrl().equals(element.getUrl());
  }

  @Override
  public int hashCode() {
    return Objects.hash(anchorText, getUrl());
  }

  public void setCurrentParent(String currentParent) {
    this.currentParent = currentParent;
  }

  public String getCurrentParent() {
    return currentParent;
  }
}
