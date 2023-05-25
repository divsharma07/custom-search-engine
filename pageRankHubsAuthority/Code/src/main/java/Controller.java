import java.io.IOException;

import elasticsearch.ESClient;

import static config.Configuration.INDEX_NAME;

/**
 * This is the controller class that orchestrates all functionalities.
 * NOTE: Locally running redis is a hard dependency for the code to run.
 */
public class Controller {

  public static void main(String[] args) throws IOException {

    // gets all pages from ES and saves them in redis
    ESClient.getElasticSearchClient(INDEX_NAME, true).getAllPagesFromES();
    PageRank.computePageRankMergedIndex();
    PageRank.computePageRankWt2g();
    HITS.orchestrateHITS();
  }


}
