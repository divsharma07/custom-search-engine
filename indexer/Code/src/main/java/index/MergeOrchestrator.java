package index;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import model.InvertedIndexTerm;
import util.Helper;

public class MergeOrchestrator implements Callable<Void> {
  private int file1Name;
  private int file2Name;
  private File[] files;
  private Map<Integer, String> globalTermIdMap;
  private int newFileIndex;
  private String outputDirectory;
  private boolean isStemmed;
  public MergeOrchestrator(int file1Name, int file2Name, File[] files,
                           Map<Integer, String> globalTermIdMap, int newFileIndex, String outputDirectory, boolean isStemmed) {
    this.file1Name = file1Name;
    this.file2Name = file2Name;
    this.files = files;
    this.globalTermIdMap = globalTermIdMap;
    this.newFileIndex = newFileIndex;
    this.outputDirectory = outputDirectory;
    this.isStemmed = isStemmed;
  }

  @Override
  public Void call() throws IOException {
    TreeMap<Integer, InvertedIndexTerm> file1Map = null;
    TreeMap<Integer, InvertedIndexTerm> file2Map = null;
    try {
      file1Map = Helper.readInvertedIndexFromFile(file1Name, files[file1Name].getParentFile().getAbsolutePath(), globalTermIdMap, 0, 0);
      file2Map = Helper.readInvertedIndexFromFile(file2Name, files[file2Name].getParentFile().getAbsolutePath(), globalTermIdMap, 0, 0);
    } catch (IOException e) {
      e.printStackTrace();
    }

    TreeMap<Integer, InvertedIndexTerm> termIdIndexMap = new TreeMap<>();
    int file1Index = 0;
    int file2Index = 0;
    List<InvertedIndexTerm> file1 = new ArrayList<>(file1Map.values());
    List<InvertedIndexTerm> file2 = new ArrayList<>(file2Map.values());
    while(file1Index < file1.size() && file2Index < file2.size()) {
      String file1Term = file1.get(file1Index).getTerm();
      Integer file1TermId = file1.get(file1Index).getTermId();
      String file2Term = file2.get(file2Index).getTerm();
      Integer file2TermId = file2.get(file2Index).getTermId();
      if(file1Term.compareTo(file2Term) < 0) {
        addToMap(file1.get(file1Index), termIdIndexMap);
        file1Index++;
      }
      else if(file1Term.compareTo(file2Term) > 0) {
        addToMap(file2.get(file2Index), termIdIndexMap);
        file2Index++;
      }
      // both terms are the same, then merge their contents
      else {
        InvertedIndexTerm mergedIndex = mergeDocumentsData(file1.get(file1Index), file2.get(file2Index));
        termIdIndexMap.put(file1TermId, mergedIndex);
        file1Index++;
        file2Index++;
      }
    }

    while(file1Index < file1.size()) {
      addToMap(file1.get(file1Index), termIdIndexMap);
      file1Index++;
    }

    while(file2Index < file2.size()) {
      addToMap(file2.get(file2Index), termIdIndexMap);
      file2Index++;
    }

    Helper.saveInvertedIndexToFile(termIdIndexMap.values(), newFileIndex, outputDirectory, false, isStemmed);
    return null;
  }

  // adds term to map, and merges if index already present
  private void addToMap(InvertedIndexTerm term, Map<Integer, InvertedIndexTerm> termIdIndexMap) {
    InvertedIndexTerm currTerm = new InvertedIndexTerm(term);
    if(termIdIndexMap.containsKey(term.getTermId())) {
      currTerm = mergeDocumentsData(new InvertedIndexTerm(term), termIdIndexMap.get(term.getTermId()));
    }
    termIdIndexMap.put(term.getTermId(), currTerm);
  }

  private InvertedIndexTerm mergeDocumentsData(InvertedIndexTerm term1, InvertedIndexTerm term2) {
    TreeMap<Integer, List<Integer>> map1 = term1.getDocIdPositionMap();
    TreeMap<Integer, List<Integer>> map2 = term2.getDocIdPositionMap();
    var mergedMap = new TreeMap<>(Stream.of(map1, map2).flatMap(m -> m.entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    return new InvertedIndexTerm(term1.getTerm(), term1.getTermId(), new TreeMap<>(mergedMap));
  }
}
