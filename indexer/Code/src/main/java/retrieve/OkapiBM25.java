package retrieve;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import model.InvertedIndexTerm;
import model.Query;
import model.RetrievalResponse;
import util.Helper;

public class OkapiBM25 extends RetrievalModel {
  private final Map<Integer, String> globalIdDocMap;
  private Map<String, Integer> globalDocIdLenMap;
  private int avgDocLen;
  private TreeMap<String, Integer> globalTermIdMap;
  private Map<String, Integer> globalDocIdMap;
  private TreeMap<Integer, String> globalIdTermMap;
  private Map<Integer, String> idToFileMap;
  private Map<String, List<InvertedIndexTerm>> queryInvertedIndexMap;

  public OkapiBM25(String indexName, int avgDocLen, String modelName, TreeMap<String, Integer> globalTermIdMap,
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
  public Map<String, List<RetrievalResponse>> scoreDocuments(List<Query> queries) throws IOException {
    Map<String, List<RetrievalResponse>> result = new HashMap<>();
    for (Query q : queries) {
      List<RetrievalResponse> retrieved = getRetrievedDocs(queryInvertedIndexMap, globalIdDocMap,
              globalDocIdLenMap, avgDocLen, q, globalIdTermMap.size());
      if (retrieved.size() > 1000) {
        result.put(q.getId(), retrieved.subList(0, 1000));
      } else {
        result.put(q.getId(), retrieved);
      }
    }
    writeRetrievalResult(result);
    return result;
  }

  public double calculate(InvertedIndexTerm term, int... params) {
    // TODO: needs to be changed on the bases of the change in how data is fetched for each term
    int docLen = params[0];
    int avgDocLen = params[1];
    int tfwq = params[2];
    int totalDocs = params[3];
    int dfw = term.getDocFreq();
    int tfwd = term.getTotalTernFreq();
    double k1 = 1.2;
    double k2 = 600;
    double b = 0.75;
    double val = Math.log((totalDocs + 0.5) / (dfw + 0.5)) *
            ((tfwd + k1 * tfwd) / (tfwd + k1 * ((1 - b) + (b * (docLen) / avgDocLen)))) *
            ((tfwq + (k2 * tfwq)) / (tfwq + k2));

    return val;
  }
}
