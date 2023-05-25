import weka.classifiers.AbstractClassifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.LibLINEAR;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.M5P;
import weka.core.Instance;
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

import static weka.core.Debug.writeToFile;

public class Classifier1 implements Classifier {

  private ESClient esClient;

  private List<String> nGrams;

  private String uniquePrefix;

  private String TEST_ARFF_FILE;
  private String TRAIN_ARFF_FILE;
  private String ALL_DATA_FILE;
  private String TRAIN_ARFF_REG_FILE;
  private String TEST_ARFF_REG_FILE;

  public Classifier1(ESClient esClient, List<String> nGrams, String uniquePrefix) {
    this.esClient = esClient;
    this.nGrams = nGrams;
    this.uniquePrefix = uniquePrefix;
    TEST_ARFF_FILE = "Results" + File.separator + uniquePrefix + "_dataTest.arff";
    TRAIN_ARFF_FILE = "Results" + File.separator + uniquePrefix + "_dataTrain.arff";
    ALL_DATA_FILE = "Results" + File.separator + uniquePrefix +  "_allData.txt";
    TRAIN_ARFF_REG_FILE =  "Results" + File.separator + uniquePrefix + "_dataTrainReg.arff";
    TEST_ARFF_REG_FILE =  "Results" + File.separator + uniquePrefix + "_dataTestReg.arff";
  }

  @Override
  public void orchestrateSpamDetection(Map<String, ParsedDocument> docs) throws IOException {
    createArffFile(docs, false);
    createArffFile(docs, true);

    for (int i = 0; i < Model.values().length; i++) {
      try {
        trainModelAndWriteResult(Model.values()[i]);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void trainModelAndWriteResult(Model model) throws IOException {
    Instances testData = new Instances(new BufferedReader(new FileReader(TEST_ARFF_FILE)));
    Instances trainData = new Instances(new BufferedReader(new FileReader(TRAIN_ARFF_FILE)));
    testData.setClassIndex(testData.numAttributes() - 1);
    trainData.setClassIndex(trainData.numAttributes() - 1);
    Enumeration testInstances = testData.enumerateInstances();
    Enumeration trainInstances = trainData.enumerateInstances();
    List<String> allData = null;
    allData = Files.readAllLines(Paths.get(ALL_DATA_FILE), Charset.defaultCharset());
    try {
      switch (model) {
        case DECISION_TREE:
          AbstractClassifier decisionTree = new J48();
          decisionTree.buildClassifier(trainData);
          Helper.computeAndPrintResult(Model.DECISION_TREE, decisionTree, allData, testInstances, trainInstances, trainData, false, uniquePrefix);
          break;
        case NAIVE_BAYES:
          AbstractClassifier naiveBayes = new NaiveBayes();
          naiveBayes.buildClassifier(trainData);
          Helper.computeAndPrintResult(Model.NAIVE_BAYES, naiveBayes, allData, testInstances, trainInstances, trainData, false, uniquePrefix);
          break;
        case REGRESSION:
          AbstractClassifier libLinear = new LibLINEAR();
          libLinear.buildClassifier(trainData);
          Helper.computeAndPrintResult(Model.REGRESSION, libLinear, allData, testInstances, trainInstances, trainData, false, uniquePrefix);
          break;
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  private void createArffFile(Map<String, ParsedDocument> docsMap, boolean isRegression) {
    Map<String, Map<String, Double>> nGramScoreMap = esClient.getNGramScore(nGrams);

    Map<String, String> dataTest = new LinkedHashMap<>();
    Map<String, String> dataTrain = new LinkedHashMap<>();
    StringBuilder allData = new StringBuilder();
    Map<String, List<Double>> mapDocIdNGramScore = new HashMap<>();

    for (String doc : docsMap.keySet()) {
      List<Double> nGramScores = new ArrayList<>();
      ParsedDocument parsedDoc = docsMap.get(doc);
      for (int i = 0; i < nGrams.size(); i++) {
        String currNgram = nGrams.get(i);
        nGramScores.add(nGramScoreMap.get(currNgram).getOrDefault(doc, 0.0));
      }
      mapDocIdNGramScore.put(doc, nGramScores);
      if (parsedDoc.getDataSetType() == ParsedDocument.DataSetType.TEST) {
        updateMap(dataTest, nGramScores, doc, parsedDoc);
      } else {
        updateMap(dataTrain, nGramScores, doc, parsedDoc);
      }
    }
    StringBuilder dataTestSb = Helper.listToStringBuilder(new ArrayList<>(dataTest.values()));
    StringBuilder dataTrainSb = Helper.listToStringBuilder(new ArrayList<>(dataTrain.values()));
    // combining train and test data to and adding ID's to each line so that ID prediction map can be created.
    List<String> allDataWithId = Helper.addIdToDataSet(dataTrain);
    allDataWithId.addAll(Helper.addIdToDataSet(dataTest));
    StringBuilder allDataSb = Helper.listToStringBuilder(allDataWithId);
    dataTestSb = Helper.arffHeaders(nGrams, isRegression, "").append(dataTestSb);
    dataTrainSb = Helper.arffHeaders(nGrams, isRegression, "").append(dataTrainSb);
    try {
      Helper.writeToFile(dataTestSb.toString(), isRegression? TEST_ARFF_REG_FILE: TEST_ARFF_FILE);
      Helper.writeToFile(dataTrainSb.toString(), isRegression? TRAIN_ARFF_REG_FILE: TRAIN_ARFF_FILE);
      Helper.writeToFile(allDataSb.toString(), ALL_DATA_FILE);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  private void updateMap(Map<String, String> dataMap, List<Double> nGramScores, String doc, ParsedDocument parsedDoc) {
    StringBuilder testDataInstance = new StringBuilder();
    for (Double nGramScore : nGramScores) {
      testDataInstance.append(nGramScore).append(",");
    }
    int label = parsedDoc.getLabel() == ParsedDocument.LabelType.HAM? 0 : 1;
    testDataInstance.append(label);
    dataMap.put(doc, testDataInstance.toString());
  }

  enum Model {
    DECISION_TREE,
    REGRESSION,
    NAIVE_BAYES
  }

}
