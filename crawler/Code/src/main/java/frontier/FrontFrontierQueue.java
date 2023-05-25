package frontier;

/**
 * This queue handles the priority of the urls that are being crawled. Higher priority means that even when randomly being transfered
 * to the BackFrontierQueue, if the priority number is higher, there are higher chances of the urls in this queue to be picked.
 */
public class FrontFrontierQueue extends FrontierQueue {
  // higher priority means better relevance
  private int priority;

  public FrontFrontierQueue(int priority) {
    this.priority = priority;
  }

  public int getPriority() {
    return priority;
  }

  @Override
  public String toString() {
    return "FrontQueue{" +
            "priority=" + priority +
            ", elements=" + super.getElements() +
            '}';
  }
}
