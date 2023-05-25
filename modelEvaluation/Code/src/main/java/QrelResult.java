import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains all the data parsed from the qrel ranking file.
 */
public class QrelResult {
  private List<QrelRow> qrelRowList;
  private Map<Integer, Map<String, Integer>> qrelMap;

  private Map<Integer, Integer> relevantMap;
  private Map<Integer, Integer> veryRelaventMap;
  private Map<Integer, Integer> allRelMap;

  private Map<Integer, Map<String, Integer>> gradedRelevance;

  public QrelResult(List<QrelRow> qrelRowList, Map<Integer, Map<String, Integer>> qrelMap,
                    Map<Integer,Integer> allRelMap, Map<Integer, Map<String, Integer>> gradedRelevance) {
    this.qrelRowList = qrelRowList;
    this.qrelMap = qrelMap;
    this.gradedRelevance = gradedRelevance;
    this.allRelMap = allRelMap;
  }

  public List<QrelRow> getQrelRowList() {
    return qrelRowList;
  }

  public Map<Integer, Map<String, Integer>> getQrelMap() {
    return qrelMap;
  }

  public Map<Integer, Integer> getRelevantMap() {
    return relevantMap;
  }

  public Map<Integer, Integer> getVeryRelaventMap() {
    return veryRelaventMap;
  }

  public Map<Integer, Integer> getAllRelMap() {
    return allRelMap;
  }

  public Map<Integer, Map<String, Integer>> getGradedRelevance() {
    return gradedRelevance;
  }
}
