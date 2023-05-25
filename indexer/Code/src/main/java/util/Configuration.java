package util;

import java.io.File;

public class Configuration {
  public static final String absolutePath = new File("").getAbsolutePath();
  private static final String stemmedPrefix = "stemmedIndex" + File.separator;
  private static final String unStemmedPrefix = "unstemmedIndex" + File.separator;
  private static final String resultsPrefix = "Results" + File.separator;
  public static final String stemmedRoot = absolutePath + File.separator + resultsPrefix + stemmedPrefix;
  public static final String unStemmedRoot = absolutePath + File.separator + resultsPrefix + unStemmedPrefix;
  public static final String totalCollectionFreq = unStemmedRoot + "totalCollectionFreq";
  public static final String docIdFile = unStemmedRoot + "docIdFile";
  public static final String termIdFile = unStemmedRoot + "termIdFile";
  public static final String idTermFile = unStemmedRoot + "idTerm";
  public static final String searchMap = unStemmedRoot + "searchMap";
  public static final String indexFilesDirectory = unStemmedRoot + "Index";
  public static final String finalSplitIndexFile = unStemmedRoot + "MergedSplit";
  public static final String finalSplitIndexFileCompressed = unStemmedRoot + "MergedSplitCompressed";
  public static final String docIdLenMap = unStemmedRoot + "docIdLen";
  public static final String stemmedDocIdFile = stemmedRoot + "docIdFile";
  public static final String stemmedTermIdFile = stemmedRoot + "termIdFile";
  public static final String stemmedIdTermFile = stemmedRoot + "idTerm";
  public static final String stemmedIndexFilesDirectory = stemmedRoot + "Index";
  public static final String stemmedFinalSplitIndexFile = stemmedRoot + "MergedSplit";
  public static final String stemmedFinalSplitIndexFileCompressed = stemmedRoot + "MergedSplitCompressed";
  public static final String stemmedSearchMap = stemmedRoot + "searchMap";
  public static final String stemmedTotalCollectionFreq = stemmedRoot + "totalCollectionFreq";
  public static final String stemmedDocIdLenMap = stemmedRoot + "docIdLen";
}