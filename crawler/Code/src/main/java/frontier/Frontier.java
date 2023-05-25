package frontier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;

import spider.Crawler;
import util.Canonicalizer;
import util.Helper;

import static config.Configuration.BACK_QUEUE_COUNT;
import static config.Configuration.FRONT_QUEUE_COUNT;
import static config.Configuration.LINKS_FILES_DIR;
import static util.RedisHelper.addToOutLinkQueue;
import static util.RedisHelper.getBackQueueNumberFromURL;
import static util.RedisHelper.getBackQueues;
import static util.RedisHelper.getCrawledElementsMap;
import static util.RedisHelper.getFrontQueues;
import static util.RedisHelper.getUrlCrawlCount;
import static util.RedisHelper.pollElementFromOutLinkQueue;
import static util.RedisHelper.saveQueueState;
import static util.RedisHelper.setBackQueueWithURL;
import static util.RedisHelper.urlAlreadyCrawled;


/**
 * The class that orchestrates and encapsulates all the components of a Mercator crawler.
 */
public class Frontier {

  private PriorityQueue<BackFrontierQueue> politenessHeap;
  private List<FrontierQueue> frontQueues;
  private List<FrontierQueue> backQueues;
  private ExecutorService executorService;
  private List<FrontierElement> allOutLinks;

  public Frontier(List<String> seedUrls, ExecutorService executorService) {
    politenessHeap = new PriorityQueue<>(BACK_QUEUE_COUNT);
    this.executorService = executorService;
    // initializing front queues with seeds
    // this method also makes sure to get data from redis in case this is not the initial run that
    // requires seed
    frontQueues = getFrontQueues(seedUrls);
    // initializing back queues
    backQueues = getBackQueues();
    addAllBackQueuesToHeap();
  }

  private void addAllBackQueuesToHeap() {
    for (FrontierQueue q : backQueues) {
      politenessHeap.add((BackFrontierQueue) q);
    }
  }

  public void orchestrate() {
    // add seeds to front queue
    Crawler crawler = new Crawler(executorService);

    Long urlsCrawled = getUrlCrawlCount();
    while (urlsCrawled < 40000) {
      // loading urls to the back queues
      fetchURLFrontToBack((int)(BACK_QUEUE_COUNT*0.75));
      // saving the current state of front queues in case of crash
      saveQueueState(frontQueues);

      List<FrontierElement> urlsToBeCrawled = fetchUrlsFromPolitenessHeap();

      // crawls and saves outlinks to redis
      addToOutLinkQueue(crawler.crawlAndSave(urlsToBeCrawled));


      addToFrontQueue(200);
      urlsCrawled = getUrlCrawlCount();
    }
  }


  private void addToFrontQueue(int outLinksCount) {
    while(outLinksCount-- > 0) {
      FrontierElement outLink = pollElementFromOutLinkQueue();
      if(outLink == null) {
        // this means that the queue is empty right now
        return;
      }
      if (!urlAlreadyCrawled(outLink.getUrl())) {
        int frontQueueNumber = outLink.findFrontQueueBasedOnRelevance();
        frontQueues.get(frontQueueNumber).addElement(outLink);
      }
    }
  }

  private List<FrontierElement> fetchUrlsFromPolitenessHeap() {
    List<FrontierElement> result = new ArrayList<>();

    while (!politenessHeap.isEmpty()) {

      BackFrontierQueue backQueue = politenessHeap.poll();
      if (backQueue.size() != 0) {
        result.addAll(addMultipleBackQueueElements(backQueue));
      }
    }
    addMissingQueuesToHeap();

    return result;
  }

  private List<FrontierElement> addMultipleBackQueueElements(BackFrontierQueue backQueue) {
    List<FrontierElement> result = new ArrayList<>();
    int politenessMilliseconds = 3000;
    while (backQueue.size() != 0) {
      FrontierElement element = backQueue.pollElement();
      // adding a politeness timestamp if the elements belong to the same domain
      element.setNextPoliteTimestamp(politenessMilliseconds);
      politenessMilliseconds += 1000;
      result.add(element);
    }

    return result;
  }


  private void addMissingQueuesToHeap() {
    for (FrontierQueue q : backQueues) {
      BackFrontierQueue backQueue = (BackFrontierQueue) q;
      if (!politenessHeap.contains(backQueue)) {
        addPolledQueueToHeap(backQueue);
      }
    }
  }

  private FrontierElement fetchUrlFromPolitenessHeap() {


    while (!politenessHeap.isEmpty()) {

      BackFrontierQueue backQueue = politenessHeap.poll();
      if (backQueue.size() == 0) {
        // trying to keep back queues non-empty as much as possible
        // but not re-adding to heap just yet, in order to maintain politeness
        // since a newly added url might belong to a domain that has already been picked
        // for this parallel crawling iteration
        fetchURLFrontToBack(backQueue.getIndex(), getBiasedRandomFrontierElement());
      } else {
        return backQueue.pollElement();
      }
    }

    // all possible unique back queues have been picked already
    return null;
  }

