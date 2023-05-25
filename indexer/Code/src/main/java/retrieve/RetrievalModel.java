package retrieve;

import java.io.FileWriter;
import java.io.IOException;
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

public abstract class RetrievalModel {
  private String modelName;

  public RetrievalModel(String modelName) {
    this.modelName = modelName;
  }

  public abstract Map<String, List<RetrievalResponse>> scoreDocuments(List<Query> queries) throws IOException;

  public abstract double calculate(InvertedIndexTerm term, int... params);

  public List<RetrievalResponse> getRetrievedDocs(Map<String, List<InvertedIndexTerm>> queryInvertedIndexMap,
                                                  Map<Integer, String> globalIdDocMap, Map<String, Integer> globalDocIdLenMap,
                                                  int avgDocLen, Query q, int vocabLength) {
    String[] queryWords = q.getText().split(" ");
    List<InvertedIndexTerm> invertedIndexList = queryInvertedIndexMap.get(q.getId());
    Map<String, RetrievalResponse> retrievedMap = new HashMap<>();
    for (InvertedIndexTerm term : invertedIndexList) {
      int tfwq = q.getTermCountMap().get(term.getTerm());
      TreeMap<Integer, List<Integer>> docIdPositionMap = term.getDocIdPositionMap();
      for (var doc : docIdPositionMap.entrySet()) {
        Integer docShortId = doc.getKey();
        String docIntegerId = globalIdDocMap.get(docShortId);
        if (!retrievedMap.containsKey(docIntegerId)) {
          retrievedMap.put(docIntegerId, new RetrievalResponse(docIntegerId, q.getId(), 0));
        }
        RetrievalResponse currDoc = retrievedMap.get(docIntegerId);
        currDoc.addScore(calculate(term, globalDocIdLenMap.get(docIntegerId).intValue(),
                avgDocLen, tfwq,
                globalIdDocMap.size(), vocabLength, doc.getValue().size()));
      }
    }
    List<RetrievalResponse> result = new ArrayList<>(retrievedMap.values());
    result.sort(Comparator.comparingDouble(RetrievalResponse::getScore).reversed());
    return result;
  }

  void writeRetrievalResult(Map<String, List<RetrievalResponse>> retrievedDocs) {
    StringBuilder sb = new StringBuilder();
    for(Map.Entry<String, List<RetrievalResponse>> entry: retrievedDocs.entrySet()) {
      String queryId = entry.getKey();
      List<RetrievalResponse> responses = entry.getValue();
      for(int i = 0; i < responses.size(); i++) {
        String docId = responses.get(i).getDocId();
        double score = responses.get(i).getScore();
        sb.append(queryId).append(" ").append("Q0").append(" ").append(docId).append(" ").append(i).append(" ").append(score).append(" ").append("Exp").append("\n");
      }
    }
    try {
      FileWriter myWriter = new FileWriter(modelName + ".txt");
      myWriter.write(sb.toString());
      myWriter.close();
      System.out.println("Successfully wrote to the file.");
    } catch (IOException e) {
      System.out.println("An error occurred.");
      e.printStackTrace();
    }
  }

  String getModelName() {
    return modelName;
  }

}
