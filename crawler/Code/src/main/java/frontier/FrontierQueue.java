package frontier;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Base class for the different kinds of queues in the Frontier.
 */
public abstract class FrontierQueue {

  private Queue<FrontierElement> elements = new LinkedList<>();

  public void addElement(FrontierElement element) {
    elements.add(element);
  }

  public FrontierElement peekElement() {
    return elements.peek();
  }

  public FrontierElement pollElement() {
    return elements.poll();
  }

  public Queue<FrontierElement> getElements() {
    return new LinkedList<>(elements);
  }

  public int size() {
    return elements.size();
  }
}
