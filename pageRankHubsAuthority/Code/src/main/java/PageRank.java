import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import dataobjects.Document;

import static util.Helper.getAllDocumentsW2G;
import static util.Helper.setOutLinks;
import static util.RedisHelper.getAllDocuments;
import static util.RedisHelper.getAllDocumentsIdsWithNoOutLinks;
import static util.RedisHelper.getAllDocumentsWithNoOutLinksWt2g;

public class PageRank {

  private static int perplexityMatchCount = 0;

  public static List<Document> computePageRank(int n, double d, Map<String, Document> docs, List<String> sinkDocs) {
    // initialize the transition matrix
    double threshold = 1e-6;
    for (Map.Entry<String, Document> each : docs.entrySet()) {
      each.getValue().setPageRank(1.0 / n);
    }


    while (true) {
      double sinkPR = 0.0;
      for (String sinkDoc : sinkDocs) {

        //TODO remove this
        if(docs.get(sinkDoc) != null) {
          sinkPR += docs.get(sinkDoc).getPageRank();
        }
      }
      for (Map.Entry<String, Document> each : docs.entrySet()) {
        double newPR = (1 - d) / n;
        newPR += d * sinkPR / n;

        Set<String> inLinks = new HashSet<>(each.getValue().getInLinks());


        for (String inLink : inLinks) {
          if(sinkDocs.contains(inLink)) continue;
          if (docs.get(inLink.trim()) == null) continue;
          Document inLinkDoc = docs.get(inLink.trim());
          int outLinkCount = inLinkDoc.getOutLinksCount();
          if(outLinkCount == 0) continue;
          newPR += d * inLinkDoc.getPageRank() / outLinkCount;
        }
        each.getValue().setNewPageRank(newPR);
      }

      if (hasPerplexitySimilarityLimitReached(docs)) {
        break;
      }

      for (Map.Entry<String, Document> each : docs.entrySet()) {
        double newPR = each.getValue().getNewPageRank();
        each.getValue().setPageRank(newPR);
      }
    }
    return new ArrayList<>(docs.values());
  }

  private static boolean hasPerplexitySimilarityLimitReached(Map<String, Document> docs) {
    double newPerplexity = perplexity(docs.values().stream().flatMap(o -> Stream.of(o.getNewPageRank()))
            .toArray(Double[]::new));
    double oldPerplexity = perplexity(docs.values().stream().flatMap(o -> Stream.of(o.getPageRank()))
            .toArray(Double[]::new));
    if (Math.floor(newPerplexity) == Math.floor(oldPerplexity)) {
      perplexityMatchCount++;
    } else {
      perplexityMatchCount = 0;
    }

    return perplexityMatchCount == 4;
  }


  private static double perplexity(Double[] arr) {
    double sum = 0.0;
    for (double digit : arr) {
      sum += digit;
    }
    double entropy = 0.0;
    for (double digit : arr) {
      double p = digit / sum;
      entropy -= p * Math.log(p) / Math.log(2);
    }
    return Math.pow(2, entropy);
  }

  public static void computePageRankMergedIndex() throws IOException {
    Map<String, Document> docs = getAllDocuments();
    List<String> sinkDocs = getAllDocumentsIdsWithNoOutLinks();
    List<Document> docsWithPageRanks = PageRank.computePageRank(docs.size(), 0.85, docs, sinkDocs);
    docsWithPageRanks.sort((a, b) -> Double.compare(b.getPageRank(), a.getPageRank()));
    writePageRankResult("mergedDocPageRank.txt", docsWithPageRanks.subList(0, 501));
  }

  public static void computePageRankWt2g() throws IOException {
    Map<String, Document> docs = getAllDocumentsW2G();
    setOutLinks(docs);
    List<String> sinkDocs = getAllDocumentsWithNoOutLinksWt2g(docs);
    List<Document> docsWithPageRanks = PageRank.computePageRank(docs.size(), 0.85, docs, sinkDocs);
    docsWithPageRanks.sort((a, b) -> Double.compare(b.getPageRank(), a.getPageRank()));
    writePageRankResult("wt2gPageRank.txt", docsWithPageRanks.subList(0, 501));
  }

  private static void writePageRankResult(String s, List<Document> docs) throws IOException {
    BufferedWriter writer = new BufferedWriter(new FileWriter(s));
    String largeSpace = " ".repeat(50);
    String smallSpace = " ".repeat(10);
    writer.write("Page" + largeSpace + "PageRank" + largeSpace + "OutLinksCount" + smallSpace + "InLinksCount\n");
    for (Document doc : docs) {
      StringBuilder sb = new StringBuilder();
      sb.append(doc.getDocid()).append("  ").append(doc.getPageRank()).append("  ")
              .append(doc.getOutLinksCount()).append("  ")
              .append(doc.getInLinks().size()).append("\n");
      writer.write(sb.toString());
    }
    writer.close();
  }
}