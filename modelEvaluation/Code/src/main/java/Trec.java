import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import javax.swing.*;

import picocli.CommandLine;

/**
 * Orchestrated the TREC based metric evaluation.
 */
@CommandLine.Command(name = "trec_eval", mixinStandardHelpOptions = true, version = "trec_eval 1.0",
        description = "Evaluates relevance scoring algorithms for information retrieval systems")
public class Trec implements Callable<Integer> {
  @CommandLine.Parameters(index = "0", description = "The qrel file that contains confirmed relevance grades.")
  private static String qrelFile;

  @CommandLine.Parameters(index = "1", description = "The trec file that contains each query and corresponding scores in decreasing order.")
  private static String trecFile;
  @CommandLine.Option(names = "-q")
  private static boolean allQueriesFlag;

  @Override
  public Integer call() throws Exception {
    QrelResult qrel = parseQrelFile();
    TrecResult trec = parseTrecInput();
    computeAndPrintRelevanceMetrics(qrel, trec);

    return 0;
  }


  public static void main(String[] args) throws IOException {
    int exitCode = new CommandLine(new Trec()).execute(args);
    //System.exit(exitCode);
  }

  /**
   * This method prints relevance metrics methond in the printResult method
   *
   * @param qrel
   * @param trec
   */
  private void computeAndPrintRelevanceMetrics(QrelResult qrel, TrecResult trec) {
    List<Double> recalls = new ArrayList<>(List.of(0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0));
    List<Integer> cutOffs = new ArrayList<>(List.of(4, 9, 14, 19, 29, 99, 199, 499, 999));
    int numTopics = 0;
    Map<Integer, Integer> allRelMap = qrel.getAllRelMap();
    Map<Integer, Map<String, Integer>> qrelMap = qrel.getQrelMap();
    Map<Integer, Map<String, Integer>> gradedRelevance = qrel.getGradedRelevance();

    int totalDocsRetrieved = 0;
    int totalRelevantDocsRetrieved = 0;
    int totalRelevantDocs = 0;
    int totalQueries = qrel.getQrelMap().size();
    double[] avgPrecisionAtCutOff = new double[cutOffs.size()];
    double[] avgPrecisionAtRecall = new double[recalls.size()];
    double[] sumPrecisionAtCutOff = new double[cutOffs.size()];
    double[] sumPrecisionAtRecall = new double[recalls.size()];
    double[] sumRecallAtCutOff = new double[cutOffs.size()];
    double[] avgRecallAtCutOff = new double[cutOffs.size()];
    double[] sumF1AtCutOff = new double[cutOffs.size()];
    double[] avgF1AtCutOff = new double[cutOffs.size()];

    double sumAveragePrecision = 0;
    double sumRPrecision = 0;
    double sumnDcg = 0;

    double meanAvgPrecision = 0;
    double avgRPrecision = 0;
    double averagenDcg = 0;
    for (Map.Entry<Integer, Map<String, Double>> trecMap : trec.getTrecMap().entrySet()) {
      int queryId = trecMap.getKey();
      if (allRelMap.get(queryId) == 0) {
        continue;
      }
      numTopics++;

      List<Double> precList = new ArrayList<>();
      List<Double> recallList = new ArrayList<>();
      List<Double> f1List = new ArrayList<>();
      List<Double> dcgList = new ArrayList<>();

      int numDocsRetrieved = 0;
      int numRelevantDocsRetreived = 0;
      double sumPrecision = 0;
      LinkedHashMap<String, Double> sortedMap = trecMap.getValue().entrySet()
              .stream()
              .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
              .collect(Collectors.toMap(
                      Map.Entry::getKey,
                      Map.Entry::getValue,
                      (oldValue, newValue) -> oldValue, LinkedHashMap::new));
      for (Map.Entry<String, Double> each : sortedMap.entrySet()) {
        numDocsRetrieved++;
        String docId = each.getKey();
        Integer relevance = qrelMap.get(queryId).get(docId);

        if (relevance != null && relevance > 0) {
          sumPrecision += (relevance * (1 + numRelevantDocsRetreived)) * 1.0 / numDocsRetrieved;
          numRelevantDocsRetreived += relevance;
        }

        double precision = numRelevantDocsRetreived * 1.0 / numDocsRetrieved;
        double recall = numRelevantDocsRetreived * 1.0 / allRelMap.get(queryId);


        double f1 = 0;
        if (precision + recall != 0) {
          f1 = 2 * ((precision * recall) / (precision + recall));
        }
        precList.add(precision);
        recallList.add(recall);
        f1List.add(f1);
        if (numDocsRetrieved > 1000) {
          break;
        }
      }

      double averagePrecision = sumPrecision / allRelMap.get(queryId);
      double finalRecall = numRelevantDocsRetreived * 1.0 / allRelMap.get(queryId);

      for (int i = numDocsRetrieved + 1; i <= 1000; i++) {
        double finalPrecision = numRelevantDocsRetreived * 1.0 / i;
        double f1 = 0;
        if (finalPrecision + finalRecall != 0) {
          f1 = 2 * ((finalPrecision * finalRecall) / (finalPrecision + finalRecall));
        }
        precList.add(finalPrecision);
        recallList.add(finalRecall);
        f1List.add(f1);
      }

      List<Double> precAtCutOffs = new ArrayList<>();
      List<Double> recallAtCutOffs = new ArrayList<>();
      List<Double> f1AtCutOffs = new ArrayList<>();

      for (int cutoff : cutOffs) {
        precAtCutOffs.add(precList.get(cutoff));
      }

      for (int cutoff : cutOffs) {
        recallAtCutOffs.add(recallList.get(cutoff));
      }

      for (int cutoff : cutOffs) {
        f1AtCutOffs.add(f1List.get(cutoff));
      }

      double rPrecision = 0.0;
      if (allRelMap.get(queryId) > numDocsRetrieved) {
        rPrecision = numRelevantDocsRetreived * 1.0 / allRelMap.get(queryId);
      }
      else {
        rPrecision = precList.get(allRelMap.get(queryId));
      }
      //TODO: check if we really need to do the non integer part, seems redundant


      double maxPrecision = Integer.MIN_VALUE;


      // calculating interpolated precision now
      for (int i = 999; i >= 0; i--) {
        if (precList.get(i) > maxPrecision) {
          maxPrecision = precList.get(i);
        } else {
          precList.set(i, (maxPrecision));
        }
      }

      // now calculating precision at recall levels
      int i = 0;
      List<Double> precAtRecallLevels = new ArrayList<>();

      for (double recall : recalls) {
        while (i < 1000 && recallList.get(i) < recall) {
          i++;
        }
        if (i < 1000) {
          precAtRecallLevels.add(precList.get(i));
        } else {
          precAtRecallLevels.add(0.0);
        }
      }

      List<Double> nDcgList = computenDcg(sortedMap, gradedRelevance, queryId);
      double nDcg = nDcgList.get(gradedRelevance.get(queryId).size()-1);
      if (allQueriesFlag) {
        printResult(queryId, numDocsRetrieved, allRelMap.get(queryId), numRelevantDocsRetreived,
                listToArray(precAtRecallLevels), averagePrecision, listToArray(precAtCutOffs),
                listToArray(recallAtCutOffs), listToArray(f1AtCutOffs), rPrecision, cutOffs, recalls, nDcg);
        printPrecisionRecallCurve(listToArray(precAtRecallLevels), recalls, queryId);
      }

      totalDocsRetrieved += numDocsRetrieved;
      totalRelevantDocs += allRelMap.get(queryId);
      totalRelevantDocsRetrieved += numRelevantDocsRetreived;

      saveSum(sumPrecisionAtCutOff, precAtCutOffs);
      saveSum(sumRecallAtCutOff, recallAtCutOffs);
      saveSum(sumF1AtCutOff, f1AtCutOffs);
      saveSum(sumPrecisionAtRecall, precAtRecallLevels);

      sumAveragePrecision += averagePrecision;
      sumRPrecision += rPrecision;
      sumnDcg += nDcg;
    }

    saveAverage(avgPrecisionAtCutOff, sumPrecisionAtCutOff, numTopics);
    saveAverage(avgRecallAtCutOff, sumRecallAtCutOff, numTopics);
    saveAverage(avgF1AtCutOff, sumF1AtCutOff, numTopics);
    saveAverage(avgPrecisionAtRecall, sumPrecisionAtRecall, numTopics);

    meanAvgPrecision = sumAveragePrecision / numTopics;
    avgRPrecision = sumRPrecision / numTopics;
    averagenDcg = sumnDcg / numTopics;

    printResult(numTopics, totalDocsRetrieved, totalRelevantDocs, totalRelevantDocsRetrieved,
            avgPrecisionAtRecall, meanAvgPrecision,
            avgPrecisionAtCutOff, avgRecallAtCutOff,
            avgF1AtCutOff, avgRPrecision, cutOffs, recalls, averagenDcg);
    printPrecisionRecallCurve(avgPrecisionAtRecall, recalls, -1);
  }

