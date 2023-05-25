import weka.classifiers.Evaluation;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.meta.AdditiveRegression;
import weka.classifiers.trees.J48;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;
import weka.core.pmml.jaxbbindings.DecisionTree;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import dto.DocScorePair;
import dto.TrainingData;

public class Classifier {

  private static final String ARFF_FILE_NAME = "featureMatrix";
  private List<TrainingData> datasetPerQuery;
  private List<String> datasetRaw;

  public Classifier(List<String> datasetRaw) {
    this.datasetRaw = datasetRaw;
    datasetPerQuery = convertFormat(datasetRaw);
  }


  public void orchestrateModelRun(double testTrainSplit) throws Exception {
    int totalRuns = (int) (1 / testTrainSplit);

    for (int i = 0; i < totalRuns; i++) {
      Map<String, List<String>> testSet = getTestSet(i, totalRuns);
      Map<String, List<String>> trainSet = getTrainSet(i, totalRuns);
      List<String> flattenedTestList = testSet.values().stream()
              .flatMap(Collection::stream)
              .collect(Collectors.toList());
      List<String> flattenedTrainList = trainSet.values().stream()
              .flatMap(Collection::stream)
              .collect(Collectors.toList());
      List<String> appendedRawTest = convertToRaw(trainSet);
      appendedRawTest.addAll(convertToRaw(testSet));
      String arffFileName = ARFF_FILE_NAME + i + ".arff";
      createArffFile(flattenedTestList, flattenedTrainList, arffFileName);
      trainModelAndWriteResults(arffFileName, testSet,  i, appendedRawTest);
    }
  }

  private List<String> convertToRaw(Map<String, List<String>> mapQueryToDocScores) {
    List<String> result = new ArrayList<>();
    for(Map.Entry<String, List<String>> each: mapQueryToDocScores.entrySet()) {
      List<String> docsWithScores = each.getValue();
      docsWithScores.forEach(eachDoc -> result.add(each.getKey() + " " + eachDoc));
    }

    return result;
  }
  private void trainModelAndWriteResults(String arffFileName, Map<String, List<String>> testSet, int runIndex, List<String> appendedRawTest) throws Exception {
    Instances data = ConverterUtils.DataSource.read("Results" + File.separator + arffFileName);
    Map<Integer, TreeSet<DocScorePair>> trainingTrecMap = new HashMap<>();
    Map<Integer, TreeSet<DocScorePair>> testingTrecMap = new HashMap<>();
    // Set the class index
    data.setClassIndex(data.numAttributes() - 1);
    // Create a new classifier
    LinearRegression classifier = new LinearRegression();
    int numIterations = 100;
    double learningRate = 0.1;
    double regularization = 0.01;

//    classifier.setNumIterations(numIterations);

    // Train the classifier on the data
    classifier.buildClassifier(data);
    Evaluation evaluation = new Evaluation(data);
    evaluation.crossValidateModel(classifier, data, 10, new java.util.Random(1));
    System.out.println(evaluation.toSummaryString());

    Enumeration enumerationInstances = data.enumerateInstances();
    int index = 0;
    while (enumerationInstances.hasMoreElements()) {
      String[] currRow = appendedRawTest.get(index).split(" ");
      String queryId = currRow[0];
      String docId = currRow[1];

      Instance dataElement = (Instance) enumerationInstances.nextElement();
      double result = classifier.classifyInstance(dataElement);

      if (testSet.containsKey(queryId)) {
        updateMap(testingTrecMap, queryId, docId, result);
      } else {
        updateMap(trainingTrecMap, queryId, docId, result);
      }

      index++;
    }

    writeResult(testingTrecMap, "Results" + File.separator + "testing_result_" + runIndex + ".txt");
    writeResult(trainingTrecMap, "Results" + File.separator + "training_result_" + runIndex + ".txt");
  }

  private void writeResult(Map<Integer, TreeSet<DocScorePair>> trecMap, String fileName) throws IOException {
    BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
    for (Map.Entry<Integer, TreeSet<DocScorePair>> each : trecMap.entrySet()) {
      int rankIndex = 1;
      for (DocScorePair docScorePair : each.getValue()) {
        String eachRecord = each.getKey() + " " + "Q0" + " " + docScorePair.getDocId() + " " + rankIndex + " " + docScorePair.getScore() + " " + "Exp" + "\n";
        writer.write(eachRecord);
        rankIndex++;
      }
    }
    writer.flush();
    writer.close();
  }

