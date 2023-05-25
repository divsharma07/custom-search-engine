package frontier;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * This queue pertains to each domain that is being crawled and takes care of the politeness aspect of the crawler.
 * Each of these queues get mapped to a domain using a table that is maintained in redis.
 */
public class BackFrontierQueue extends FrontierQueue implements Comparable<BackFrontierQueue> {
  private String domain;
  private int index;

  public void setDomain(String domain) {
    this.domain = domain;
  }

  private double nextPoliteTimestamp;

  public BackFrontierQueue(int index, double nextPoliteTimestamp) {
    this.nextPoliteTimestamp = nextPoliteTimestamp;
    this.index = index;
  }

  /**
   * Calculated the next time this element can be crawled.
   *
   * @param msToBeAdded milli seconds to be add to the existing timestamp
   */
  public void setNextPoliteTimestamp(int msToBeAdded) {
    double currentTime = System.currentTimeMillis();
    this.nextPoliteTimestamp = currentTime + msToBeAdded;
  }


  /**
   * Get the next polite timestamp.
   *
   * @return the timestamp
   */
  public double getNextPoliteTimestamp() {
    return nextPoliteTimestamp;
  }


  public int getIndex() {
    return index;
  }

  @Override
  public int compareTo(@NotNull BackFrontierQueue o) {
    return Double.compare(this.getNextPoliteTimestamp(), o.getNextPoliteTimestamp());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BackFrontierQueue that)) return false;
    return getIndex() == that.getIndex();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getIndex());
  }

  public String getDomain() {
    return domain;
  }
}