  private void printPrecisionRecallCurve(double[] precAtRecallLevels, List<Double> recalls, int queryId) {
    XYSeries series = new XYSeries("Precision-Recall Curve");
    for(int i = 0; i < recalls.size(); i++) {
      series.add(recalls.get(i), (Double) precAtRecallLevels[i]);
    }

    XYSeriesCollection dataset = new XYSeriesCollection();
    dataset.addSeries(series);

    // Create a precision-recall chart from the dataset
    String graphName = "";

    if(queryId == -1) {
      graphName = "Precision-Recall Curve - All Queries";
    }
    else {
      graphName = "Precision-Recall Curve - Query No:" + queryId;
    }
    JFreeChart chart = ChartFactory.createXYLineChart(
            graphName, "Recall", "Precision", dataset,
            PlotOrientation.VERTICAL, true, true, false);

    // Set the chart background color
    chart.setBackgroundPaint(Color.WHITE);

    // Customize the chart plot
    XYPlot plot = (XYPlot) chart.getPlot();
    plot.setBackgroundPaint(Color.WHITE);
    plot.setDomainGridlinePaint(Color.BLACK);
    plot.setRangeGridlinePaint(Color.BLACK);

    // Customize the x-axis
    NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
    xAxis.setAutoRangeIncludesZero(false);
    xAxis.setTickMarkInsideLength(2.0f);
    xAxis.setTickMarkOutsideLength(0.0f);
    xAxis.setMinorTickCount(0);

    // Customize the y-axis
    NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
    yAxis.setTickMarkInsideLength(2.0f);
    yAxis.setTickMarkOutsideLength(0.0f);
    yAxis.setMinorTickCount(0);

    // Customize the plot renderer
    XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
    renderer.setSeriesShapesVisible(0, true);
    plot.setRenderer(renderer);

    // Create a chart panel to display the chart
    ChartPanel chartPanel = new ChartPanel(chart);
    chartPanel.setPreferredSize(new Dimension(500, 300));

    // Create a new frame to display the chart panel
    JFrame frame = new JFrame("Precision-Recall Curve");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.add(chartPanel);
    frame.pack();
    frame.setVisible(true);
  }

