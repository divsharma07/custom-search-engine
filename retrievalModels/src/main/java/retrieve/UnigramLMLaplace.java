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

public class UnigramLMLaplace extends RetrievalModel {
  private int vocabLength;
  public UnigramLMLaplace(String modelName, int vocabLength) {
    super(modelName);
    this.vocabLength = vocabLength;
  }

  @Override
  public Map<String, List<RetrievalResponse>> scoreDocuments(List<Document> docs, List<Query> queries) {
    Map<String, List<RetrievalResponse>> result = new HashMap<>();
    for(Query q: queries) {
      List<RetrievalResponse> retrieved = new ArrayList<>();
      String[] queryWords = q.getText().split(" ");
      for(Document doc: docs) {
        double score = 0;
        for(String word: queryWords) {
          score += calculateUnigramLMLaplace(doc, word, vocabLength);
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

  private double calculateUnigramLMLaplace(Document doc, String word, int vocabLength) {
    Term currTerm = doc.getTerms().get(word);
    //penalizing the absence of a term heavily
    if(currTerm == null) return -1200.0;
    int twd = currTerm.getTermFreq();
    int docLen = doc.getLength();

    return Math.log10((twd + 1)*1.0/(docLen + vocabLength));
  }
}
