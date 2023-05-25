package util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dataobjects.Document;
import redis.JedisFactory;
import redis.clients.jedis.Jedis;

import static config.Configuration.DOCS_DB_1;
import static config.Configuration.SINK_DOCS_DB;
import static config.Configuration.SINK_DOCS_LIST;
import static util.Helper.JsonToObject;
import static util.Helper.processLinks;

/**
 * All the redis related helper methods.
 * Mainly deal with putting and retrieving key values and polling and adding to queues.
 */
public class RedisHelper {


  public static void addDocumentToRedis(Document doc) {
    try (Jedis j = JedisFactory.getInstance().getJedisPool().getResource()) {
      j.select(DOCS_DB_1);
      if (j.get(doc.getDocid()) == null) {
        j.set(doc.getDocid(), Helper.ObjectToJson(doc));
      }
    }
  }

  public static Document getDocumentById(String docId) {
    Document d = null;
    try (Jedis j = JedisFactory.getInstance().getJedisPool().getResource()) {
      j.select(DOCS_DB_1);
      d = JsonToObject(j.get(docId), Document.class);
    }
    return d;
  }

  public static Map<String, Document> getAllDocuments() {
    Map<String, Document> result = new HashMap<>();
    try (Jedis j = JedisFactory.getInstance().getJedisPool().getResource()) {
      j.select(DOCS_DB_1);
      // this is an expensive operation but is only done once to retrieve all data
      Set<String> allKeys = j.keys("*");
//      int count = 0;
      for(String key: allKeys) {
//        if(count == 50000) break;
        Document fetchedDoc = getDocumentById(key);
        Set<String> processedInLinks = processLinks(fetchedDoc.getInLinks());
        int outLinksCount = processLinks(fetchedDoc.getOutLinks()).size();
//        count++;
        result.put(key.trim(), new Document(key, processedInLinks, outLinksCount));
      }
    }
    return result;
  }



  public static List<String> getAllDocumentsWithNoOutLinksWt2g(Map<String, Document> docsWt2g) {
      List<String> result = new ArrayList<>();

      for(Map.Entry<String, Document> each: docsWt2g.entrySet()) {
        if(each.getValue().getOutLinksCount() == 0) {
          result.add(each.getKey());
        }
      }

      return result;
  }

  public static List<String> getAllDocumentsIdsWithNoOutLinks() {
    List<String> result = new ArrayList<>();

    try (Jedis j2 = JedisFactory.getInstance().getJedisPool().getResource()) {
      j2.select(SINK_DOCS_DB);
      if(j2.dbSize() != 0) {
        return j2.lrange(SINK_DOCS_LIST, 0, -1);
      }
    }
    try (Jedis j = JedisFactory.getInstance().getJedisPool().getResource()) {
      j.select(DOCS_DB_1);
      // this is an expensive operation but is only done once to retrieve all data
      Set<String> allKeys = j.keys("*");
      try (Jedis j2 = JedisFactory.getInstance().getJedisPool().getResource()) {
        j2.select(SINK_DOCS_DB);
        for (String key : allKeys) {
          Document doc = Helper.JsonToObject(j.get(key), Document.class);
          if (doc.getOutLinks().size() == 0) {
            result.add(doc.getDocid());
            j2.rpush(SINK_DOCS_LIST, doc.getDocid());
          }
        }
      }
    }
    return result;
  }

  public static void documentExists(Document doc) {
    try (Jedis j = JedisFactory.getInstance().getJedisPool().getResource()) {
      j.select(DOCS_DB_1);
      j.set(doc.getDocid(), Helper.ObjectToJson(doc));
    }
  }

}
