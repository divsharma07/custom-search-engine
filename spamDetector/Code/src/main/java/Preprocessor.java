import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

public class Preprocessor {

  public String runPreprocessor(String input, Dictionary dictionary) {
    if(input == null) return "";
    // Tokenize the input string and remove punctuation
    StringTokenizer tokenizer = new StringTokenizer(input);
    StringBuilder sb = new StringBuilder();
    Pattern pattern = Pattern.compile("[^a-zA-Z]");
    while (tokenizer.hasMoreTokens()) {
      String token = tokenizer.nextToken();
      try {
        URL url = new URL(token);
        continue;
      } catch (MalformedURLException e) {
      }

      String cleanToken = pattern.matcher(token).replaceAll("");
      sb.append(cleanToken).append(" ");
    }

    // Only keep English unigrams
    String[] tokens = sb.toString().split("\\s+");
    StringBuilder englishUnigrams = new StringBuilder();
    for (String token : tokens) {
      boolean isEnglish = dictionary.contains(token);
      if (token.matches("[a-zA-Z]+") && isEnglish) {
        englishUnigrams.append(token.toLowerCase()).append(" ");
      }
    }

    return englishUnigrams.toString();
  }
}
