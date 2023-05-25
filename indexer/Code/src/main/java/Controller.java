import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import index.CustomIndexer;
import index.Indexer;
import model.InvertedIndexTerm;
import model.Query;
import parse.QueryParser;
import retrieve.OkapiBM25;
import retrieve.OkapiTf;
import retrieve.RetrievalModel;
import util.Helper;


public class Controller {

  private static String indexName = "hw1docsindex";

  public static void main(String[] args) throws IOException {
    boolean isStemmed = Boolean.parseBoolean(args[0]);
    int threadCount = Integer.parseInt(args[1]);
    List<Query> queries;
    ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
    String collectionDirectory = new File("").getAbsolutePath() + "/IR_data/AP_DATA/ap89_collection";
    Indexer indexer = new CustomIndexer(indexName, isStemmed, executorService, collectionDirectory);
    QueryParser queryParser = new QueryParser("query_desc.51-100.short.txt", isStemmed);

    if (!indexer.indexExists()) {
      indexer.populateIndexAndMetadata();
      System.out.println("Indexing complete");
    }
    TreeMap<String, Integer> globalTermIdMap = indexer.getTermIdMap();
    Map<String, Integer> globalDocIdMap = indexer.getDocIdMap();
    TreeMap<Integer, String> globalIdTermMap = indexer.getIdTermMap();
    Map<Integer, String> idToFileMap = indexer.getIdToFileMap();
    Map<Integer, String> globalIdDocMap =
            new TreeMap<>(globalDocIdMap.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey)));
    Map<String, Integer> globalDocIdLenMap = indexer.getDocIdLenMap();
    // load all auxiliary files except index files themselves

    int vocabLength = indexer.getVocabulary().size();
    int avgDocLen = indexer.getAvgDocLength();
    queries = queryParser.parseQueries();
    runRetrievalModels(queries, vocabLength, avgDocLen, globalTermIdMap, globalDocIdMap, globalIdTermMap, indexer,
            idToFileMap, globalIdDocMap, globalDocIdLenMap, isStemmed);
    executorService.shutdown();
    indexer.close();
  }


  // TODO fix the whole situation related to how the search would actually work
  private static void runRetrievalModels(List<Query> queries, int avgDocLength, int vocabLength,
                                         TreeMap<String, Integer> globalTermIdMap, Map<String, Integer> globalDocIdMap,
                                         TreeMap<Integer, String> globalIdTermMap, Indexer indexer,
                                         Map<Integer, String> idToFileMap, Map<Integer, String> globalIdDocMap,
                                         Map<String, Integer> globalDocIdLenMap, boolean isStemmed) throws IOException {
    Map<String, List<InvertedIndexTerm>> queryInvertedIndexMap = null;
    String stemmedPrefix = isStemmed ? "-stemmed" : "";
    queryInvertedIndexMap = Helper.getInvertedIndexMapForQuery(queries, indexer);

    RetrievalModel model = new OkapiBM25(indexName, avgDocLength, "OkapiBM25" + stemmedPrefix,
            globalTermIdMap, globalDocIdMap, globalIdTermMap, idToFileMap, queryInvertedIndexMap, globalIdDocMap, globalDocIdLenMap);
    model.scoreDocuments(queries);
    model = new OkapiTf(indexName, avgDocLength, "OkapiTf" + stemmedPrefix,
            globalTermIdMap, globalDocIdMap, globalIdTermMap, idToFileMap, queryInvertedIndexMap, globalIdDocMap, globalDocIdLenMap);
    model.scoreDocuments(queries);
    model = new OkapiTf(indexName, avgDocLength, "UnigramLMLaplace" + stemmedPrefix,
            globalTermIdMap, globalDocIdMap, globalIdTermMap, idToFileMap, queryInvertedIndexMap, globalIdDocMap, globalDocIdLenMap);
    model.scoreDocuments(queries);
  }
}
