import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import elasticsearch.ESClient;
import model.Document;
import model.Query;
import model.RetrievalResponse;
import model.Term;
import parse.DocParser;
import parse.QueryParser;
import retrieve.ESDefault;
import retrieve.OkapiBM25;
import retrieve.OkapiTf;
import retrieve.RetrievalModel;
import retrieve.TfIdf;
import retrieve.UnigramLMJM;
import retrieve.UnigramLMLaplace;


public class Controller {

    private static String indexName = "hw1docsindex";
    public static void main(String args[]) {
        String filePath = new File("").getAbsolutePath();
        List<Document> docs = new ArrayList<>();
        List<Query> queries;
        ESClient esClient = ESClient.getElasticSearchClient(indexName);
        QueryParser queryParser = new QueryParser("query_desc.51-100.short.txt");
        DocParser docParser = new DocParser(indexName);
        Set<String> set = new HashSet<>();
        try {
            docs = docParser.parseDocs(filePath + "/IR_data/AP_DATA/ap89_collection");
            docParser.fetchDocStats(docs);
            int vocabLength = docParser.getVocabulary(docs).size();
            if(!esClient.indexExists(indexName)) {
                esClient.createIndex(indexName);
                esClient.populateIndex(docs);
                System.out.println("Indexing complete");
            }
            int avgDocLen = docParser.getAvgDocLength(docs);
            queries = queryParser.parseQueries();
            runRetrievalModels(docs, queries, avgDocLen, vocabLength);
            Query selectedQuery = queries.get(3);
            relevanceFeedback(docs, selectedQuery, 30, avgDocLen, docParser);
            relevanceFeedbackES(esClient, docs, selectedQuery, 30, avgDocLen, docParser);
            esClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void relevanceFeedbackES(ESClient esClient, List<Document> docs, Query query, int k, int avgDocLength, DocParser docParser) {
        List<String> sigTerms = esClient.getSignificantQueryTerms(query.getTermCountMap().keySet());
        RetrievalModel model = new OkapiBM25(avgDocLength, "OkapiTfPreFeedbackES");
        List<RetrievalResponse> responses = model.scoreDocuments(docs, new ArrayList<Query>(List.of(query))).get(query.getId());
        List<Document> newDocuments = new ArrayList<>();

        // Top k elements picked for pseudo-relevance
        for(int i = 0; i < k; i++) {
            newDocuments.add(responses.get(i).getDocument());
        }
        sigTerms = docParser.rankSigTerms(newDocuments, docs, sigTerms);
        Map<String, Integer> newMap = query.getTermCountMap();
        StringBuilder newText = new StringBuilder(query.getText());
        // adding 3 terms for expanding query
        for(int i = 0; i < 3; i++) {
            newMap.put(sigTerms.get(i), 1);
            newText.append(" ").append(sigTerms.get(i));
        }
        Query newQuery = new Query(query.getId(), newText.toString(), newMap);
        model = new OkapiBM25(avgDocLength, "OkapiTfPostFeedbackES");
        model.scoreDocuments(docs, new ArrayList<>(List.of(newQuery))).get(query.getId());
    }

    private static void relevanceFeedback(List<Document> docs, Query query, int k, int avgDocLength, DocParser docParser) {
        RetrievalModel model = new OkapiBM25(avgDocLength, "OkapiBM25PreFeedback");
        List<RetrievalResponse> responses = model.scoreDocuments(docs, new ArrayList<>(List.of(query))).get(query.getId());
        List<Document> newDocuments = new ArrayList<>();

        // Top k elements picked for pseudo-relevance
        for(int i = 0; i < k; i++) {
            newDocuments.add(responses.get(i).getDocument());
        }
        List<Term> terms = docParser.getTopRankedTerms(newDocuments, docs, query.getTermCountMap().keySet());

        // now adding 3 terms to the query and re-running the process
        Map<String, Integer> newMap = query.getTermCountMap();
        StringBuilder newText = new StringBuilder(query.getText());
        for(int i = 0; i < 3; i++) {
            newMap.put(terms.get(i).getTitle(), 1);
            newText.append(" ").append(terms.get(i).getTitle());
        }

        Query newQuery = new Query(query.getId(), newText.toString(), newMap);
        model = new OkapiBM25(avgDocLength, "OkapiBM25PostFeedback");
        model.scoreDocuments(docs, new ArrayList<Query>(List.of(newQuery))).get(query.getId());
    }

    private static void runRetrievalModels(List<Document> docs, List<Query> queries, int avgDocLength, int vocabLength) {
        RetrievalModel model = new ESDefault(indexName, "ESDefault");
        model.scoreDocuments(docs, queries);
        model = new OkapiTf(indexName, avgDocLength, "OkapiTf");
        model.scoreDocuments(docs, queries);
        model = new TfIdf( "TfIdf");
        model.scoreDocuments(docs, queries);
        model = new OkapiBM25(avgDocLength, "OkapiBM25");
        model.scoreDocuments(docs, queries);
        model = new UnigramLMLaplace("UnigramLMLaplace", vocabLength);
        model.scoreDocuments(docs, queries);
        model = new UnigramLMJM("UnigramLMJM", vocabLength);
        model.scoreDocuments(docs, queries);
    }
}
