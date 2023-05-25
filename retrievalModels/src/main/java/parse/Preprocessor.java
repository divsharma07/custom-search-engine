package parse;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;


import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Preprocessor {
    private CharArraySet getStopWordSet() {
        List<String> stopWords = new ArrayList<>();
        try {
            String filePath = new File("").getAbsolutePath() + "\\IR_data\\AP_DATA\\stoplist.txt";
            stopWords = Files.readAllLines(Paths.get(filePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new CharArraySet(stopWords, false);
    }

    /**
     *
     * @param inputString
     * @return
     * @throws IOException
     */
    // inspired from this example: https://thekandyancode.wordpress.com/2013/02/04/tokenizing-stopping-and-stemming-using-apache-lucene/
    public String stemmingAndStopWords(String inputString) throws IOException {
        Tokenizer tokenizer = new StandardTokenizer();
        tokenizer.setReader(new StringReader(inputString));
        TokenStream tokenStream = tokenizer;
        tokenStream = new LowerCaseFilter(tokenStream);
        tokenStream = new StopFilter(tokenStream, getStopWordSet());
        tokenStream = new PorterStemFilter(tokenStream);
        CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();
        StringBuilder sb = new StringBuilder();
        while (tokenStream.incrementToken()) {
            sb.append(" ").append(charTermAttribute.toString());
        }
        tokenStream.close();
        return sb.toString();
    }
}
