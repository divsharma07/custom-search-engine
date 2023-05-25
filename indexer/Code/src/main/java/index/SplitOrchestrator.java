package index;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import model.InvertedIndexTerm;
import util.Configuration;
import util.Helper;

public class SplitOrchestrator implements Callable<Void> {
  private String finalIndexDirectory;
  private TreeMap<Integer, String> globalIdTermMap;
  private List<Integer> thresholdList;
  private int index;
  private boolean isStemmed;

  public SplitOrchestrator(String finalIndexDirectory, TreeMap<Integer, String> globalIdTermMap, List<Integer> thresholdList, int index, boolean isStemmed) {
    this.finalIndexDirectory = finalIndexDirectory;
    this.globalIdTermMap = globalIdTermMap;
    this.thresholdList = thresholdList;
    this.index = index;
    this.isStemmed = isStemmed;
  }

  @Override
  public Void call() throws Exception {
    TreeMap<Integer, InvertedIndexTerm> splitInvertedIndices = Helper.readInvertedIndexFromFile(0, finalIndexDirectory,
            globalIdTermMap, thresholdList.get(index), thresholdList.get(index - 1));
    Helper.saveInvertedIndexToFile(splitInvertedIndices.values(), index - 1,
            isStemmed ? Configuration.stemmedFinalSplitIndexFile : Configuration.finalSplitIndexFile, true, isStemmed);
    return null;
  }
}
