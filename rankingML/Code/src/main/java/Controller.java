import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dto.TrainingData;
import util.Helper;
public class Controller {
  private static final String[] trecFiles = new String[] { "ESDefault.txt", "OkapiTf.txt", "TfIdf.txt",
          "OkapiBM25.txt", "UnigramLMLaplace.txt", "UnigramLMJM.txt"};
  private static final String trainingFileName = "training.txt";

  private static final double TEST_TRAIN_SPLIT = 0.2;
  public static void main(String[] args) throws Exception {
    DataProcessor dataProcessor = new DataProcessor();
//    if(!dataProcessor.trainingFileExist(trainingFileName)) {
//
//    }
    List<String> dataset = dataProcessor.getFeatureMatrixData(trecFiles, trainingFileName);
    Classifier classifier = new Classifier(dataset);

    classifier.orchestrateModelRun(TEST_TRAIN_SPLIT);
  }
}
