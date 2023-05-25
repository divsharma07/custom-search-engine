import weka.classifiers.AbstractClassifier;
import weka.classifiers.functions.LibLINEAR;
import weka.core.Instances;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Classifier2 implements Classifier {

  private ESClient esClient;
  private Map<String, Integer> nGramsWithIdMap;
  private String uniquePrefix;
  private String TEST_ARFF_REG_FILE;
  private String TRAIN_ARFF_REG_FILE;
  private String ALL_DATA_FILE;
  public Classifier2(ESClient esClient, Map<String, Integer> nGramsWithIdMap, String uniquePrefix) {
    this.esClient = esClient;
    this.nGramsWithIdMap = nGramsWithIdMap;
    this.uniquePrefix = uniquePrefix;
    TEST_ARFF_REG_FILE = "Results" + File.separator + uniquePrefix + "_dataTestReg.arff";
    TRAIN_ARFF_REG_FILE = "Results" + File.separator + uniquePrefix + "_dataTrainReg.arff";
    ALL_DATA_FILE = "Results" + File.separator + uniquePrefix +  "_allData.txt";
  }

  @Override
  public void orchestrateSpamDetection(Map<String, ParsedDocument> docs) throws Exception {
    docs = removeEmptyDocs(docs);
    Map<String, Map<String, Integer>> docIdTermFreqMap = getdocIdTermFreqMap(docs, nGramsWithIdMap);
    createArffFile(docs, docIdTermFreqMap, nGramsWithIdMap);
    Instances testData = new Instances(new BufferedReader(new FileReader(TEST_ARFF_REG_FILE)));
    Instances trainData = new Instances(new BufferedReader(new FileReader(TRAIN_ARFF_REG_FILE)));
    testData.setClassIndex(testData.numAttributes() - 1);
    trainData.setClassIndex(trainData.numAttributes() - 1);
    Enumeration testInstances = testData.enumerateInstances();
    Enumeration trainInstances = trainData.enumerateInstances();
    AbstractClassifier libLinear = new LibLINEAR();
    libLinear.buildClassifier(trainData);
    List<String> allData = null;
    allData = Files.readAllLines(Paths.get(ALL_DATA_FILE), Charset.defaultCharset());
    Helper.computeAndPrintResult(Classifier1.Model.REGRESSION, libLinear, allData, testInstances, trainInstances, trainData, false, uniquePrefix);
  }

  private Map<String,ParsedDocument> removeEmptyDocs(Map<String,ParsedDocument> docs) {
    Map<String, ParsedDocument> result = new LinkedHashMap<>();
    for(Map.Entry<String, ParsedDocument> each: docs.entrySet()) {
      boolean isEmptyDoc = each.getValue().getContent().equals("");

      if(!isEmptyDoc) {
        result.put(each.getKey(), each.getValue());
      }
    }
    return result;
  }

  private Map<String, Map<String, Integer>> getdocIdTermFreqMap(Map<String, ParsedDocument> docs, Map<String,
          Integer> nGrams) throws IOException {
    Map<String, Map<String, Integer>> result = new HashMap<>();
    for(Map.Entry<String, ParsedDocument> each: docs.entrySet()) {
      result.put(each.getKey(), esClient.getTermFreqMap(each.getKey(), "content", nGrams));
    }

    return result;
  }

  private void createArffFile(Map<String, ParsedDocument> docs, Map<String,
          Map<String, Integer>> docIdTermFreqMap,
                              Map<String, Integer> nGramsIdMap) {
    Map<String, String> dataTest = new LinkedHashMap<>();
    Map<String, String> dataTrain = new LinkedHashMap<>();
    StringBuilder allData = new StringBuilder();


    // saving ngrams for each document
    for (String doc : docIdTermFreqMap.keySet()) {
      Map<Integer, Integer> nGramScores = new TreeMap<>();
      ParsedDocument parsedDoc = docs.get(doc);
      Map<String, Integer> docNgrams = docIdTermFreqMap.get(doc);
      // mapping ngram text to its id, which leads to the map nGramScores containing id: termFreq
      for (Map.Entry<String, Integer> docNgram : docNgrams.entrySet()) {
        nGramScores.put(nGramsIdMap.get(docNgram.getKey()), docNgram.getValue());
      }

      if (parsedDoc.getDataSetType() == ParsedDocument.DataSetType.TEST) {
        updateMap(dataTest, nGramScores, doc, parsedDoc, nGramsIdMap.size());
      } else {
        updateMap(dataTrain, nGramScores, doc, parsedDoc, nGramsIdMap.size());
      }
    }
    StringBuilder dataTestSb = Helper.listToStringBuilder(new ArrayList<>(dataTest.values()));
    StringBuilder dataTrainSb = Helper.listToStringBuilder(new ArrayList<>(dataTrain.values()));
    // combining train and test data to and adding ID's to each line so that ID prediction map can be created.
    List<String> allDataWithId = Helper.addIdToDataSet(dataTrain);
    allDataWithId.addAll(Helper.addIdToDataSet(dataTest));
    StringBuilder allDataSb = Helper.listToStringBuilder(allDataWithId);
    List<String> nGramIds = new ArrayList<>();
    nGramsIdMap.forEach((k, v) -> nGramIds.add(String.valueOf(v)));
    dataTestSb = Helper.arffHeaders(nGramIds, false, "FEAT-").append(dataTestSb);
    dataTrainSb = Helper.arffHeaders(nGramIds, false, "FEAT-").append(dataTrainSb);
    try {
      Helper.writeToFile(dataTestSb.toString(), TEST_ARFF_REG_FILE);
      Helper.writeToFile(dataTrainSb.toString(), TRAIN_ARFF_REG_FILE);
      Helper.writeToFile(allDataSb.toString(), ALL_DATA_FILE);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void updateMap(Map<String, String> dataMap, Map<Integer, Integer> nGramScores, String doc, ParsedDocument parsedDoc, int nGramsSize) {
    StringBuilder dataInstance = new StringBuilder("{");
    for (Map.Entry<Integer, Integer> eachTerm: nGramScores.entrySet()) {
      dataInstance.append(eachTerm.getKey()).append(" ").append(eachTerm.getValue()).append(",").append(" ");
    }
    int label = parsedDoc.getLabel() == ParsedDocument.LabelType.HAM? 0 : 1;
    dataInstance.append(nGramsSize).append(" ").append(label).append("}");
    dataMap.put(doc, dataInstance.toString());
  }

  private void createTermIdMap() {
  }
}
