package retrieve;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import elasticsearch.ESClient;
import model.Document;
import model.Query;
import model.RetrievalResponse;

public class ESDefault extends RetrievalModel {
  private ESClient esClient;
  public ESDefault(String indexName, String modelName) {
    super(modelName);
    esClient = ESClient.getElasticSearchClient(indexName);
  }

  @Override
  public Map<String ,List<RetrievalResponse>> scoreDocuments(List<Document> docs, List<Query> queries) {
    Map<String, List<RetrievalResponse>> result = new HashMap<>();

    for(Query q: queries) {
      result.put(q.getId(), esClient.search(q));
    }
    writeRetrievalResult(result);
    return result;
  }
}
