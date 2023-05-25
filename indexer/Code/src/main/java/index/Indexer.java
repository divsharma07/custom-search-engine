package index;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import model.Document;
import model.InvertedIndexTerm;
import model.Query;
import model.RetrievalResponse;

public interface Indexer {

  /**
   * Helps populate the index with documents and returns a list of those documents.
   * @return the List of documents that has been added to the index.
   */
  void populateIndexAndMetadata();

  /**
   * Identifies if the index exists.
   * @return
   */
  boolean indexExists();


  /**
   * Return the inverted index for a given word
   * @return
   */
  InvertedIndexTerm search(String words) throws IOException;

  /**
   * Wraps up all the tasks related to the indexer, like deleting decompressed files.
   */
  void close();

  /**
   * Returns list of indexed documents.
   * @return list of indexed documents.
   */
  List<Document> getIndexedDocs();

  /**
   * Returns all the terms contained in the index vocabulary
   * @return the vocabulary.
   */
  Set<String> getVocabulary();

  /**
   * Returns a map that stores the total length of each document;
   * @return the length of each document.
   */
  Map<String, Integer> getDocIdLenMap();

  /**
   * Get average length of indexed document.
   * @return the average length.
   */
  int getAvgDocLength();

  /**
   * Get the map that contains information related to id of each term.
   * @return
   */
  TreeMap<String, Integer> getTermIdMap();

  /**
   * Get the map that contains information related to id of each document.
   * @return
   */
  Map<String, Integer> getDocIdMap();

  /**
   * Get the map that contains information related to term corresponding to each id.
   * @return
   */
  TreeMap<Integer, String> getIdTermMap();

  /**
   * Metadata that points to exact index files where a term could be found.
   * @return
   */
  Map<Integer, String> getIdToFileMap();

  /**
   * Return the total number of tokens in the collection.
   * @return
   */
  int getTotalCollectionTerms();
}
