package retrieve;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.Document;
import model.Query;
import model.RetrievalResponse;
import model.Term;
import util.Helper;

public class OkapiBM25 extends RetrievalModel {
  private int avgDocLen;
  public OkapiBM25(int avgDocLen, String modelName) {
    super(modelName);
    this.avgDocLen = avgDocLen;
  }

  @Override
  public Map<String, List<RetrievalResponse>> scoreDocuments(List<Document> docs, List<Query> queries) {
    Map<String, List<RetrievalResponse>> result = new HashMap<>();
    int docLen = docs.size();
    for(Query q: queries) {
      List<RetrievalResponse> retrieved = new ArrayList<>();
      String[] queryWords = q.getText().split(" ");
      for(Document doc: docs) {
        double score = 0;
        for(String word: queryWords) {
          int tfwq = q.getTermCountMap().get(word);
          score += calculateBM25(doc, word, docLen, avgDocLen, tfwq);
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
    writeRetrievalResult(result);
    return result;
  }

  private double calculateBM25(Document doc, String word, int docLen, int avgDocLen, int tfwq) {
    Map<String, Term> terms = doc.getTerms();
    Term currTerm = terms.get(word);

    if(currTerm == null) return 0.0;

    int dfw = currTerm.getDocFreq();
    int tfwd = currTerm.getTermFreq();
    double k1 = 1.2;
    double k2 = 600;
    double b = 0.75;
    double val =  Math.log((docLen + 0.5)/(dfw + 0.5)) *
            ((tfwd + k1 * tfwd)/(tfwd + k1 * ((1-b) + (b * (doc.getLength())/avgDocLen)))) *
            ((tfwq + (k2 * tfwq))/(tfwq + k2));

    return val;
  }
}
