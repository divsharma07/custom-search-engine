import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.ClearScrollResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.SpanTermQueryBuilder;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.xcontent.XContentType;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class ESClient {

  private RestHighLevelClient restClient = null;
  private static ESClient esClient = null;
  private String indexName;
  private int avgDocLength;

  private final int SEARCH_HIT_COUNT = 100000;

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
    if (esClient == null) {
      esClient = new ESClient(indexName);
    }
    return esClient;
  }

  /**
   * Gets all the unique words in an index based on a document frequency ration metric.
   * @param fieldName the field name from which the unique words need to be fetched
   * @param totalDocs the total docs that are present in the index
   * @return
   */
  public Map<String, Integer> getAllUniqueWords(String fieldName, int totalDocs) {
    totalDocs = 75000;
    int wordCount = 0;
    Map<String, Integer> result = new LinkedHashMap<>();
    SearchRequest searchRequest = new SearchRequest(indexName);
    searchRequest.scroll(TimeValue.timeValueMinutes(1L)); // Set the scroll time

    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    searchSourceBuilder.query(QueryBuilders.matchAllQuery());
    searchSourceBuilder.aggregation(AggregationBuilders
            .terms("terms")
            .field(fieldName)
            .size(28000)); // max terms to return
    searchRequest.source(searchSourceBuilder);

    SearchResponse searchResponse = null;
    try {
      searchResponse = restClient.search(searchRequest, RequestOptions.DEFAULT);
    } catch (IOException e) {
      e.printStackTrace();
    }

    Terms termsAgg = searchResponse.getAggregations().get("terms");

    for (Terms.Bucket bucket : termsAgg.getBuckets()) {
      String uniqueWord = bucket.getKeyAsString();
      long count = bucket.getDocCount();
      double fractionDocFreq = (count * 1.0)/totalDocs;
      if(fractionDocFreq > 0.0001 && fractionDocFreq < 0.999) {
        result.put(uniqueWord, wordCount);
        wordCount++;
      }
    }
    return result;
  }

  /**
   * Gets all the terms in a document and their frequency.
   * @param docId the document for which the search needs to be made.
   * @param field the field from which the terms need to be extracted
   * @param requiredTerms the set of important terms, for which the tf needs to be calculated.
   * @return the map of term and its freq.
   * @throws IOException
   */
  public Map<String, Integer> getTermFreqMap(String docId, String field, Map<String, Integer> requiredTerms) throws IOException {
    GetRequest getRequest = new GetRequest(indexName)
            .id(docId)
            .fetchSourceContext(new FetchSourceContext(true));
    GetResponse getResponse = restClient.get(getRequest, RequestOptions.DEFAULT);
    Map<String,Integer> termFrequencies = new HashMap<>();
    if (getResponse.isExists() && getResponse.getSource() != null) {
      String text = getResponse.getSource().get(field).toString();
      // Tokenize the text into terms
      String[] terms = text.split("\\s+");
      // Count the frequency of each term
      for (String term : terms) {
        if(requiredTerms.containsKey(term)) {
          int frequency = termFrequencies.getOrDefault(term, 0);
          termFrequencies.put(term, frequency + 1);
        }
      }
    }
    return termFrequencies;
  }


  /**
   * Gets the documents with all the data and loads them up in the index.
   * @param docs
   */
  public void populateIndex(List<ParsedDocument> docs) {

    for (ParsedDocument curr : docs) {
      Map<String, Object> jsonMap = new HashMap<String, Object>();
      jsonMap.put("content", curr.getContent());
      jsonMap.put("label", curr.getLabel());
      jsonMap.put("split", curr.getDataSetType());
      IndexRequest indexRequest = new IndexRequest(indexName)
              .id(curr.getId()).source(jsonMap);
      try {
        restClient.index(indexRequest, RequestOptions.DEFAULT);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Close the client connection to elastic search
   */
  public void close() {
    try {
      restClient.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Check if a particular index exists or not.
   * @param indexName the name of the index
   * @return if the index exists or not.
   */
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

  /**
   * Creates a particular index with the required setting.
   * @param indexName the name of the index
   */
  public void createIndex(String indexName) {
    CreateIndexRequest indexRequest = new CreateIndexRequest(indexName);
    indexRequest.source("{\"settings\":{\"number_of_shards\":1,\"number_of_replicas\":1,\"analysis\":{\"filter\":{\"english_stop\":{\"type\":\"stop\",\"stopwords_path\":\"stoplist.txt\"}},\"analyzer\":{\"stopped\":{\"type\":\"custom\",\"tokenizer\":\"standard\",\"filter\":[\"lowercase\",\"english_stop\", \"stemmer\"]}}}},\"mappings\":{\"nameType\":{\"properties\":{\"content\":{\"type\":\"text\",\"fielddata\":true,\"term_vector\":\"with_positions_offsets_payloads\",\"analyzer\":\"stopped\"},\"split\":{\"type\":\"text\",\"fielddata\":true},\"label\":{\"type\":\"text\",\"fielddata\":true}}}}}",
            XContentType.JSON);
    try {
      restClient.indices().create(indexRequest, RequestOptions.DEFAULT);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  /**
   * Search a particular word, splitting it on the basis of if its a uni, bi or tri-gram, returning the documents and scores.
   * @param query the word tokenized using whitespace.
   * @return
   */
  public Map<String, Double> search(List<String> query) {
    Map<String, Double> result = new HashMap<>();
    int nGramValue = query.size();
    Scroll scroll = new Scroll(TimeValue.timeValueMinutes(1L));
    SearchRequest searchRequest = new SearchRequest(indexName);
    searchRequest.scroll(scroll);
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    searchSourceBuilder.query(QueryBuilders.matchAllQuery());
    searchRequest.source(searchSourceBuilder);
    QueryBuilder matchQueryBuilder = null;
    if (nGramValue == 1) {
      matchQueryBuilder = QueryBuilders.matchQuery("content", query.get(0));
    } else if (nGramValue == 2) {
      matchQueryBuilder = QueryBuilders.spanNearQuery(new SpanTermQueryBuilder("content", query.get(0)), 3)
              .addClause(new SpanTermQueryBuilder("content", query.get(1))).inOrder(true);
    } else if (nGramValue == 3) {
      matchQueryBuilder = QueryBuilders.spanNearQuery(new SpanTermQueryBuilder("content", query.get(0)), 3)
              .addClause(new SpanTermQueryBuilder("content", query.get(1)))
              .addClause(new SpanTermQueryBuilder("content", query.get(2))).inOrder(true);
    }

    searchSourceBuilder.query(matchQueryBuilder);
    try {
      SearchResponse searchResponse = restClient.search(searchRequest, RequestOptions.DEFAULT);
      String scrollId = searchResponse.getScrollId();
      while (true) {
        SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
        scrollRequest.scroll(TimeValue.timeValueMinutes(1L));
        SearchResponse scrollResponse = restClient.scroll(scrollRequest, RequestOptions.DEFAULT);
        scrollId = scrollResponse.getScrollId();
        SearchHits hits = scrollResponse.getHits();
        if (scrollResponse.getHits().getHits().length == 0) {
          ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
          clearScrollRequest.addScrollId(scrollId);
          ClearScrollResponse clearScrollResponse = restClient.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
          break;
        }
        for (SearchHit hit : hits) {
          result.put(hit.getId(), (double)hit.getScore());
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return result;
  }

  /**
   * Gets the ngram score given a list of nGrams
   * @param nGrams the list of nGrams
   * @return the map of scores.
   */
  public Map<String, Map<String, Double>> getNGramScore(List<String> nGrams) {
    Map<String, Map<String, Double>> result = new HashMap<>();
    for (String nGram : nGrams) {
      String[] split = nGram.split(" ");
      result.put(nGram, esClient.search(List.of(nGram)));
    }
    return result;
  }
}
