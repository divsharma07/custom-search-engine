import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Controller {

  private static String DATA_DIRECTORY_PATH = "trec07p/trec07p/data";

  private static String INDEX_NAME = "hw7index";

  private static String NGRAMS_FILE = "spam_words.txt";

  public static void main(String[] args) throws Exception {
    Parser parser = new Parser();
    Map<String, ParsedDocument> docs = null;
    try {
      docs = parser.parseHtml(DATA_DIRECTORY_PATH);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    ESClient esClient = ESClient.getElasticSearchClient(INDEX_NAME);
    if (!esClient.indexExists(INDEX_NAME)) {
      esClient.createIndex(INDEX_NAME);
      esClient.populateIndex(new ArrayList<>(docs.values()));
    }


    Map<String, Integer> uniqueWords = esClient.getAllUniqueWords("content", docs.size());
    Set<String> nGrams = Helper.getCustomNgrams();
    // Part1A
    Classifier1 classifier1A = new Classifier1(esClient, new ArrayList<>(nGrams), "Part1A");
    classifier1A.orchestrateSpamDetection(docs);

    // Part1B
    nGrams = Helper.getNgramsFromFile(NGRAMS_FILE);
    Classifier1 classifier1B = new Classifier1(esClient, new ArrayList<>(nGrams), "Part1B");
    classifier1B.orchestrateSpamDetection(docs);

    // Part2
    Classifier2 classifier2 = new Classifier2(esClient, uniqueWords, "Part2");
    classifier2.orchestrateSpamDetection(docs);
    esClient.close();
  }
}
