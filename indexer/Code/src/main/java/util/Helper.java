package util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import index.Indexer;
import model.Document;
import model.InvertedIndexTerm;
import model.Query;
import model.RetrievalResponse;
import retrieve.RetrievalModel;

public class Helper {
  private static final AtomicInteger count = new AtomicInteger(0);


  public static int autogenerateId() {
    return count.getAndIncrement();
  }

  public static void saveDocumentList(List<Document> o, String filename) {
    FileOutputStream fos = null;
    ObjectOutputStream oop = null;

    try {
      fos = new FileOutputStream(filename);
      oop = new ObjectOutputStream(fos);
      oop.writeObject(o);
      oop.flush();
      fos.flush();
      oop.close();
      fos.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  public static List<Document> loadDocumentList(String filename) {
    FileInputStream in;
    ObjectInputStream ois;

    List<Document> o = null;
    String path = new File("").getAbsolutePath() + "\\" + filename;

    try {
      in = new FileInputStream(path);
      ois = new ObjectInputStream(in);
      o = (List<Document>) ois.readObject();
      ois.close();
      in.close();
    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace();
    }

    return o;
  }

  public static void saveOkapiScore(Map<String, Double> o, String filename) {
    FileOutputStream fos = null;
    ObjectOutputStream oop = null;

    try {
      fos = new FileOutputStream(filename);
      oop = new ObjectOutputStream(fos);
      oop.writeObject(o);
      oop.flush();
      fos.flush();
      oop.close();
      fos.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static Map<String, Double> loadOkapiScore(String filename) {
    FileInputStream in;
    ObjectInputStream ois;

    Map<String, Double> o = null;
    String path = new File("").getAbsolutePath() + "\\" + filename;

    try {
      in = new FileInputStream(path);
      ois = new ObjectInputStream(in);
      o = (Map<String, Double>) ois.readObject();
      ois.close();
      in.close();
    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace();
    }

    return o;
  }

  public static boolean fileExists(String fileName) {
    File f = new File(fileName);
    return f.exists() && !f.isDirectory();
  }

  public static void saveMapToFileStringToId(String file, Map<String, Integer> map) {
    FileOutputStream fos = null;
    ObjectOutputStream oop = null;

    try {
      fos = new FileOutputStream(file);
      oop = new ObjectOutputStream(fos);
      oop.writeObject(map);
      oop.flush();
      fos.flush();
      oop.close();
      fos.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void saveMapToFileIdToString(String file, Map<Integer, String> map) {
    FileOutputStream fos = null;
    ObjectOutputStream oop = null;

    try {
      fos = new FileOutputStream(file);
      oop = new ObjectOutputStream(fos);
      oop.writeObject(map);
      oop.flush();
      fos.flush();
      oop.close();
      fos.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static Map<String, Integer> getMapFromFileStringToId(String docIdFile) {
    FileInputStream in;
    ObjectInputStream ois;

    Map<String, Integer> o = null;
    try {
      in = new FileInputStream(docIdFile);
      ois = new ObjectInputStream(in);
      o = (Map<String, Integer>) ois.readObject();
      ois.close();
      in.close();
    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace();
      return new HashMap<>();
    }

    return o;
  }

  public static Map<Integer, String> getMapFromFileIdToString(String docIdFile) {
    FileInputStream in;
    ObjectInputStream ois;

    Map<Integer, String> o = null;
    try {
      in = new FileInputStream(docIdFile);
      ois = new ObjectInputStream(in);
      o = (Map<Integer, String>) ois.readObject();
      ois.close();
      in.close();
    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace();
      return new HashMap<>();
    }

    return o;
  }

  public static TreeMap<Integer, InvertedIndexTerm> readInvertedIndexFromFile(int batchId,
                                                                           String directory,
                                                                           Map<Integer, String> globalIdTermMap, Integer... partialReadInfo) throws IOException {
    String catalogFilePath = directory + File.separator + "catalog" + batchId;
    String indexFilePath = directory + File.separator + "index" + batchId;

    int threshold = 0;
    int idOffset = 0;
    if (partialReadInfo != null) {
      threshold = partialReadInfo[0];
      idOffset = partialReadInfo[1];
    }

    Path fileName
            = Path.of(catalogFilePath);
    String str = Files.readString(fileName);
    // splitting at previous threshold so that only new terms get scanned
    if (idOffset != 0) {
      str = str.split(" " + (idOffset - 1) + "  ")[1];
    }

    Scanner sc = new Scanner(str);

    if (idOffset != 0) {
      discardRemainingDataFromScanner(sc);
    }

    TreeMap<Integer, InvertedIndexTerm> resultList = new TreeMap<>();
    while (sc.hasNext()) {
      InvertedIndexTerm term = extractTermFromStream(sc, indexFilePath, globalIdTermMap);
      int termId = term.getTermId();
      resultList.put(termId, term);
      if (termId == threshold - 1) {
        return resultList;
      }
    }

    return resultList;
  }

  private static void discardRemainingDataFromScanner(Scanner sc) {
    // discarded offset
    sc.nextLong();
    // discarded size
    sc.nextInt();
  }

  public static void initDirectory(String multipartLocation) throws IOException {
    Path storageDirectory = Paths.get(multipartLocation);
    if (!Files.exists(storageDirectory)) {
      Files.createDirectory(storageDirectory);
    }
  }

  public static boolean checkDirectoryExists(String filePath) {
    Path path = Paths.get(filePath);
    return Files.exists(path);
  }

  public static void saveInvertedIndexToFile(Collection<InvertedIndexTerm> partialInvertedIndex, int batchId, String directory, boolean compress, boolean isStemmed) throws IOException {
    initDirectory(directory);
    String catalogFilePath = directory + File.separator + "catalog" + batchId;
    String indexFilePath = directory + File.separator + "index" + batchId;
    BufferedWriter catalogWriter = new BufferedWriter(new FileWriter(catalogFilePath, true));
    BufferedWriter indexFileWriter = new BufferedWriter(new FileWriter(indexFilePath, true));
    for (InvertedIndexTerm currTerm : partialInvertedIndex) {
      StringBuilder catalogSb = new StringBuilder();
      StringBuilder indexSb = new StringBuilder();
      // adding an extra space after the term id which helps differentiate it from other values
      catalogSb.append(currTerm.getTermId()).append("  ");
      long initialOffset = new File(indexFilePath).length();
      for (Map.Entry<Integer, List<Integer>> eachDoc : currTerm.getDocIdPositionMap().entrySet()) {
        for (int pos : eachDoc.getValue()) {
          indexSb.append(eachDoc.getKey()).append(" ");
          indexSb.append(pos).append(" ");
        }
      }

      indexFileWriter.write(indexSb.toString());
      indexFileWriter.flush();
      long finalOffset = new File(indexFilePath).length();
      long size = finalOffset - initialOffset;
      catalogSb.append(initialOffset).append(" ").append(size).append(" ");
      catalogWriter.write(catalogSb.toString());
      catalogWriter.flush();
    }
    catalogWriter.close();
    indexFileWriter.close();

    if (compress) {
      String basePath = isStemmed ? Configuration.stemmedFinalSplitIndexFileCompressed : Configuration.finalSplitIndexFileCompressed;
      compressFile(catalogFilePath, basePath + File.separator + "catalog" + batchId);
      compressFile(indexFilePath, basePath + File.separator + "index" + batchId);
    }
  }

  private static long getFileOffset(String filePath) throws IOException {
    RandomAccessFile raFile = new RandomAccessFile(filePath, "rw");
    return raFile.length();
  }

  // borrowed from https://www.digitalocean.com/community/tutorials/java-randomaccessfile-example
  private static void writeData(String filePath, String data, int seek) throws IOException {
    RandomAccessFile file = new RandomAccessFile(filePath, "rw");
    file.seek(seek);
    file.write(data.getBytes());
    file.close();
  }

  public static void copyFile(String originalPath, String copyPath)
          throws IOException {

    Path copied = Paths.get(copyPath);
    Path original = Paths.get(originalPath);
    Files.copy(original, copied, StandardCopyOption.REPLACE_EXISTING);
  }

  // borrowed from https://www.digitalocean.com/community/tutorials/java-randomaccessfile-example
  private static byte[] readCharsFromFile(String filePath, long seek, int chars) throws IOException {
    RandomAccessFile file = new RandomAccessFile(filePath, "r");
    file.seek(seek);
    byte[] bytes = new byte[chars];
    file.read(bytes);
    file.close();
    return bytes;
  }

  // borrowed from https://www.digitalocean.com/community/tutorials/java-gzip-example-compress-decompress-file
  public static void decompressFile(String gzipFile, String newFile) {
    try {
      FileInputStream fis = new FileInputStream(gzipFile);
      GZIPInputStream gis = new GZIPInputStream(fis);
      FileOutputStream fos = new FileOutputStream(newFile);
      byte[] buffer = new byte[1024];
      int len;
      while ((len = gis.read(buffer)) != -1) {
        fos.write(buffer, 0, len);
      }
      //close resources
      fos.close();
      gis.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void deleteFile(String filePath) {
    File file = new File(filePath);
    file.delete();
  }

  // borrowed from https://www.digitalocean.com/community/tutorials/java-gzip-example-compress-decompress-file
  private static void compressFile(String file, String gzipFile) {
    try {
      FileInputStream fis = new FileInputStream(file);
      FileOutputStream fos = new FileOutputStream(gzipFile);
      GZIPOutputStream gzipOS = new GZIPOutputStream(fos);
      byte[] buffer = new byte[1024];
      int len;
      while ((len = fis.read(buffer)) != -1) {
        gzipOS.write(buffer, 0, len);
      }
      //close resources
      gzipOS.close();
      fos.close();
      fis.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  public static void saveIntToFile(int totalCollectionFrequency, String filePath) {
    FileOutputStream fos = null;
    Writer wr = null;
    try {
      wr = new FileWriter(filePath);
      wr.write(String.valueOf(totalCollectionFrequency));
      wr.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static int getIntFromFile(String filePath) {
    File text = new File(filePath);

    try {
      Scanner sc = new Scanner(text);
      return Integer.parseInt(sc.next());
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    return 0;
  }

  public static Map<String, List<InvertedIndexTerm>> getInvertedIndexMapForQuery(List<Query> queries, Indexer indexer) throws IOException {
    Map<String, InvertedIndexTerm> invertedIndexMap = new HashMap<>();
    Map<String, List<InvertedIndexTerm>> result = new HashMap<>();
    for (Query q : queries) {
      String[] queryWords = q.getText().split(" ");
      List<InvertedIndexTerm> invertedIndexList = new ArrayList<>();

      // change this to look for one word at a time
      for (String word : queryWords) {
        if (!invertedIndexMap.containsKey(word)) {
          InvertedIndexTerm term = indexer.search(word);
          if(term == null) continue;
          invertedIndexMap.put(word, term);
        }
        invertedIndexList.add(invertedIndexMap.get(word));
      }
      result.put(q.getId(), invertedIndexList);
    }
    return result;
  }

  public static InvertedIndexTerm readParticularTerm(String indexFilePath, String catalogFilePath,
                                                     TreeMap<Integer, String> idTermMap, int searchTerm) throws IOException {
    Path fileName
            = Path.of(catalogFilePath);
    String str = Files.readString(fileName);
    str = str.split(" " + (searchTerm - 1) + "  ")[1];
    Scanner sc = new Scanner(str);
    discardRemainingDataFromScanner(sc);
    return extractTermFromStream(sc, indexFilePath, idTermMap);
  }


  public static InvertedIndexTerm extractTermFromStream(Scanner sc, String indexFilePath,
                                                        Map<Integer, String> idTermMap) throws IOException {
    int termId = sc.nextInt();
    long offset = sc.nextLong();
    int size = sc.nextInt();
    TreeMap<Integer, List<Integer>> docIdPositionMap = new TreeMap<>();
    String indexData = new String(readCharsFromFile(indexFilePath, offset, size));
    Scanner indexScanner = new Scanner(indexData);
    while (indexScanner.hasNext()) {
      int docId = indexScanner.nextInt();
      int position = indexScanner.nextInt();
      if (!docIdPositionMap.containsKey(docId)) {
        docIdPositionMap.put(docId, new ArrayList<>());
      }
      List<Integer> currList = docIdPositionMap.get(docId);
      currList.add(position);
    }
    return new InvertedIndexTerm(idTermMap.get(termId), termId, docIdPositionMap);
  }

//
//  public static List<RetrievalResponse> getDocumentsInIndex(InvertedIndexTerm invertedIndexList, Map<Long, String> globalIdDocMap, RetrievalModel model) {
//    Document result = null;
//    TreeMap<Long, List<Integer>> docIdPositionMap = term.getDocIdPositionMap();
//    for(var doc: docIdPositionMap.entrySet()) {
//      long docShortId = doc.getKey();
//      String docLongId = globalIdDocMap.get(docShortId);
//      if(!result.containsKey(docLongId)) {
//        result.put(docLongId, new Document(docLongId, docShortId));
//      }
//      Document currDoc = result.get(docLongId);
//      currDoc.addTerm(term.getTerm(), doc.getValue());
//    }
//
//    for(var doc: result.entrySet()) {
//      var docObject = doc.getValue();
//      docObject.setLength(docObject.getTerms().values().size());
//    }
//
//    return result;
//  }
}
