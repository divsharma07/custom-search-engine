import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dataobjects.Document;
import elasticsearch.ESClient;
import frontier.Frontier;

import static config.Configuration.CRAWLED_DOCS_DIR;
import static config.Configuration.INDEX_NAME;
import static util.Helper.parseDocs;


/**
 * This is the controller class that orchestrates all functionalities.
 * NOTE: Locally running redis is a hard dependency for the code to run.
 */
public class Controller {

  public static void main(String[] args) throws IOException {
    int threads = Runtime.getRuntime().availableProcessors();
    // set the second parameter to false to run ES locally
    ESClient esClient = ESClient.getElasticSearchClient(INDEX_NAME, true);
    ExecutorService executorService = Executors.newFixedThreadPool(threads);
    List<String> seedUrls = new ArrayList<>(List.of("https://www.britannica.com/event/World-War-II",
            "https://en.wikipedia.org/wiki/World_War_II", "https://en.wikipedia.org/wiki/List_of_World_War_II_battles_involving_the_United_States",
            "https://en.wikipedia.org/wiki/Military_history_of_the_United_States_during_World_War_II",
            "https://www.nps.gov/perl/learn/historyculture/pacific-battles.htm"));
    Frontier frontier = new Frontier(seedUrls, executorService);
    frontier.orchestrate();

    // write inlinks and outlinks to file
    frontier.writeLinkGraphsToFile();

    // get all docs that have been crawled
    Set<Document> docs = parseDocs(CRAWLED_DOCS_DIR);
    if (!esClient.indexExists(INDEX_NAME)) {
      esClient.createIndex(INDEX_NAME);
    }
    esClient.populateIndex(docs);
    System.out.println("Indexing complete");
    executorService.shutdownNow();
    System.out.println("Crawling and Indexing complete");
  }
}
