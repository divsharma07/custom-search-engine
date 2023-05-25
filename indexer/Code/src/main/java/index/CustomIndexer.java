package index;

import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import model.Document;
import model.InvertedIndexTerm;
import model.ParsedDocumentBatch;
import model.Query;
import model.RetrievalResponse;
import parse.DocParser;
import util.Configuration;
import util.Helper;

import static util.Helper.initDirectory;

public class CustomIndexer implements Indexer {
  private String indexName;
  private boolean isStemmed;
  private DocParser docParser;
  private List<Document> docs;
  private ExecutorService executorService;
  private TreeMap<String, Integer> termIdMap;
  private Map<String, Integer> docIdMap;
  private TreeMap<Integer, String> idTermMap;
  private Map<Integer, String> idToFileMap;
  private Map<String, Integer> docIdLengthMap;
  private String collectionDirectory;

  public CustomIndexer(String indexName, boolean isStemmed, ExecutorService executorService, String collectionDirectory) {
    this.indexName = indexName;
    this.isStemmed = isStemmed;
    docParser = new DocParser(indexName, this, isStemmed);
    this.executorService = executorService;
    this.collectionDirectory = collectionDirectory;
  }

  @Override
  public void populateIndexAndMetadata() {
    TreeMap<String, Integer> termIdMap = new TreeMap<>();
    Map<String, Integer> docIdMap = new HashMap<>();
    Map<String, Integer> docIdLengthMap = new HashMap<>();
    int totalCollectionFrequency = 0;
    try {
      initialiseDirectories();
    } catch (IOException e) {
      e.printStackTrace();
    }
    int batchId = 0;
    try {
      File directoryFolder = new File(collectionDirectory);
      File[] files = directoryFolder.listFiles();
      // approximately 4 files have 1000 documents
      // if the thread count for ExecutorService are higher, more documents will be processed in parallel, set it to 1 for 1000 docs
      List<List<File>> partitionedList = Lists.partition(List.of(files), 4);
      List<Callable<ParsedDocumentBatch>> parseCallable = new ArrayList<>();

      for (List<File> fileBatch : partitionedList) {
        parseCallable.add(() -> {   // Lambda Expression
          return docParser.parseDocsPartially(fileBatch);
        });
      }
      List<Future<ParsedDocumentBatch>> batchResponseFuture = executorService.invokeAll(parseCallable);
      List<ParsedDocumentBatch> parsedBatchResponse = awaitAndReturnParsedBatch(batchResponseFuture);
      List<Callable<Void>> saveFileCallable = new ArrayList<>();
      for (ParsedDocumentBatch batch : parsedBatchResponse) {
        totalCollectionFrequency += batch.getTotalTokens();
        docIdMap.putAll(batch.getDocIdMap());
        docIdLengthMap.putAll(batch.getDocIdLenMap());
        updateGlobalTermIdMap(batch.getDocs(), termIdMap);
        Collection<InvertedIndexTerm> partialInvertedIndex = getInvertedIndex(batch, termIdMap);
        int currBatchId = batchId;
        saveFileCallable.add(() -> {   // Lambda Expression
          Helper.saveInvertedIndexToFile(partialInvertedIndex, currBatchId,
                  isStemmed ? Configuration.stemmedIndexFilesDirectory : Configuration.indexFilesDirectory, false, isStemmed);
          return null;
        });

        batchId++;
      }
      List<Future<Void>> saveResponseFuture = executorService.invokeAll(saveFileCallable);
      awaitResponse(saveResponseFuture);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    // save metadata to files
    Helper.saveIntToFile(totalCollectionFrequency, isStemmed ? Configuration.stemmedTotalCollectionFreq : Configuration.totalCollectionFreq);
    Helper.saveMapToFileStringToId(isStemmed ? Configuration.stemmedDocIdFile : Configuration.docIdFile, docIdMap);
    Helper.saveMapToFileStringToId(isStemmed ? Configuration.stemmedDocIdLenMap : Configuration.docIdLenMap, docIdLengthMap);
    Helper.saveMapToFileStringToId(isStemmed ? Configuration.stemmedTermIdFile : Configuration.termIdFile, termIdMap);
    TreeMap<Integer, String> globalIdTermMap =
            new TreeMap<>(termIdMap.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey)));
    Helper.saveMapToFileIdToString(isStemmed ? Configuration.stemmedIdTermFile : Configuration.idTermFile, globalIdTermMap);
    System.out.println("Started Merging Files");
    String finalIndexDirectory = mergePartialIndexFiles(globalIdTermMap);

