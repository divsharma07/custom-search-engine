package util;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import dataobjects.Document;
import frontier.FrontierElement;
import static config.Configuration.CRAWLED_URLS_DB;
import static util.RedisHelper.getFrontierElementFromUrl;

/**
 * Helper methods
 */
public class Helper {
  public static <T> String ObjectToJson(T object) {
    Gson gson = new Gson();
    return gson.toJson(object);
  }

  public static int randomBiased(int low, int high, double bias) {
    double r = new Random().nextDouble();    // random between 0 and 1
    r = Math.pow(r, bias);
    return (int) (low + (high - low) * r);
  }

  public static void initDirectory(String multipartLocation) throws IOException {
    Path storageDirectory = Paths.get(multipartLocation);
    if (!Files.exists(storageDirectory)) {
      Files.createDirectory(storageDirectory);
    }
  }
  public static void main(String[] args) {
    Map<Integer, Integer> map = new HashMap<>();
    for (int i = 0; i < 5000; i++) {
      int r = randomBiased(0, 5, 0.4);
      int curr = map.getOrDefault(r, 0);
      map.put(r, curr + 1);
    }
    System.out.println(map);
  }

  public static <T> T JsonToObject(String json, Class<T> c) {
    return new Gson().fromJson(json, c);
  }

  public static Set<Document> parseDocs(String path) throws IOException {
    List<Document> docs = new ArrayList<>();
    File directoryFolder = new File(path);
    File[] files = directoryFolder.listFiles();
    for (File file : files) {
      BufferedReader fileReader = new BufferedReader(new FileReader(file));

      String line = "";
      String docId = "";
      String content = "";
      String title = "";
      while ((line = fileReader.readLine()) != null) {
        if (line.contains("<DOCNO>")) {
          if (!docId.equals("")) {
            Document doc = new Document(docId, content, title);
            setLinks(doc);
            docs.add(doc);
          }
          content = "";
          title = "";
          docId = removeHtmlElement(line, "DOCNO");
        }
        if (line.contains("<TEXT>")) {
          // contents are all in a single line
          content = removeHtmlElement(line, "TEXT");
        }
        if(line.contains("<HEAD>")) {
          // contents are all in a single line
          title = removeHtmlElement(line, "HEAD");
        }
      }

      if (!docId.equals("") && content.length() > 0) {
        Document doc = new Document(docId, content, title);
        setLinks(doc);
        docs.add(doc);
      }
    }
    return new HashSet<>(docs);
  }

  private static void setLinks(Document doc) {
    FrontierElement element = getFrontierElementFromUrl(doc.getDocid(), CRAWLED_URLS_DB);

    if(element == null) {
      throw new IllegalStateException("Encountered an uncrawled document in the list");
    }
    doc.setInLinks(element.getInlinks());
    doc.setOutLinks(element.getOutlinks());
  }

  private static String removeHtmlElement(String content, String element) {
    String startTag = String.format("<%s>", element);
    String closeTag = String.format("</%s>", element);
    return content.replace(startTag, "").replace(closeTag, "").trim();
  }
}