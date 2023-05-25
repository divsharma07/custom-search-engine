import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import dto.QrelResult;
import dto.QrelRow;
import dto.TrainingData;
import dto.TrecResult;
import dto.TrecRow;

public class DataProcessor {

  private Map<String, List<String>> trainingSet = new LinkedHashMap<>();
  private Map<String, List<String>> testSet = new LinkedHashMap<>();
  private static final String QREL_FILE = "qrels.adhoc.51-100.AP89.txt";

  public boolean trainingFileExist(String fileName) {
    File file = new File(fileName);
    return file.exists();
  }

  public Map<String, List<String>> getTestSet() {
    return testSet;
  }

  public Map<String, List<String>> getTrainingSet() {
    return trainingSet;
  }

  public List<String> getFeatureMatrixData(String[] trecFiles, String trainingFileName) throws IOException {
    List<TrecResult> trecList = new ArrayList<>();
    for (String trecFile : trecFiles) {
      trecList.add(parseTrecInput(trecFile));
    }

    Set<String> commonQueryDocPairs = findCommonQueryDocPairs(trecList);
    Map<String, String> uniqueQueryDocMap = new HashMap<>();
    commonQueryDocPairs.forEach((key) -> uniqueQueryDocMap.put(key, ""));
    Map<Integer, Map<String, Integer>> qrelMap = parseQrelFile().getQrelMap();
    for (TrecResult trecResult : trecList) {
      for (Map.Entry<Integer, Map<String, Double>> eachQuery : trecResult.getTrecMap().entrySet()) {
        for (Map.Entry<String, Double> eachDoc : eachQuery.getValue().entrySet()) {
          String queryDocConcat = eachQuery.getKey() + " " + eachDoc.getKey();
          if (commonQueryDocPairs.contains(queryDocConcat)) {
            String value = uniqueQueryDocMap.get(queryDocConcat).trim();
            value += " " + eachDoc.getValue();
            uniqueQueryDocMap.put(queryDocConcat, value);
          }
        }
      }
    }

    int count = 0;
    Random random = new Random();
    // creating another map to reduce the number of non-relevant documents
    Map<String, String> resultMap = new HashMap<>();
    // adding the relevance value for the query document pairs for which it is available
    for(Map.Entry<String, String> docValue: uniqueQueryDocMap.entrySet()) {
      String[] split = docValue.getKey().split(" ");
      int query = Integer.parseInt(split[0]);
      String doc = split[1];

      int relevance = 0;
      if(qrelMap.get(query).get(doc) != null) {
        relevance = qrelMap.get(query).get(doc);
        count++;
      }

      String currValue = docValue.getValue();
      currValue += " " + relevance;
      docValue.setValue(currValue);

      if(relevance != 0) {
        resultMap.put(docValue.getKey(), currValue);
      }
      else {
        int randomNumber = random.nextInt(5);
        // filtering and picking only 1/5th of non-relevant docs
        if(randomNumber == 0) {
          resultMap.put(docValue.getKey(), currValue);
        }
      }
    }

    List<String> dataRowList = new ArrayList<String>();

    for(Map.Entry<String, String> entry : resultMap.entrySet()) {
      dataRowList.add(entry.getKey() + " " + entry.getValue());
    }
    return dataRowList;
  }

  private static Set<String> findCommonQueryDocPairs(List<TrecResult> trecList) {
    Map<String, Set<String>> modelPairMap = new HashMap<>();

    for(TrecResult trecResult: trecList) {
      for(Map.Entry<Integer, Map<String, Double>> eachQuery: trecResult.getTrecMap().entrySet()) {
        for(Map.Entry<String, Double> eachDoc: eachQuery.getValue().entrySet()) {
          String queryDocConcat = eachQuery.getKey() + " " + eachDoc.getKey();
          if(modelPairMap.containsKey(trecResult.getModel())) {
            Set<String> currSet = modelPairMap.get(trecResult.getModel());
            currSet.add(queryDocConcat);
          }
          else {
            Set<String> newSet = new HashSet<>();
            newSet.add(queryDocConcat);
            modelPairMap.put(trecResult.getModel(), newSet);
          }
        }
      }
    }
    Set<String> result = new HashSet<>(modelPairMap.values().iterator().next());

    for(Set<String> set: modelPairMap.values()) {
      result.retainAll(set);
    }

    return result;
  }

  private QrelResult parseQrelFile() throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(QREL_FILE));
    Map<Integer, Map<String, Integer>> qrelMap = new TreeMap<>();
    Map<Integer, Map<String, Integer>> gradedRelevance = new TreeMap<>();
    Map<Integer, Integer> allRelMap = new TreeMap<>();
    List<QrelRow> qrelList = new ArrayList<QrelRow>();
    String line = "";
    while ((line = reader.readLine()) != null) {
      String split[] = line.split("\\s+");
      int queryId = Integer.parseInt(split[0]);
      String docId = split[2];
      String accessorId = split[1];
      int relevanceGrade = Integer.parseInt(split[3]);
      int allRel = relevanceGrade == 1 || relevanceGrade == 2 ? 1 : 0;
      int rel = relevanceGrade == 1 ? 1 : 0;
      int vRel = relevanceGrade == 2 ? 1 : 0;
      qrelList.add(new QrelRow(queryId, accessorId, docId, relevanceGrade));
      qrelMap.computeIfAbsent(queryId, k -> new TreeMap<>());
      qrelMap.get(queryId).put(docId, allRel);
      allRelMap.put(queryId, allRelMap.getOrDefault(queryId, 0) + allRel);
      gradedRelevance.computeIfAbsent(queryId, k -> new TreeMap<>());
      gradedRelevance.get(queryId).put(docId, relevanceGrade);
    }

    return new QrelResult(qrelList, qrelMap, allRelMap, gradedRelevance);
  }

  private TrecResult parseTrecInput(String filePath) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(filePath));
    List<TrecRow> trecList = new ArrayList<>();
    Map<Integer, Map<String, Double>> trecMap = new TreeMap<>();
    String line = "";
    while ((line = reader.readLine()) != null) {
      String split[] = line.split("\\s+");
      int queryId = Integer.parseInt(split[0]);
      String docId = split[2];
      double score = Double.parseDouble(split[4]);
      trecList.add(new TrecRow(queryId, docId, score));
      trecMap.computeIfAbsent(queryId, k -> new TreeMap<>());
      trecMap.get(queryId).put(docId, score);
    }

    return new TrecResult(trecMap, trecList, filePath);
  }

  public void segregateTrainingAndTestingSet(Map<String, List<String>> featureMatrixData) {
    int index = 0;
    testSet = new HashMap<>();
    trainingSet = new HashMap<>();
    for(Map.Entry<String, List<String>> each: featureMatrixData.entrySet()) {
      if(index < 5) {
        testSet.put(each.getKey(), each.getValue());
      }
      else {
        trainingSet.put(each.getKey(), each.getValue());
      }
      index++;
    }
  }
}
