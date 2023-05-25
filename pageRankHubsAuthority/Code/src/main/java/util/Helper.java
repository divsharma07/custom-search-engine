package util;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.print.Doc;

import dataobjects.Document;

import static config.Configuration.W2G_FILE_NAME;


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

  public static Map<String, Document> getAllDocumentsW2G() throws IOException {
    Map<String, Document> map = new HashMap<>();
    BufferedReader reader = new BufferedReader(new FileReader(W2G_FILE_NAME));
    String line = "";
    while ((line = reader.readLine()) != null) {
      String[] split = line.split(" ");
      if (split.length == 1) continue;
      Set<String> inLinks = new HashSet<>(Arrays.asList(split).subList(1, split.length));
      map.put(split[0], new Document(split[0], new ArrayList<>(inLinks)));
    }

    return map;
  }

  // this is required because crawled data is a mix of list of strings and comma seperated links
  public static Set<String> processLinks(List<String> links) {
    Set<String> processedLinks = new HashSet<>();
    for (String link : links) {
      // this is done because some of the inLinks are concatenated, instead of being in the list form
      String[] splitLinks = link.split(",");

      for(String splitLink: splitLinks) {
        processedLinks.add(splitLink.trim());
      }
    }
    return processedLinks;
  }

  public static void setOutLinks(Map<String, Document> docs) throws IOException {
    for (Map.Entry<String, Document> each : docs.entrySet()) {
      Document doc = each.getValue();
      List<String> inLinks = doc.getInLinks();
      for (String inLink : inLinks) {
        if (docs.get(inLink) == null) continue;
        docs.get(inLink).addOutLink(doc.getDocid());
      }
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
}