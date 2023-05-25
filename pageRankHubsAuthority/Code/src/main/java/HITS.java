import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.print.Doc;

import dataobjects.Document;
import elasticsearch.ESClient;

import static config.Configuration.INDEX_NAME;

public class HITS {

  private static Map<String, Document> getRootSet() {
    ESClient client = ESClient.getElasticSearchClient(INDEX_NAME, true);
    String query = "WORLD WAR II";
    return client.search(query, 1000);
  }

  private static Map<String, Document> getBaseSet(Map<String, Document> seedDocs) {
    int threshold = 200;
//    Map<String, Document> allDoc = getAllDocuments();
    Map<String, Document> baseSet = new HashMap<>();

    for (Document doc : seedDocs.values()) {
      for(String outLink: doc.getOutLinks()) {
        Document outLinkDoc = null;
        if(baseSet.containsKey(outLink)) {
          outLinkDoc = baseSet.get(outLink);
        }
        else {
          outLinkDoc = new Document(outLink);
        }
        outLinkDoc.addInLinks(doc.getDocid());
        baseSet.put(outLink, outLinkDoc);
      }
      if (doc.getInLinks().size() < threshold) {
        for (String inLink : doc.getInLinks()) {
          Document inLinkDoc = null;
          if(baseSet.get(inLink) != null) {
            inLinkDoc = baseSet.get(inLink);
          } else {
            inLinkDoc = new Document(inLink);
          }
          inLinkDoc.addOutLink(doc.getDocid());
          baseSet.put(inLink, inLinkDoc);
        }
      } else {
        baseSet.putAll(pickRandomInLinks(doc.getDocid(), threshold, doc.getInLinks(), baseSet));
      }
      if(baseSet.size() > 10000) break;
    }

    return  baseSet;
  }



  private static Map<String,? extends Document> pickRandomInLinks(String parentId, int threshold, List<String> inLinks ,Map<String, Document> baseSet) {
    Random random = new Random();
    Map<String, Document> result = new HashMap<>();
    while(result.size() < threshold) {
      String randomInLink = inLinks.get(random.nextInt(inLinks.size()));
      Document inLinkDoc = null;
      if(baseSet.containsKey(randomInLink)) {
        inLinkDoc = baseSet.get(randomInLink);
      }
      else {
        inLinkDoc = new Document(randomInLink);
      }
      inLinkDoc.addOutLink(parentId);
      result.put(randomInLink, inLinkDoc);
    }
    return result;
  }


  private static void computeHubsAndAuthorityScores(Map<String, Document> baseSet, Map<String, Document> rootSet) throws IOException {
    double threshold = 1e-4;
    int maxIterations = 700;

    // getting the values and storing them as a list
    // documents are seeked using the map and changes are then applied
    List<Document> baseList = new ArrayList<>(baseSet.values());
    List<Document> rootList = new ArrayList<>(rootSet.values());
    while(true) {

      for(Document doc: rootList) {
        double authorityScore = 0.0;
        List<String> inLinks = doc.getInLinks();
        for(String inLink : inLinks) {
          if(baseSet.get(inLink) == null) {
            continue;
          }
          authorityScore += baseSet.get(inLink).getHubScore();
        }
        doc.setNewAuthorityScore(authorityScore);
      }

      double[] newAuthorityScores = new double[rootList.size()];
      double[] authorityScores = new double[rootList.size()];
      int index = 0;
      for(Document doc: rootList) {
        authorityScores[index] = doc.getAuthorityScore();
        newAuthorityScores[index] = doc.getNewAuthorityScore();
        index++;
      }

      newAuthorityScores = normalize(newAuthorityScores);
      saveNewAuthorityScores(rootList, newAuthorityScores);

      for(Document doc: baseList) {
        double hubScore = 0.0;
        List<String> outLinks = doc.getOutLinks();
        for(String outLink: outLinks) {
          if(rootSet.get(outLink) == null) continue;
          hubScore += rootSet.get(outLink).getNewAuthorityScore();
        }
        doc.setNewHubScore(hubScore);
      }

      index = 0;
      double[] newHubScores = new double[baseSet.size()];
      double[] hubScores = new double[baseSet.size()];
      for(Document each: baseList) {
        hubScores[index] = each.getHubScore();
        newHubScores[index] = each.getNewHubScore();
        index++;
      }
      newHubScores = normalize(newHubScores);

      if(distance(authorityScores, newAuthorityScores) < threshold && distance(hubScores, newHubScores) < threshold) {
        break;
      }
      saveNewHubScores(baseList, newHubScores);
    }
    printScores(rootList, "authorityScores.txt", true, 500);
    printScores(baseList, "hubScores.txt", false, 500);
  }

  private static void saveNewHubScores(List<Document> baseList, double[] hubScores) {
    for(int i = 0; i < baseList.size(); i++) {
      Document doc = baseList.get(i);
      doc.setHubScore(hubScores[i]);
    }
  }

  private static void saveNewAuthorityScores(List<Document> rootList, double[] authorityScores) {
    for(int i = 0; i < rootList.size(); i++) {
      Document doc = rootList.get(i);
      doc.setAuthorityScore(authorityScores[i]);
    }
  }

  private static double[] normalize(double[] scores) {
    double sumSquares = 0.0;
    for (int i = 0; i < scores.length; i++) {
      sumSquares += Math.pow(scores[i], 2);
    }
    double norm = Math.sqrt(sumSquares);
    for(int i = 0; i < scores.length; i++) {
      scores[i] = scores[i]/norm;
    }
    return scores;
  }

  public static double distance(double[] x, double[] y) {
    double distance = 0.0;
    for (int i = 0; i < x.length; i++) {
      distance += Math.pow((x[i] - y[i]), 2);
    }
    distance = Math.sqrt(distance);
    return distance;
  }


  public static void orchestrateHITS() throws IOException {
    Map<String, Document> rootSet = getRootSet();
    Map<String, Document> baseSet = getBaseSet(rootSet);
    computeHubsAndAuthorityScores(baseSet, rootSet);
  }

  private static void printScores(List<Document> docs, String fileName, boolean isAuthority, int printCount) throws IOException {
    BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
    String largeSpace = " ".repeat(50);

    if(isAuthority) {
      docs.sort((a, b) -> Double.compare(b.getAuthorityScore(), a.getAuthorityScore()));
    }
    else {
      docs.sort((a, b) -> Double.compare(b.getHubScore(), a.getHubScore()));
    }
    writer.write("Page" + largeSpace + "Score\n");
    for (Document doc : docs.subList(0, printCount)) {
      StringBuilder sb = new StringBuilder();
      sb.append(doc.getDocid() + "  ");
      if(isAuthority) {
        sb.append(doc.getAuthorityScore() + largeSpace + "\n");
      }
      else {
        sb.append(doc.getHubScore() + largeSpace + "\n");
      }
      writer.write(sb.toString());
    }
    writer.close();
  }
}
