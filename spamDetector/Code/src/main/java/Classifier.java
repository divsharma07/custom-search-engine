import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

public interface Classifier {
  /**
   * Orchestrate the process of spam detection.
   */
  void orchestrateSpamDetection(Map<String, ParsedDocument> docs) throws Exception;
}
