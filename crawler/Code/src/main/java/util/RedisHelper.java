package util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import config.Configuration;
import frontier.BackFrontierQueue;
import frontier.FrontFrontierQueue;
import frontier.FrontierElement;
import frontier.FrontierQueue;
import redis.JedisFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import static config.Configuration.ALL_URLS_DB;
import static config.Configuration.BACK_QUEUE_COUNT;
import static config.Configuration.FRONT_QUEUE_COUNT;
import static config.Configuration.OUTLINKS_QUEUE;
import static config.Configuration.QUEUE_MAP_DB;

/**
 * All the redis related helper methods.
 * Mainly deal with putting and retrieving key values and polling and adding to queues.
 */
public class RedisHelper {

  public static synchronized boolean urlAlreadyCrawled(String url) {
    boolean result;
    try (Jedis j = JedisFactory.getInstance().getJedisPool().getResource()) {
      j.select(Configuration.CRAWLED_URLS_DB);
      result = (j.get(url) != null);
    }
    return result;
  }

  public static void saveQueueState(List<FrontierQueue> queues) {
    for (int i = 0; i < queues.size(); i++) {
      FrontFrontierQueue front = (FrontFrontierQueue) queues.get(i);
      setFrontQueue(i, front);
    }
  }

  public static synchronized FrontierElement getFrontierElementFromUrl(String url, int dbIndex) {
    FrontierElement element;
    try (Jedis j = JedisFactory.getInstance().getJedisPool().getResource()) {
      j.select(dbIndex);
      String jsonString = j.get(url);
      if (jsonString == null) return null;
      element = Helper.JsonToObject(j.get(url), FrontierElement.class);
    }
    return element;
  }

  public static synchronized void setFrontierElementWithUrl(String url, FrontierElement element, int dbIndex) {
    try (Jedis j = JedisFactory.getInstance().getJedisPool().getResource()) {
      j.select(dbIndex);
      j.set(url, Helper.ObjectToJson(element));
    }
  }

  public static synchronized Integer getBackQueueNumberFromURL(String url) {
    Integer result = null;
    try (Jedis j = JedisFactory.getInstance().getJedisPool().getResource()) {
      j.select(QUEUE_MAP_DB);
      String value = j.get(url);
      if (value == null) return null;
      result = Integer.parseInt(j.get(url));
    }
    return result;
  }

  public static synchronized void setBackQueueWithURL(String url, int queueNumber) {
    try (Jedis j = JedisFactory.getInstance().getJedisPool().getResource()) {
      j.select(QUEUE_MAP_DB);
      j.set(url, String.valueOf(queueNumber));
    }
  }

  public static List<FrontierQueue> getBackQueues() {
    List<FrontierQueue> backQueues = new ArrayList<>();
    for (int i = 0; i < BACK_QUEUE_COUNT; i++) {
      // adding a really high time for empty queues so that new non empty ones get picked at the start
      backQueues.add(new BackFrontierQueue(i, System.currentTimeMillis() + 500000));
    }
    return backQueues;
  }

  public static Long getUrlCrawlCount() {
    Long result;
    try (Jedis j = JedisFactory.getInstance().getJedisPool().getResource()) {
      j.select(Configuration.CRAWLED_URLS_DB);
      result = j.dbSize();
    }
    return result;
  }

  public static List<FrontierQueue> getFrontQueues(List<String> seedUrls) {
    List<FrontierQueue> frontQueues = new ArrayList<>();
    boolean shouldSeed = false;
    try (Jedis j = JedisFactory.getInstance().getJedisPool().getResource()) {
      j.select(Configuration.FRONT_QUEUE_DB);
      var size = j.dbSize();
      if (j.dbSize() == 0) {

        shouldSeed = true;
      }
    }


    for (int i = 0; i < FRONT_QUEUE_COUNT; i++) {
      // this makes sure if the redis has pending urls, they get used
      FrontFrontierQueue curr = getFrontQueue(i);

      if (curr == null) {
        // add the seeds to the highest priority queue if needed
        curr = new FrontFrontierQueue(i);
        if (i == FRONT_QUEUE_COUNT - 1 && shouldSeed) {
          for (String url : seedUrls) {
            curr.addElement(new FrontierElement(0, url));
          }
        }
      }
      frontQueues.add(curr);
    }
    return frontQueues;
  }

  public static Map<String, FrontierElement> getCrawledElementsMap() {
    Map<String, FrontierElement> result = new HashMap<>();
    try (Jedis j = JedisFactory.getInstance().getJedisPool().getResource()) {
      j.select(Configuration.CRAWLED_URLS_DB);
      // this is an expensive operation but is only done once to retrieve all data
      Set<String> allKeys = j.keys("*");
      for(String key: allKeys) {
        result.put(key, Helper.JsonToObject(j.get(key), FrontierElement.class));
      }
    }
    return result;
  }


  public static Map<String, FrontierElement> getNewLinksFromAllLinksMap(int cursor, int count) {

    Map<String, FrontierElement> resultMap = new HashMap<>();
    try (Jedis j = JedisFactory.getInstance().getJedisPool().getResource()) {
      j.select(Configuration.ALL_URLS_DB);
      // fetch 2 keys in every scan
      ScanParams scanParams = new ScanParams().count(count);

      ScanResult<String> scanResult = j.scan(cursor, scanParams);
      scanResult.getResult().forEach((key) -> {
        resultMap.put(key, getFrontierElementFromUrl(key, ALL_URLS_DB));
      });

    }
    return resultMap;
  }

  public static void addToOutLinkQueue(Set<FrontierElement> elements) {
    try (Jedis j = JedisFactory.getInstance().getJedisPool().getResource()) {
      for(FrontierElement element: elements) {
        j.rpush(OUTLINKS_QUEUE, Helper.ObjectToJson(element));
      }
    }
  }

  public static FrontierElement pollElementFromOutLinkQueue() {
    try (Jedis j = JedisFactory.getInstance().getJedisPool().getResource()) {

      return Helper.JsonToObject(j.lpop(OUTLINKS_QUEUE), FrontierElement.class);
    }
  }

  public static FrontFrontierQueue getFrontQueue(int queueNumber) {
    FrontFrontierQueue result = null;
    try (Jedis j = JedisFactory.getInstance().getJedisPool().getResource()) {
      j.select(Configuration.FRONT_QUEUE_DB);
      result = Helper.JsonToObject(j.get(String.valueOf(queueNumber)), FrontFrontierQueue.class);
    }
    return result;
  }

  public static void setFrontQueue(int queueNumber, FrontFrontierQueue q) {
    try (Jedis j = JedisFactory.getInstance().getJedisPool().getResource()) {
      j.select(Configuration.FRONT_QUEUE_DB);
      j.set(String.valueOf(queueNumber), Helper.ObjectToJson(q));
    }
  }
}
