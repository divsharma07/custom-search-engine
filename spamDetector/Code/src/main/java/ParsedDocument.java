import java.io.Serializable;
import java.util.Objects;

public class ParsedDocument implements Serializable {
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ParsedDocument)) return false;
    ParsedDocument document = (ParsedDocument) o;
    return getContent().equals(document.getContent()) && getId().equals(document.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getContent(), getId());
  }
  private LabelType label;
  private DataSetType dataSetType;
  private String content;
  private String id;
  private final int length;

  public ParsedDocument(String id, String content, LabelType label, DataSetType dataSetType) {
    this.id = id;
    this.content = content;
    this.length = content.trim().split("\\s+").length;
    this.dataSetType = dataSetType;
    this.label = label;
  }


  public void setContent(String content) {
    this.content = content;
  }

  public String getContent() {
    return content;
  }

  public String getId() {
    return id;
  }


  public int getLength() {
    return length;
  }

  public DataSetType getDataSetType() {
    return dataSetType;
  }

  public LabelType getLabel() {
    return label;
  }

  public enum DataSetType {
    TEST,
    TRAIN
  }

  public enum LabelType {
    SPAM,
    HAM
  }
}

