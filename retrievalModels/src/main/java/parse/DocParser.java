package parse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.print.Doc;

import elasticsearch.ESClient;
import model.Document;
import model.Query;
import model.Term;

public class DocParser {
    ESClient esClient;
    String indexName;
    public DocParser(String indexName) {
        this.indexName = indexName;
        this.esClient = ESClient.getElasticSearchClient(indexName);
    }

    public List<Document> parseDocs(String path) throws IOException {
        List<Document> docs = new ArrayList<>();
        Preprocessor preprocessor = new Preprocessor();
        File directoryFolder = new File(path);
        File[] files = directoryFolder.listFiles();
        for(File file: files) {
            BufferedReader fileReader = new BufferedReader(new FileReader(file));

            String line = "";
            String docId = "";
            StringBuilder content = new StringBuilder();
            while ((line = fileReader.readLine()) != null) {
                if(line.contains("<DOCNO>")) {
                    if(!docId.equals("")) {
                        docs.add(new Document(docId, preprocessor.stemmingAndStopWords(content.toString())));
                    }
                    content.setLength(0);
                    docId = line.split(" ")[1];
                }
                if(line.contains("<TEXT>")) {
                    while((line = fileReader.readLine()) != null && !line.contains("</TEXT>")) {
                        content.append(" ").append(line.trim());
                    }
                }
            }

            if(!docId.equals("") && content.length() > 0) {
                docs.add(new Document(docId, content.toString()));
            }
        }
        return docs;
    }

    public int getAvgDocLength(List<Document> docs) {
        int totalLengths = 0;

        for(Document doc: docs) {
            totalLengths += doc.getLength();
        }

        return totalLengths/docs.size();
    }

    public List<Term> getTopRankedTerms(List<Document> topDocs, List<Document> allDocs, Set<String> queryTerms) {
        fetchDocStats(topDocs);
        Set<Term> topTerms = new HashSet<>();
        Map<String, Integer> termCount = new HashMap<>();
        Map<String, Term> allTerms = new HashMap<>();
        double docLen = 0;
        for(Document doc: topDocs) {
            topTerms.addAll(doc.getTerms().values());
            docLen += doc.getLength();
        }

        for(Term term: topTerms) {
            String title = term.getTitle();
            for(Document doc: topDocs) {
                if(doc.getTerms().containsKey(title)) {
                    termCount.put(title, termCount.getOrDefault(title, 0) + 1);
                }
            }
        }
        for(Document doc: allDocs) {
            allTerms.putAll(doc.getTerms());
        }

        List<Term> result = new ArrayList<>(topTerms);
        for(Term t: topTerms) {
            if(queryTerms.contains(t.getTitle())) continue;
            if(allTerms.get(t.getTitle()) == null) {
                continue;
            }
            double idf = allTerms.get(t.getTitle()).getIdf();
            t.setRelavenceScore(termCount.get(t.getTitle()) * idf);
            result.add(t);
        }
        result.sort(Comparator.comparingDouble(Term::getRelavenceScore).reversed());
        return result.subList(0, 5);
    }

    public void fetchDocStats(List<Document> docs) {
        for(Document doc : docs) {
            Map<String, Term> terms = esClient.getDocTermInfo(doc.getId(), docs.size());
            doc.setTerms(terms);
        }
    }

  public Set<String> getVocabulary(List<Document> docs) {
       Set<String> result = new HashSet<>();
       for(Document doc: docs) {
           result.addAll(doc.getTerms().keySet());
       }
       return result;
  }

    public List<String> rankSigTerms(List<Document> topDocs, List<Document> docs, List<String> sigTerms) {
        List<String> result = new ArrayList<>();
        List<Term> resultTerms = new ArrayList<>();
        fetchDocStats(topDocs);

        for(String sigTerm: sigTerms) {
            for(Document doc: topDocs) {
                if(doc.getTerms().containsKey(sigTerm)) {
                    resultTerms.add(doc.getTerms().get(sigTerm));
                    break;
                }
            }
        }
        resultTerms.sort(Comparator.comparingDouble(Term::getIdf).reversed());
        for(Term term: resultTerms) {
            result.add(term.getTitle());
        }

        return result;
    }
}