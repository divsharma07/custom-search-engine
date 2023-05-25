package retrieve;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import elasticsearch.ESClient;
import model.Document;
import model.Query;
import model.RetrievalResponse;
import model.Term;
import util.Helper;

public class OkapiTf extends RetrievalModel {
  private ESClient esClient;
  private int avgDocLen;
  public OkapiTf(String indexName, int avgDocLen, String modelName) {
    super(modelName);
    this.avgDocLen = avgDocLen;
    esClient = ESClient.getElasticSearchClient(indexName);
  }

  @Override
  public Map<String, List<RetrievalResponse>> scoreDocuments(List<Document> docs, List<Query> queries) {
    Map<String, List<RetrievalResponse>> result = new HashMap<>();
    Map<String, Double> okapiScore = new HashMap<>();
    for(Query q: queries) {
      List<RetrievalResponse> retrieved = new ArrayList<>();
      String[] queryWords = q.getText().split(" ");
      for(Document doc: docs) {
        double score = 0;
        for(String word: queryWords) {
          double okapiwd = calculateOkapiScore(doc, word);
          okapiScore.put(doc+"-"+word, okapiwd);
          score += calculateOkapiScore(doc, word);
        }
        if(Double.compare(score, 0.0) == 0) continue;
        retrieved.add(new RetrievalResponse(doc, q.getId(), score));
      }
      retrieved.sort(Comparator.comparingDouble(RetrievalResponse::getScore).reversed());
      if(retrieved.size() > 1000) {
        result.put(q.getId(), retrieved.subList(0, 1000));
      }
      else {
        result.put(q.getId(), retrieved);
      }
    }
    Helper.saveOkapiScore(okapiScore, "okapiData");
    writeRetrievalResult(result);
    return result;
  }

  private double calculateOkapiScore(Document doc, String word) {
    Map<String, Term> terms = doc.getTerms();
    Term currTerm = terms.get(word);
    if(currTerm == null) return 0;

    int termFreq = terms.get(word).getTermFreq();
    int docLen = doc.getLength();
    double val = termFreq/(termFreq + 0.5 + 1.5 * (docLen/avgDocLen));
    return val;
  }
}
