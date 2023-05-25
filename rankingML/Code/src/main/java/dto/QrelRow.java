package dto;

/**
 * Contains data from each row of qrel ranked file.
 */
public class QrelRow {
  private String documentId;
  private int queryId;
  private String accessorId;

  private int grade;

  public QrelRow(int queryId, String accessorId, String documentId, int grade) {
    this.documentId = documentId;
    this.queryId = queryId;
    this.accessorId = accessorId;
    this.grade = grade;
  }
}
