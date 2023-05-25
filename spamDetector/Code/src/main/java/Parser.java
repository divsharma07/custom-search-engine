import org.apache.commons.mail.util.MimeMessageParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;


public class Parser {

  private Preprocessor preprocessor = new Preprocessor();

  private String LABEL_FILE_PATH = "trec07p/trec07p/full/index";
  private Dictionary dictionary;

  private Map<String, String> docFileLabelMap;
  public Parser() {
    try {
      docFileLabelMap = parseLabelFile();
      dictionary = new Dictionary();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Map<String, String> parseLabelFile() throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(LABEL_FILE_PATH));
    String line = "";
    Map<String, String> map = new HashMap<>();
    while ((line = reader.readLine()) != null) {
      String[] split = line.split("\\s+");
      String fileName = split[1].split("/")[2];
      map.put(fileName, split[0]);
    }

    return map;
  }

  public Map<String, ParsedDocument> parseHtml(String directoryPath) throws IOException, MessagingException {;
    File directory = new File(directoryPath);
    File[] files = directory.listFiles();
    Map<String, ParsedDocument> docList = new HashMap<>();
    for (File file : files) {
      ParsedDocument document = null;
      if (file.isFile()) {
        // Parse the MIME message from an input stream
        MimeMessage mimeMessage = new MimeMessage(null, Files.newInputStream(file.toPath()));

        // Use Apache Commons Email to extract the text from the MIME message
        MimeMessageParser parser = new MimeMessageParser(mimeMessage);
        String text = "";
        try {
          parser.parse();
          text = parser.getPlainContent();
        }
        catch (Exception e) {
          // just using empty string if encoding is not supported
        }
        String fileName = file.getName();
        ParsedDocument.DataSetType dataSetType = ParsedDocument.DataSetType.TRAIN;
        ParsedDocument.LabelType labelType;
        if(docFileLabelMap.get(fileName).equals("spam")) {
          labelType = ParsedDocument.LabelType.SPAM;
        }
        else {
          labelType = ParsedDocument.LabelType.HAM;
        }
        if(Math.random() <= 0.2) {
          dataSetType = ParsedDocument.DataSetType.TEST;
        }
        document = new ParsedDocument(fileName,
                preprocessor.runPreprocessor(text, dictionary),
                labelType, dataSetType);
        docList.put(fileName, document);
      }
    }
    return docList;
  }

  // helper method to extract content from a message
  private static String extractContent(Message message) throws Exception {
    Object content = message.getContent();
    if (content instanceof Multipart) {
      // extract content from multipart message
      Multipart multipart = (Multipart) content;
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < multipart.getCount(); i++) {
        BodyPart bodyPart = multipart.getBodyPart(i);
        String bodyPartContent = extractBodyPartContent(bodyPart);
        sb.append(bodyPartContent);
      }
      return sb.toString();
    } else {
      // extract content from simple message
      return content.toString();
    }
  }

  // helper method to extract content from a body part
  private static String extractBodyPartContent(BodyPart bodyPart) throws Exception {
    Object content = bodyPart.getContent();
    if (content instanceof String) {
      // return text content
      return (String) content;
    } else if (content instanceof MimeMultipart) {
      // recursively extract content from multipart body part
      MimeMultipart multipart = (MimeMultipart) content;
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < multipart.getCount(); i++) {
        BodyPart subBodyPart = multipart.getBodyPart(i);
        String subBodyPartContent = extractBodyPartContent(subBodyPart);
        sb.append(subBodyPartContent);
      }
      return sb.toString();
    } else {
      // ignore other types of body parts
      return "";
    }
  }
}
