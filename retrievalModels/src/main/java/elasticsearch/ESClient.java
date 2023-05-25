package elasticsearch;

import org.apache.http.HttpHost;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.TermVectorsRequest;
import org.elasticsearch.client.core.TermVectorsResponse;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.DateFieldScript;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.SignificantTerms;
import org.elasticsearch.search.aggregations.bucket.terms.SignificantTermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xcontent.XContentType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import model.Document;
import model.Query;
import model.RetrievalResponse;
import model.Term;


public class ESClient {

  private RestHighLevelClient  restClient = null;
  private static ESClient esClient = null;
  private String indexName;
  private int avgDocLength;
  private ESClient(String indexName) {
    restClient = new RestHighLevelClient(
            RestClient.builder(
                    new HttpHost("localhost", 9200, "http")));
    this.indexName = indexName;
  }

  public RestHighLevelClient getClient() {
    return this.restClient;
  }

  public static ESClient getElasticSearchClient(String indexName) {
    if(esClient == null) {
      esClient = new ESClient(indexName);
    }
    return esClient;
  }

  public void populateIndex(List<Document> docs) {

    for (Document curr : docs) {
      Map<String, Object> jsonMap = new HashMap<>();
      jsonMap.put("content", curr.getContent());
      IndexRequest indexRequest = new IndexRequest(indexName)
              .id(curr.getId()).source(jsonMap);
      try {
        restClient.index(indexRequest, RequestOptions.DEFAULT);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void close() {
    try {
      restClient.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public boolean indexExists(String indexName) {
    GetIndexRequest existsRequest = new GetIndexRequest(indexName);
    boolean exists = false;
    try {
      exists = restClient.indices().exists(existsRequest, RequestOptions.DEFAULT);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return exists;
  }

  public void createIndex(String indexName) {
    CreateIndexRequest indexRequest = new CreateIndexRequest(indexName);
    indexRequest.source("{\"settings\":{\"number_of_shards\":1,\"number_of_replicas\":1,\"analysis\":{\"filter\":{\"english_stop\":{\"type\":\"stop\",\"stopwords_path\":\"my_stoplist.txt\"}}}},\"mappings\":{\"properties\":{\"content\":{\"type\":\"text\",\"fielddata\":true,\"index_options\":\"positions\",\"term_vector\":\"yes\"}}}}",
            XContentType.JSON);
    try {
      restClient.indices().create(indexRequest, RequestOptions.DEFAULT);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  public List<RetrievalResponse> search(Query query) {
    List<RetrievalResponse> result = new ArrayList<>();
    SearchRequest searchRequest = new SearchRequest(indexName);
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    searchSourceBuilder.size(1000);
    searchSourceBuilder.query(QueryBuilders.matchAllQuery());
    searchRequest.source(searchSourceBuilder);
    QueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("content", query.getText());
    searchSourceBuilder.query(matchQueryBuilder);
    try {
      SearchResponse searchResponse = restClient.search(searchRequest, RequestOptions.DEFAULT);
      SearchHits hits = searchResponse.getHits();
      for(SearchHit hit: hits) {
        result.add(new RetrievalResponse(hit.getId(), query.getId(), hit.getScore()));
      }
      System.out.println(searchResponse);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return result;
  }

  public int getAvgDocLength() {
    SearchRequest searchRequest = new SearchRequest(indexName);
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    searchSourceBuilder.query(new MatchAllQueryBuilder());

    AggregationBuilder avgBuilder = AggregationBuilders.avg("avg_length");
    searchSourceBuilder.aggregation(avgBuilder);
    searchRequest.source(searchSourceBuilder);

    SearchResponse response = null;
    try {
      response = restClient.search(searchRequest, RequestOptions.DEFAULT);
    } catch (IOException e) {
      e.printStackTrace();
    }

    assert response != null;
    Aggregations aggregations = response.getAggregations();
    Map<String, Aggregation> aggregationMap = aggregations.getAsMap();
    Avg avg = (Avg) aggregationMap.get("avg_length");

    return (int)avg.getValue();
  }


  public List<String> getSignificantQueryTerms(Set<String> terms) {
    List<String> result = new ArrayList<>();

    for(String term: terms) {
      SearchRequest searchRequest = new SearchRequest(indexName);
      SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
      searchSourceBuilder.query(QueryBuilders.termQuery("content", term));
      SignificantTermsAggregationBuilder sigTermsAgg = AggregationBuilders
              .significantTerms("significant_terms")
              .field("content")
              .minDocCount(2); // minimum number of documents a term must appear in

      searchSourceBuilder.aggregation(sigTermsAgg);
      searchRequest.source(searchSourceBuilder);

      SearchResponse searchResponse = null;
      try {
        searchResponse = restClient.search(searchRequest, RequestOptions.DEFAULT);
      } catch (IOException e) {
        e.printStackTrace();
      }
      SignificantTerms sigTerms = searchResponse.getAggregations().get("significant_terms");

      for (SignificantTerms.Bucket entry : sigTerms.getBuckets()) {
        if(entry.getKeyAsString().equals(term)) continue;
        result.add(entry.getKeyAsString());
      }
    }
    return result;
  }



// Borrowed from doc: https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/java-rest-high-document-term-vectors.html
  public Map<String, Term> getDocTermInfo(String docId, int totalDocs) {
    Map<String, Term> allTerms = new HashMap<>();
    TermVectorsRequest request = new TermVectorsRequest(indexName, docId);
    TermVectorsResponse response = null;
    request.setFields("content");
    request.setFieldStatistics(true);
    request.setTermStatistics(true);
    try {
      response =
              restClient.termvectors(request, RequestOptions.DEFAULT);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if(response.getTermVectorsList() == null) return allTerms;
    for (TermVectorsResponse.TermVector tv : response.getTermVectorsList()) {
      String fieldname = tv.getFieldName();
      int docCount = tv.getFieldStatistics().getDocCount();
      long sumTotalTermFreq =
              tv.getFieldStatistics().getSumTotalTermFreq();
      long sumDocFreq = tv.getFieldStatistics().getSumDocFreq();
      if (tv.getTerms() != null) {
        List<TermVectorsResponse.TermVector.Term> terms =
                tv.getTerms();
        for (TermVectorsResponse.TermVector.Term currTerm : terms) {
          String termStr = currTerm.getTerm();
          int termFreq = currTerm.getTermFreq();
          int docFreq = currTerm.getDocFreq();
          long totalTermFreq = currTerm.getTotalTermFreq();
          double idf = Math.log(totalDocs*1.0/docCount);
          allTerms.put(termStr, new Term(termStr, docFreq, termFreq, totalTermFreq, idf));
        }
      }
    }
    return allTerms;
  }
}