  private void addPolledQueueToHeap(BackFrontierQueue queue) {
    // setting next polite time to pick the queue
    queue.setNextPoliteTimestamp(1500);
    politenessHeap.add(queue);
  }

  private void addBackQueueToHeap() {
    for (FrontierQueue q : backQueues) {
      if (!politenessHeap.contains((BackFrontierQueue) q)) {
        politenessHeap.add((BackFrontierQueue) q);
      }
    }
  }

  private void fetchURLFrontToBack(int urlCount) {
    while (urlCount-- > 0) {
      FrontierElement element = getBiasedRandomFrontierElement();

      if (element == null) {
        // this means that front queues are empty right now
        return;
      }
      String domain = Canonicalizer.getDomain(element.getUrl());
      Integer backQueueIndex = getBackQueueNumberFromURL(domain);
      if (backQueueIndex == null) {
        // find empty queue and add element to it
        int backQueueNumber = findEmptyBackQueue();
        if (backQueueNumber == -1) {
          // all back queues are filled and we have more unique domains than number of back queues
          putElementBackToFrontQueue(element);
        }
        fetchURLFrontToBack(findEmptyBackQueue(), element);
      } else {
        fetchURLFrontToBack(backQueueIndex, element);
      }
    }
  }

  private void putElementBackToFrontQueue(FrontierElement element) {
    frontQueues.get(element.findFrontQueueBasedOnRelevance()).addElement(element);
  }

  //  The method polls a Frontier queue based on biased priority
  // higher numbered queues get picked more often
  private FrontierElement getBiasedRandomFrontierElement() {
    FrontierElement element = null;
    if (allQueuesEmpty(frontQueues)) {
      return null;
    }
    do {
      int pickFrom = Helper.randomBiased(0, FRONT_QUEUE_COUNT, 0.3);
      element = frontQueues.get(pickFrom).pollElement();
    } while (element == null);
    return element;
  }

  private boolean allQueuesEmpty(List<FrontierQueue> queues) {
    for (FrontierQueue q : queues) {
      if (q.size() > 0) return false;
    }
    return true;
  }

  private void fetchURLFrontToBack(int backQueueNumber, FrontierElement element) {
    if (element == null) return;
    String elementDomain = Canonicalizer.getDomain(element.getUrl());
    Integer existingQueueNumber = getBackQueueNumberFromURL(elementDomain);
    if (existingQueueNumber != null) {
      BackFrontierQueue backQueue = (BackFrontierQueue) backQueues.get(existingQueueNumber);
      if (backQueue.size() > 0 && !backQueue.getDomain().equals(elementDomain)) {
        System.out.println("Cannot assign new domain to a non empty back queue");
        return;
      }
    }

    setBackQueueWithURL(elementDomain, backQueueNumber);
    BackFrontierQueue bq = (BackFrontierQueue) backQueues.get(backQueueNumber);
    bq.setDomain(elementDomain);
    bq.addElement(element);
  }

  private int findEmptyBackQueue() {
    for (int i = 0; i < backQueues.size(); i++) {
      if (backQueues.get(i).size() == 0) return i;
    }
    return -1;
  }


  public static void main(String[] args) {

  }

  @Override
  public String toString() {
    return "Frontier{" +
            "frontQueues=" + frontQueues +
            ", backQueues=" + backQueues +
            '}';
  }

  public void writeLinkGraphsToFile() throws IOException {
    Map<String, FrontierElement> map = getCrawledElementsMap();
    Helper.initDirectory(LINKS_FILES_DIR);
    String inLinkFilename = LINKS_FILES_DIR + "/inLinks.txt";
    BufferedWriter inLinkWriter = new BufferedWriter(new FileWriter(inLinkFilename, true));
    String outLinkFilename = LINKS_FILES_DIR + "/outLinks.txt";
    BufferedWriter outLinkWriter = new BufferedWriter(new FileWriter(outLinkFilename, true));

    for (Map.Entry<String, FrontierElement> each: map.entrySet()) {
      FrontierElement element = each.getValue();
      Gson gson = new GsonBuilder()
              .excludeFieldsWithoutExposeAnnotation()
              .create();

      JsonObject inLinksJson = (JsonObject) gson.toJsonTree(element);
      inLinksJson.remove("outlinks");
      JsonObject outLinksJson = (JsonObject) gson.toJsonTree(element);
      outLinksJson.remove("inlinks");
      inLinkWriter.write(inLinksJson.toString() + "\n");
      inLinkWriter.flush();
      outLinkWriter.write(outLinksJson.toString() + "\n");
      outLinkWriter.flush();
    }
    inLinkWriter.close();
    outLinkWriter.close();
  }

  private void writeOutLinksFile(Map<String, FrontierElement> map) {
  }

  private void writeInLinksFile(Map<String, FrontierElement> map) {

  }
}