  private void updateMap(Map<Integer, TreeSet<DocScorePair>> trecMap, String queryId, String docId, double result) {
    if (trecMap.containsKey(Integer.parseInt(queryId))) {
      TreeSet<DocScorePair> currSet = trecMap.get(Integer.parseInt(queryId));
      currSet.add(new DocScorePair(docId, result));
    } else {
      TreeSet<DocScorePair> newSet = new TreeSet<>();
      newSet.add(new DocScorePair(docId, result));
      trecMap.put(Integer.parseInt(queryId), newSet);
    }
  }

  private void createArffFile(List<String> testSet, List<String> trainSet, String fileName) {
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter("Results" + File.separator + fileName));
      // Write the headers
      writer.write("@relation ML\n");
      writer.write("@attribute ES NUMERIC\n");
      writer.write("@attribute OKAPI_TF NUMERIC\n");
      writer.write("@attribute TF_IDF NUMERIC\n");
      writer.write("@attribute OKAPI_BM25 NUMERIC\n");
      writer.write("@attribute LM_LAPLACE NUMERIC\n");
      writer.write("@attribute LM_JM NUMERIC\n");
      writer.write("@attribute label NUMERIC\n");
      writer.write("@data\n");
      // Write the training instances
      for (String instance : trainSet) {
        String instanceWithoutDocId = Arrays.stream(instance.split(" "))
                .skip(1)
                .collect(Collectors.joining(" "));
        writer.write(instanceWithoutDocId + "\n");
      }
      // Write the test instances
      for (String instance : testSet) {
        String[] splitString = instance.split(" ");
        String instanceWithoutDocId = Arrays.stream(splitString)
                .skip(1)
                .collect(Collectors.joining(" "));
        splitString = instanceWithoutDocId.split(" ");
        String instanceWithoutRelevance =  Arrays.stream(splitString)
                .limit(splitString.length - 1)
                .reduce((a, b) -> a + " " + b)
                .orElse("");
        writer.write(instanceWithoutRelevance + " ?\n");
      }
      writer.close();
      System.out.println("Successfully saved instances to " + fileName);
    } catch (IOException e) {
      System.err.println("Error saving instances to " + fileName);
      e.printStackTrace();
    }
  }

  /**
   * Converts input to a map of Query and List of docid and their features
   *
   * @param rawDataset contains a list of string with format, "queryId docId score1 score2 ..... label"
   * @return a map that is in the format, key: "queryId", value:"docId score1 score2 ..... label"
   */
  private List<TrainingData> convertFormat(List<String> rawDataset) {
    Map<String, List<String>> result = new HashMap<>();
    for (String each : rawDataset) {
      String[] split = each.split(" ");
      String newValue = split[1];
      for (int i = 2; i < split.length; i++) {
        newValue += " " + split[i];
      }
      if (result.containsKey(split[0])) {
        List<String> currList = result.get(split[0]);
        currList.add(newValue);
      } else {
        List<String> newList = new ArrayList<>();
        newList.add(newValue);
        result.put(split[0], newList);
      }
    }
    return changeToList(result);
  }

  private List<TrainingData> changeToList(Map<String, List<String>> queryMap) {
    List<TrainingData> result = new ArrayList<>();
    for (Map.Entry<String, List<String>> each : queryMap.entrySet()) {
      result.add(new TrainingData(each.getKey(), each.getValue()));
    }

    return result;
  }

  private Map<String, List<String>> getTestSet(int i, int totalRuns) {
    int startIndex = i * 5;
    int endIndex = startIndex + totalRuns - 1;
    Map<String, List<String>> result = new LinkedHashMap<>();

    int index = 0;
    for (TrainingData each : datasetPerQuery) {
      if (index >= startIndex && index <= endIndex) {
        result.put(each.getQueryId(), each.getDocWithScoreList());
      }
      index++;
    }
    return result;
  }

  private Map<String, List<String>> getTrainSet(int i, int totalRuns) {
    int testStartIndex = i * 5;
    int testEndIndex = testStartIndex + totalRuns - 1;
    Map<String, List<String>> result = new LinkedHashMap<>();

    int index = 0;
    for (TrainingData each : datasetPerQuery) {
      if (index >= testStartIndex && index <= testEndIndex) {
        index++;
        continue;
      }
      result.put(each.getQueryId(), each.getDocWithScoreList());
      index++;
    }
    return result;
  }

}
