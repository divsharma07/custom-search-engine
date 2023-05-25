/**
 * Contains data from each row parsed from trec file.
 */
public class TrecRow {
  private String documentId;
  private Integer queryId;
  private double score;

  public TrecRow(Integer queryId, String documentId, double score) {
    this.documentId = documentId;
    this.queryId = queryId;
    this.score = score;
  }
}
