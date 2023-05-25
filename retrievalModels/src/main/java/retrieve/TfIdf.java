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

public class TfIdf extends RetrievalModel {
  public TfIdf(String modelName) {
    super(modelName);
  }

  @Override
  public Map<String, List<RetrievalResponse>> scoreDocuments(List<Document> docs, List<Query> queries) {
    Map<String, Double> okapiData = null;
    Map<String, List<RetrievalResponse>> result = new HashMap<>();
    if(Helper.fileExists("okapiData")) {
      okapiData = Helper.loadOkapiScore("okapiData");
    }
    else {
      System.out.println("Okapi data missing, Run Okapi TF model before this");
      System.exit(0);
    }
    float docLen = docs.size();
    for(Query q: queries) {
      List<RetrievalResponse> retrieved = new ArrayList<>();
      String[] queryWords = q.getText().split(" ");
      for(Document doc: docs) {
        double score = 0;
        for(String word: queryWords) {
          double okapiwd = okapiData.getOrDefault(doc+"-"+word, 0.0);
          Term term = doc.getTerms().get(word);
          if(term == null) continue;
          score += okapiwd * Math.log(docLen/doc.getTerms().get(word).getDocFreq());
        }
        if(score == 0) continue;
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
}
