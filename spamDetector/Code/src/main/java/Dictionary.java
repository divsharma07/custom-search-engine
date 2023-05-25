import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Dictionary class that checks if a word is a valid english word.
 */
public class Dictionary
{
  private Set<String> wordsSet;

  public Dictionary() throws IOException
  {
    Path path = Paths.get("words.txt");
    byte[] readBytes = Files.readAllBytes(path);
    String wordListContents = new String(readBytes, StandardCharsets.UTF_8);
    String[] words = wordListContents.split("\n");
    for(int i = 0; i < words.length; i++) {
      words[i]= words[i].replaceAll("\r", "");
    }
    wordsSet = new HashSet<>();
    Collections.addAll(wordsSet, words);
  }

  public boolean contains(String word)
  {
    return wordsSet.contains(word);
  }
}