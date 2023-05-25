package retrieve;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import model.Document;
import model.Query;
import model.RetrievalResponse;

public abstract class RetrievalModel {
  private String modelName;

  public RetrievalModel(String modelName) {
    this.modelName = modelName;
  }

  public abstract Map<String, List<RetrievalResponse>> scoreDocuments(List<Document> docs, List<Query> queries);

  void writeRetrievalResult(Map<String, List<RetrievalResponse>> retrievedDocs) {
    StringBuilder sb = new StringBuilder();
    for(Map.Entry<String, List<RetrievalResponse>> entry: retrievedDocs.entrySet()) {
      String queryId = entry.getKey();
      List<RetrievalResponse> responses = entry.getValue();
      for(int i = 0; i < responses.size(); i++) {
        String docId = responses.get(i).getDocId();
        double score = responses.get(i).getScore();
        sb.append(queryId).append(" ").append("Q0").append(" ").append(docId).append(" ").append(i).append(" ").append(score).append(" ").append("Exp").append("\n");
      }
    }
    try {
      FileWriter myWriter = new FileWriter(modelName + ".txt");
      myWriter.write(sb.toString());
      myWriter.close();
      System.out.println("Successfully wrote to the file.");
    } catch (IOException e) {
      System.out.println("An error occurred.");
      e.printStackTrace();
    }

  }

  public String getModelName() {
    return modelName;
  }
}
