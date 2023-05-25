package retrieve;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import model.Document;
import model.InvertedIndexTerm;
import model.Query;
import model.RetrievalResponse;

public class UnigramLMLaplace extends RetrievalModel {
  private final Map<Integer, String> globalIdDocMap;
  private Map<String, Integer> globalDocIdLenMap;
  private int avgDocLen;
  private TreeMap<String, Integer> globalTermIdMap;
  private Map<String, Integer> globalDocIdMap;
  private TreeMap<Integer, String> globalIdTermMap;
  private Map<Integer, String> idToFileMap;
  private Map<String, List<InvertedIndexTerm>> queryInvertedIndexMap;

  public UnigramLMLaplace(String indexName, int avgDocLen, String modelName, TreeMap<String, Integer> globalTermIdMap,
                   Map<String, Integer> globalDocIdMap, TreeMap<Integer, String> globalIdTermMap,
                   Map<Integer, String> idToFileMap, Map<String, List<InvertedIndexTerm>> queryInvertedIndexMap,
                   Map<Integer, String> globalIdDocMap, Map<String, Integer> globalDocIdLenMap) {
    super(modelName);
    this.avgDocLen = avgDocLen;
    this.globalTermIdMap = globalTermIdMap;
    this.globalDocIdMap = globalDocIdMap;
    this.globalIdTermMap = globalIdTermMap;
    this.idToFileMap = idToFileMap;
    this.queryInvertedIndexMap = queryInvertedIndexMap;
    this.globalIdDocMap = globalIdDocMap;
    this.globalDocIdLenMap = globalDocIdLenMap;
  }

  @Override
  public Map<String, List<RetrievalResponse>> scoreDocuments(List<Query> queries) {
    Map<String, List<RetrievalResponse>> result = new HashMap<>();
    for (Query q : queries) {
      List<RetrievalResponse> retrieved = getRetrievedDocs(queryInvertedIndexMap, globalIdDocMap,
              globalDocIdLenMap, avgDocLen, q, globalIdTermMap.size());
      retrieved.sort(Comparator.comparingDouble(RetrievalResponse::getScore).reversed());
      if (retrieved.size() > 1000) {
        result.put(q.getId(), retrieved.subList(0, 1000));
      } else {
        result.put(q.getId(), retrieved);
      }
    }
    writeRetrievalResult(result);
    return result;
  }

  @Override
  public double calculate(InvertedIndexTerm term, int... params) {
    int twd = params[5];
    int docLen = params[0];
    int vocabLength = params[4];
    return Math.log10((twd + 1)*1.0/(docLen + vocabLength));
  }
}