  private List<Double> computenDcg(LinkedHashMap<String, Double> map, Map<Integer,
          Map<String, Integer>> gradedRelevance, int queryId) {
    int index = 1;
    List<Double> nDcgList = new ArrayList<>();
    double dcg = 0;
    List<Double> idealDcg = getIdealDcg(gradedRelevance.get(queryId));
    for (Map.Entry<String, Double> each : map.entrySet()) {
      String docId = each.getKey();
      if(index > idealDcg.size()) return nDcgList;
      if(gradedRelevance.get(queryId).get(docId) == null) {
        if(nDcgList.size() == 0) {
          nDcgList.add(0.0);
          index++;
        }
        else {
          nDcgList.add(nDcgList.get(nDcgList.size() - 1));
        }
        continue;
      }
      int relevance = gradedRelevance.get(queryId).get(docId);
      if (index == 1) {
        dcg = relevance;
        nDcgList.add(dcg/idealDcg.get(0));
        index++;
        continue;
      }
      dcg += relevance * 1.0 / log2(index);
      nDcgList.add(dcg/idealDcg.get(index-1));
      index++;
    }

    return nDcgList;
  }

  private List<Double> getIdealDcg(Map<String, Integer> gradedRelevance) {
    // sorting documents by grades
    LinkedHashMap<String, Integer> sortedMap = gradedRelevance.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
            .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (oldValue, newValue) -> oldValue, LinkedHashMap::new));

    List<Double> result = new ArrayList<>();
    int index = 1;
    double idealDcg = 0;
    for(Map.Entry<String, Integer> each : sortedMap.entrySet()) {
      String docId = each.getKey();
      int relevance = gradedRelevance.get(docId);
      if (index == 1) {
        idealDcg = relevance;
        result.add(idealDcg);
        index++;
        continue;
      }

      idealDcg += relevance * 1.0 / log2(index);
      index++;
      result.add(idealDcg);
    }

    return result;
  }

  private void saveSum(double[] sums, List<Double> values) {
    for (int j = 0; j < sums.length; j++) {
      sums[j] += values.get(j);
    }
  }

  private static double log2(int n) {
    return Math.log(n) / Math.log(2);
  }

  private void printResult(int queryId, int numDocsRetrieved, Integer numRelevantDocs, int numRelevantDocsRetrieved,
                           double[] precAtRecallLevels,
                           double averagePrecision, double[] precAtCutOffs, double[] recallAtCutOffs,
                           double[] f1AtCutOffs, double rPrecision, List<Integer> cutOffs, List<Double> recalls, double averagenDcg) {

    System.out.printf("\nQueryid (Num):    %5d\n", queryId);
    System.out.println("Total number of documents over all queries");
    System.out.printf("    Retrieved:    %5d\n", numDocsRetrieved);
    System.out.printf("    Relevant:     %5d\n", numRelevantDocs);
    System.out.printf("    Rel_ret:      %5d\n", numRelevantDocsRetrieved);
    System.out.println("Interpolated Recall - Precision Averages:\n");
    printAtInterpolatedRecalls(precAtRecallLevels, recalls);
    System.out.println("Average precision (non-interpolated) for all rel docs(averaged over queries)");
    System.out.printf("                  %.4f\n", averagePrecision);
    System.out.println("Precision:");
    printWithCutOff(precAtCutOffs, cutOffs);
    System.out.println("Recall:");
    printWithCutOff(recallAtCutOffs, cutOffs);
    System.out.println("F1:");
    printWithCutOff(f1AtCutOffs, cutOffs);
    System.out.println("R-Precision (precision after R (= num_rel for a query) docs retrieved):");
    System.out.printf("    Exact:        %.4f\n", rPrecision);
    System.out.println("nDCG:");
    System.out.printf("                  %.4f\n", averagenDcg);
  }

  private void saveAverage(double[] averages, double[] sums, int numTopics) {
    for (int i = 0; i < averages.length; i++) {
      averages[i] = sums[i] * 1.0 / numTopics;
    }
  }

  private void printAtInterpolatedRecalls(double[] precAtRecallLevels, List<Double> recalls) {
    for (int i = 0; i < recalls.size(); i++) {
      System.out.printf("    at %.2f       %.4f\n", recalls.get(i), precAtRecallLevels[i]);
    }
  }

  private void printWithCutOff(double[] valuesAtCutOffs, List<Integer> cutOffs) {
    for (int i = 0; i < cutOffs.size(); i++) {
      System.out.printf("  At    %d docs:   %.4f\n", cutOffs.get(i) + 1, valuesAtCutOffs[i]);
    }
  }

  public double[] listToArray(List<Double> list) {
    double[] doubleArray = new double[list.size()];
    for (int i = 0; i < list.size(); i++) {
      doubleArray[i] = list.get(i);
    }
    return doubleArray;
  }

  private TrecResult parseTrecInput() throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(trecFile));
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

    return new TrecResult(trecMap, trecList);
  }

  private QrelResult parseQrelFile() throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(qrelFile));
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


}
