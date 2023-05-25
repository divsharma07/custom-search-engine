package elasticsearch;

import com.google.gson.Gson;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.xcontent.XContentType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import dataobjects.Document;

import static config.Configuration.ELASTIC_HOSTNAME;
import static config.Configuration.ELASTIC_PASSWORD;
import static config.Configuration.INDEX_SETTINGS;

/**
 * Class encapsulated all functionality that requires communicaition with Elastic Search
 */
public class ESClient {

  private RestHighLevelClient restClient = null;
  private static ESClient esClient = null;
  private String indexName;
  private int avgDocLength;

  private ESClient(String indexName, boolean cloud) {
    if(cloud) {
      final CredentialsProvider credentialsProvider =
              new BasicCredentialsProvider();
      credentialsProvider.setCredentials(AuthScope.ANY,
              new UsernamePasswordCredentials("elastic", ELASTIC_PASSWORD));
      RestClientBuilder builder = RestClient.builder(
                      new HttpHost(ELASTIC_HOSTNAME, 9243, "https"))
              .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                      .setDefaultCredentialsProvider(
                              credentialsProvider));
      restClient = new RestHighLevelClient(builder);
    } else {
      restClient = new RestHighLevelClient(
              RestClient.builder(
                      new HttpHost("localhost", 9200, "http")));
    }
    this.indexName = indexName;
  }

  public static ESClient getElasticSearchClient(String indexName, boolean cloud) {
    if (esClient == null) {
      esClient = new ESClient(indexName, cloud);
    }
    return esClient;
  }

  public void populateIndex(Set<Document> docs) throws IOException {

    for (Document currDoc : docs) {

      if(!docExists(currDoc)) {
        populateIndex(currDoc);
      }
      else {
        Document mergedDoc = merge(currDoc);
        updateIndex(mergedDoc);
      }
    }
  }

  private void updateIndex(Document mergedDoc) {
    UpdateRequest updateRequest = new UpdateRequest(indexName, mergedDoc.getDocid());
    String json = new Gson().toJson(mergedDoc);
    updateRequest.doc(json, XContentType.JSON);
    try {
      restClient.update(updateRequest, RequestOptions.DEFAULT);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private Document merge(Document currDoc) throws IOException {
    GetRequest getRequest = new GetRequest(
            indexName,
            currDoc.getDocid());

    GetResponse getResponse = restClient.get(getRequest, RequestOptions.DEFAULT);
    ObjectMapper objectMapper = new ObjectMapper();
    Document existingDoc = objectMapper.readValue(getResponse.getSourceAsString(), Document.class);

    // now merging the required values.
    Set<String> inLinks = new HashSet<>(existingDoc.getInLinks());
    inLinks.addAll(currDoc.getInLinks());
    Set<String> outLinks = new HashSet<>(existingDoc.getOutLinks());
    outLinks.addAll(currDoc.getOutLinks());
    String author = existingDoc.getAuthor();
    if(!existingDoc.getAuthor().contains(currDoc.getAuthor())) {
      author += " , " + currDoc.getAuthor();
    }

    return new Document(currDoc.getDocid(), currDoc.getContent(), currDoc.getTitle(),
            new ArrayList<>(inLinks), new ArrayList<>(outLinks), author);
  }

  private boolean docExists(Document currDoc) throws IOException {
    GetRequest getRequest = new GetRequest(
            indexName,
            currDoc.getDocid());
    getRequest.fetchSourceContext(new FetchSourceContext(false));
    getRequest.storedFields("_none_");
    return restClient.exists(getRequest, RequestOptions.DEFAULT);
  }

  public void populateIndex(Document doc) {
    String json = new Gson().toJson(doc);
    IndexRequest indexRequest = new IndexRequest(indexName);
    indexRequest.id(doc.getDocid());
    indexRequest.source(json, XContentType.JSON);
    try {
      restClient.index(indexRequest, RequestOptions.DEFAULT);
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
    indexRequest.source(INDEX_SETTINGS,
            XContentType.JSON);
    try {
      restClient.indices().create(indexRequest, RequestOptions.DEFAULT);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
