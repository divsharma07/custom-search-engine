import weka.classifiers.AbstractClassifier;
import weka.classifiers.Evaluation;
import weka.core.Instance;
import weka.core.Instances;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Helper {
  public static StringBuilder arffHeaders(List<String> nGrams, boolean isRegression, String prefix) {
    StringBuilder sb = new StringBuilder();
    sb.append("@RELATION ML").append("\n");

    for(String nGram: nGrams) {
      nGram = nGram.replace(" ", "_");
      sb.append("@ATTRIBUTE ").append(prefix).append(nGram).append(" NUMERIC").append("\n");
    }
    if(isRegression) {
      sb.append("@ATTRIBUTE label NUMERIC").append("\n");
    }
    else {
      sb.append("@ATTRIBUTE label {0, 1}").append("\n");
    }
    sb.append("@DATA").append("\n");

    return sb;
  }

  /**
   * Given a map of mail id's and their scores, returns a list of strings with these values appended.
   * @param mapMailIdScores the map containing mail ids and their corresponding scores.
   * @return
   */
  public static List<String> addIdToDataSet(Map<String, String> mapMailIdScores) {
    List<String> result = new ArrayList<>();
    for(Map.Entry<String, String> each: mapMailIdScores.entrySet()) {
      String scores = each.getValue();
      result.add(each.getKey() + " " + scores);
    }

    return result;
  }


  /**
   * Writes the string to a file
   * @param dataSet the string that contains the data
   * @param fileName the name of the file
   * @throws IOException thrown if fine not found.
   */
  public static void writeToFile(String dataSet, String fileName) throws IOException {
    BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
    writer.write(dataSet);
    writer.close();
  }

  /**
   * Takes a list of strings and returns a StringBuilder with newLine character.
   * @param values the list of string.
   * @return
   */
  public static StringBuilder listToStringBuilder(List<String> values) {
    StringBuilder sb = new StringBuilder();
    for (String value : values) {
      sb.append(value).append("\n");
    }
    return sb;
  }

  public static StringBuilder getSortedMapString(Map<String, Double> mailIdPredictionMap,
                                    String modelName, boolean isDescending, int k) {
    String suffix = modelName;
    StringBuilder result = new StringBuilder();
    Map<String, Double> sortedMap;
    if(isDescending) {
      suffix += " Spams";
      sortedMap = mailIdPredictionMap.entrySet()
              .stream()
              .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
              .collect(Collectors.toMap(
                      Map.Entry::getKey,
                      Map.Entry::getValue,
                      (v1, v2) -> v1,
                      LinkedHashMap::new
              ));
    } else {
      suffix += " Hams";
      sortedMap = mailIdPredictionMap.entrySet()
              .stream()
              .sorted(Map.Entry.comparingByValue())
              .collect(Collectors.toMap(
                      Map.Entry::getKey,
                      Map.Entry::getValue,
                      (v1, v2) -> v1,
                      LinkedHashMap::new
              ));
    }


    int index = 0;
    result.append("Top ").append(suffix).append("\n");
    for (Map.Entry<String, Double> entry : sortedMap.entrySet()) {
      if (index == k) {
        break;
      }
      result.append("Key : " + entry.getKey()  + " Value : " + entry.getValue()).append("\n");
      index++;
    }

    return result;
  }

  /**
   * Gets all the ngrams present in a file
   * @param ngramsFile the name of the file
   * @return returns a set of ngrams.
   * @throws IOException
   */
  public static Set<String> getNgramsFromFile(String ngramsFile) throws IOException {
    Set<String> result = new HashSet<>();
    BufferedReader reader = new BufferedReader(new FileReader(ngramsFile));

    String line = "";
    while((line = reader.readLine()) != null) {
      line = line.replace("%", "");
      result.add(line);
    }

    return result;
  }

  /**
   * Returns a list of custom ngrams.
   * @return
   */
  public static Set<String> getCustomNgrams() {
    Set<String> nGrams = new HashSet<>();
    nGrams.add("free");
    nGrams.add("win");
    nGrams.add("porn");
    nGrams.add("lottery");
    nGrams.add("erectile dysfunction");
    nGrams.add("click here");
    return nGrams;
  }

  /**
   * Computes and prints the result of a ML algorithm
   * @param model the name of the model being used
   * @param classifier the classifier
   * @param allData test and train data combines, with the mail ids
   * @param testInstances test data enumeration
   * @param trainInstances train data enumeration
   * @param trainData train data instances
   * @param isRegression identifies if this is a regression algorithm
   * @throws Exception
   */
  public static void computeAndPrintResult(Classifier1.Model model, AbstractClassifier classifier, List<String> allData,
                                     Enumeration testInstances, Enumeration trainInstances,
                                     Instances trainData, boolean isRegression, String uniquePrefix) throws Exception {

    Map<String, Double> mailIdPredictionMap = new HashMap<>();
    int index = 0;
    index = addPredictions(trainInstances, mailIdPredictionMap, index, allData, classifier);
    addPredictions(testInstances, mailIdPredictionMap, index, allData, classifier);
    Evaluation evaluation = new Evaluation(trainData);
    evaluation.evaluateModel(classifier, trainData);

    StringBuilder result = new StringBuilder();
    result.append(evaluation.toSummaryString()).append("\n");
    if(!isRegression) {
      result.append("Area under ROC ").append(evaluation.areaUnderROC(0)).append("\n");
    }
    result.append(Helper.getSortedMapString(mailIdPredictionMap, model.toString(), false, 10)).append("\n");
    result.append(Helper.getSortedMapString(mailIdPredictionMap, model.toString(), true, 10)).append("\n");
    Helper.writeToFile(result.toString(),"Results" + File.separator + model + uniquePrefix + ".txt");
    System.out.println(model + uniquePrefix + " Output Generated");
  }


  /**
   * Given instances of the data set updates a map that contains predictions
   * @param instances the instances of data set
   * @param mailIdPredictionMap the mail id to prediction map that needs to be updated
   * @param index
   * @param allData
   * @param classifier
   * @return
   * @throws Exception
   */
  private static int addPredictions(Enumeration instances, Map<String, Double> mailIdPredictionMap, int index, List<String> allData, AbstractClassifier classifier) throws Exception {
    while(instances.hasMoreElements()) {
      Instance currElement = (Instance) instances.nextElement();
      double result = classifier.classifyInstance(currElement);
      String mailId = allData.get(index).split("\\s+")[0];

      mailIdPredictionMap.put(mailId, result);
      index++;
    }
    return index;
  }
}
