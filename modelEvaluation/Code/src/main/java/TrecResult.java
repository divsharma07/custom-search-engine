import java.util.List;
import java.util.Map;

/**
 * Contains all the data parsed from the TrecFile retrieved from Elastic Search or any other relevance model.
 */
public class TrecResult {
  private Map<Integer, Map<String, Double>> trecMap;
  private List<TrecRow> trecRowList;

  public TrecResult(Map<Integer, Map<String, Double>> trecMap, List<TrecRow> trecRowList) {
    this.trecMap = trecMap;
    this.trecRowList = trecRowList;
  }

  public Map<Integer, Map<String, Double>> getTrecMap() {
    return trecMap;
  }

  public List<TrecRow> getQrelRowList() {
    return trecRowList;
  }
}