    List<Integer> thresholdList = getIdThresholdList(globalIdTermMap.size(), 10);
    Map<Integer, String> idToFileMap = initializeSplitMap(globalIdTermMap, 10);
    Helper.saveMapToFileIdToString(isStemmed ? Configuration.stemmedSearchMap : Configuration.searchMap, idToFileMap);
    System.out.println("Started splitting and compressing files");
    splitMergedIndexFileAndCompress(thresholdList, globalIdTermMap, finalIndexDirectory);
  }

  private void initialiseDirectories() throws IOException {
    if (isStemmed) {
      initDirectory(Configuration.stemmedRoot);
      initDirectory(Configuration.stemmedIndexFilesDirectory);
      initDirectory(Configuration.stemmedFinalSplitIndexFile);
      initDirectory(Configuration.stemmedFinalSplitIndexFileCompressed);
    } else {
      initDirectory(Configuration.unStemmedRoot);
      initDirectory(Configuration.indexFilesDirectory);
      initDirectory(Configuration.finalSplitIndexFile);
      initDirectory(Configuration.finalSplitIndexFileCompressed);
    }
  }

  private void awaitResponse(List<Future<Void>> future) {
    for (Future<Void> each : future) {
      try {
        each.get();
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
    }
  }

  private List<ParsedDocumentBatch> awaitAndReturnParsedBatch(List<Future<ParsedDocumentBatch>> responseFuture) {
    List<ParsedDocumentBatch> result = new ArrayList<>();
    for (Future<ParsedDocumentBatch> each : responseFuture) {
      try {
        result.add(each.get());
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
    }
    return result;
  }


  private List<Integer> getIdThresholdList(int size, int split) {
    List<Integer> result = new ArrayList<>();
    int threshold = size / split;
    int currVal = 0;
    while (currVal < size) {
      result.add(currVal);
      currVal += threshold;
    }
    return result;
  }

  private Map<Integer, String> initializeSplitMap(Map<Integer, String> globalIdTermMap, int split) {
    Map<Integer, String> splitMap = new HashMap<>();
    int totalTerms = globalIdTermMap.size();
    int threshold = totalTerms / 10;
    int currTermIndex = 0;
    int fileIndex = 0;
    for (Map.Entry<Integer, String> each : globalIdTermMap.entrySet()) {
      splitMap.put(each.getKey(), String.valueOf(fileIndex));
      currTermIndex++;
      if (currTermIndex == threshold) {
        currTermIndex = 0;
        fileIndex++;
      }
    }

    return splitMap;
  }

  private void splitMergedIndexFileAndCompress(List<Integer> thresholdList, TreeMap<Integer, String> globalIdTermMap, String finalIndexDirectory) {
    List<Callable<Void>> callableList = new ArrayList<>();
    for (int i = 1; i < thresholdList.size(); i++) {
      callableList.add(new SplitOrchestrator(finalIndexDirectory, globalIdTermMap, thresholdList, i, isStemmed));
    }
    try {
      awaitResponse(executorService.invokeAll(callableList));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private String mergePartialIndexFiles(Map<Integer, String> globalIdTermMap) {
    File directoryFolder = new File(isStemmed ? Configuration.stemmedIndexFilesDirectory : Configuration.indexFilesDirectory);
    File[] files = directoryFolder.listFiles();
    int totalFiles = files.length / 2;
    String inputDirectory = isStemmed ? Configuration.stemmedIndexFilesDirectory : Configuration.indexFilesDirectory;
    String outputDirectory = inputDirectory + File.separator + "merged";
    while (totalFiles > 1) {
      List<Future<Void>> response = mergeFilePairs(inputDirectory, outputDirectory, totalFiles, globalIdTermMap);
      if (totalFiles % 2 == 0) {
        totalFiles = totalFiles / 2;
      } else {
        totalFiles = totalFiles / 2 + 1;
      }
      awaitAndSaveMergedFiles(response, outputDirectory);
      inputDirectory = outputDirectory;
      outputDirectory = outputDirectory + File.separator + "merged";
    }
    return inputDirectory;
  }

  private void awaitAndSaveMergedFiles(List<Future<Void>> response, String outputDirectory) {
    int index = 0;
    for (Future<Void> each : response) {
      try {
        each.get();
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
    }
  }

  private List<Future<Void>> mergeFilePairs(String inputDirectory, String outputDirectory, int totalFiles, Map<Integer, String> globalTermIdMap) {
    List<Future<Void>> result = new ArrayList<>();
    List<Callable<Void>> callableTasks = new ArrayList<>();
    File directoryFolder = new File(inputDirectory);
    File[] files = directoryFolder.listFiles();
    int newIndex = 0;
    // odd files, then copy the last file to the output directory
    if(totalFiles % 2 != 0) {
      String catalog = File.separator + "catalog" + (totalFiles/2);
      String index = File.separator + "index" + (totalFiles/2);
      try {
        initDirectory(outputDirectory);
        Helper.copyFile(inputDirectory + File.separator + "catalog" +  (totalFiles-1), outputDirectory + catalog);
        Helper.copyFile(inputDirectory +  File.separator + "index" +(totalFiles-1), outputDirectory + index);
      } catch (IOException e) {
        e.printStackTrace();
      }
      totalFiles--;
    }
    for (int i = 0; i < totalFiles; i += 2) {
      callableTasks.add(new MergeOrchestrator(i, i + 1, files, globalTermIdMap, newIndex++, outputDirectory, isStemmed));
    }
    try {
      result = executorService.invokeAll(callableTasks);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    return result;
  }


  private Collection<InvertedIndexTerm> getInvertedIndex(ParsedDocumentBatch batchData, Map<String, Integer> globalTermIdMap) {
    List<Document> currDocs = batchData.getDocs();
    TreeMap<Integer, InvertedIndexTerm> termIndexMap = new TreeMap<>();
    for (Document doc : currDocs) {
      for (Map.Entry<String, List<Integer>> term : doc.getTerms().entrySet()) {
        String currTerm = term.getKey();
        Integer currTermId = globalTermIdMap.get(currTerm);
        if (!termIndexMap.containsKey(currTermId)) {
          termIndexMap.put(currTermId, new InvertedIndexTerm(currTerm, currTermId));
        }
        InvertedIndexTerm currInvertedIndex = termIndexMap.get(currTermId);
        currInvertedIndex.addDoc(doc.getShortId(), term.getValue());
      }
    }

    return termIndexMap.values();
  }



  private void updateGlobalTermIdMap(List<Document> docs, Map<String, Integer> termIdMap) {
    for (Document doc : docs) {
      Map<String, List<Integer>> docTerms = doc.getTerms();
      for (String termString : docTerms.keySet()) {
        if (!termIdMap.containsKey(termString)) {
          termIdMap.put(termString, Helper.autogenerateId());
        }
      }
    }
  }

  @Override
  public boolean indexExists() {
    if (isStemmed) {
      return Helper.checkDirectoryExists(Configuration.stemmedDocIdFile) &
              Helper.checkDirectoryExists(Configuration.stemmedTermIdFile) &
              Helper.checkDirectoryExists(Configuration.stemmedIndexFilesDirectory) &
              Helper.checkDirectoryExists(Configuration.stemmedFinalSplitIndexFile) &
              Helper.checkDirectoryExists(Configuration.stemmedFinalSplitIndexFileCompressed) &
              Helper.checkDirectoryExists(Configuration.stemmedSearchMap) &
              Helper.checkDirectoryExists(Configuration.stemmedIdTermFile) &
              Helper.checkDirectoryExists(Configuration.stemmedDocIdLenMap);
    } else {
      return Helper.checkDirectoryExists(Configuration.docIdFile) &
              Helper.checkDirectoryExists(Configuration.termIdFile) &
              Helper.checkDirectoryExists(Configuration.indexFilesDirectory) &
              Helper.checkDirectoryExists(Configuration.finalSplitIndexFile) &
              Helper.checkDirectoryExists(Configuration.finalSplitIndexFileCompressed) &
              Helper.checkDirectoryExists(Configuration.searchMap) &
              Helper.checkDirectoryExists(Configuration.idTermFile) &
              Helper.checkDirectoryExists(Configuration.docIdLenMap);
    }
  }


  @Override
  public InvertedIndexTerm search(String word) throws IOException {
    var idToFileMap = getIdToFileMap();
    var termIdMap = getTermIdMap();
    var idTermMap = getIdTermMap();

    Integer termId = termIdMap.get(word);

    if(termId == null) return null;
    String directory = (isStemmed ? Configuration.stemmedFinalSplitIndexFileCompressed : Configuration.finalSplitIndexFileCompressed) + File.separator;
    int fileIndex = 0;
    try {
      fileIndex = Integer.parseInt(idToFileMap.get(termId));

    }
    catch (NumberFormatException e) {
      System.out.println("the term id is" + termId);
      System.out.println("value in map is " + idToFileMap.get(termId));
    }
    String catalogFile = directory + "catalog" + fileIndex;
    String indexFile = directory + "index" + fileIndex;
    String decompCatalogFile = catalogFile + "decomp";
    String decompIndexFile = indexFile + "decomp";
    if(!Helper.fileExists(decompCatalogFile)) {
      Helper.decompressFile(catalogFile, decompCatalogFile);
    }
    if(!Helper.fileExists(decompIndexFile)) {
      Helper.decompressFile(indexFile, decompIndexFile);
    }
    InvertedIndexTerm result = Helper.readParticularTerm(decompIndexFile, decompCatalogFile, idTermMap, termId);
    return result;
  }

  @Override
  public void close() {
    String directory = (isStemmed ? Configuration.stemmedFinalSplitIndexFileCompressed : Configuration.finalSplitIndexFileCompressed) + File.separator;
    File[] files = new File(directory).listFiles();
    for(File file : files) {
      if(file.getName().contains("decomp")) {
        Helper.deleteFile(file.getAbsolutePath());
      }
    }
  }

  @Override
  public List<Document> getIndexedDocs() {
    return null;
  }

  @Override
  public Set<String> getVocabulary() {
    if (indexExists()) {
      return Helper.getMapFromFileStringToId(isStemmed ? Configuration.stemmedTermIdFile : Configuration.termIdFile).keySet();
    } else {
      return new HashSet<>();
    }
  }

  @Override
  public int getAvgDocLength() {
    if (indexExists()) {
      int totalTerms = getTotalCollectionTerms();
      int totalDocs = getDocIdMap().size();
      return totalTerms / totalDocs;
    } else return 0;
  }

  @Override
  public TreeMap<String, Integer> getTermIdMap() {
    if (termIdMap != null) {
      return termIdMap;
    }
    this.termIdMap = new TreeMap<>(Helper.getMapFromFileStringToId(isStemmed ? Configuration.stemmedTermIdFile : Configuration.termIdFile));
    return termIdMap;
  }

  @Override
  public Map<String, Integer> getDocIdLenMap() {
    if (docIdLengthMap != null) {
      return docIdLengthMap;
    }
    this.docIdLengthMap = new TreeMap<>(Helper.getMapFromFileStringToId(isStemmed ? Configuration.stemmedDocIdLenMap : Configuration.docIdLenMap));
    return docIdLengthMap;
  }

  @Override
  public Map<String, Integer> getDocIdMap() {
    if (docIdMap != null) {
      return docIdMap;
    }
    this.docIdMap = Helper.getMapFromFileStringToId(isStemmed ? Configuration.stemmedDocIdFile : Configuration.docIdFile);
    return docIdMap;
  }

  @Override
  public TreeMap<Integer, String> getIdTermMap() {
    if (idTermMap != null) {
      return idTermMap;
    }
    this.idTermMap = new TreeMap<>(Helper.getMapFromFileIdToString(isStemmed ? Configuration.stemmedIdTermFile : Configuration.idTermFile));
    return idTermMap;
  }

  @Override
  public Map<Integer, String> getIdToFileMap() {
    if (idToFileMap != null) {
      return idToFileMap;
    }
    this.idToFileMap = Helper.getMapFromFileIdToString(isStemmed ? Configuration.stemmedSearchMap : Configuration.searchMap);
    return idToFileMap;
  }

  @Override
  public int getTotalCollectionTerms() {
    return Helper.getIntFromFile(isStemmed ? Configuration.stemmedTotalCollectionFreq : Configuration.totalCollectionFreq);
  }
}
