package spider;

import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import crawlercommons.robots.SimpleRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;
import frontier.FrontierElement;
import util.Canonicalizer;
import util.Helper;

import static config.Configuration.ALL_URLS_DB;
import static config.Configuration.CRAWLED_DOCS_DIR;
import static config.Configuration.CRAWLED_URLS_DB;
import static config.Configuration.TOTAL_CRAWL_FILES;
import static config.Configuration.URL_KEYWORD_BLACKLIST;
import static util.RedisHelper.getFrontierElementFromUrl;
import static util.RedisHelper.setFrontierElementWithUrl;
import static util.RedisHelper.urlAlreadyCrawled;

public class Crawler {
  private final ExecutorService executorService;

  public Crawler(ExecutorService executorService) {
    this.executorService = executorService;
  }

  /**
   * Crawls list of urls, saves them to file
   *
   * @param elements the list of elements that need to be crawled
   * @return Set of FrontierElements that are outlinks of the current crawled elements
   */
  public Set<FrontierElement> crawlAndSave(List<FrontierElement> elements) {

    // multithreading the actual crawling
    List<Callable<CrawlerResponse>> crawlCallable = new ArrayList<>();

    for (FrontierElement element : elements) {
      crawlCallable.add(() -> {
        // Lambda Expression to split crawling of each element
        return crawlAndSave(element);
      });
    }
    List<Future<CrawlerResponse>> batchResponseFuture;
    try {
      batchResponseFuture = executorService.invokeAll(crawlCallable);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    List<CrawlerResponse> result = awaitAndReturnCrawledBatch(batchResponseFuture);
    try {
      writeWebPageContentToFile(result);
      saveInLinks(result);
      saveOutLinks(result);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return getAllOutLinks(result);
  }

  private Set<FrontierElement> getAllOutLinks(List<CrawlerResponse> result) {
    Set<FrontierElement> outLinks = new HashSet<>();
    for (CrawlerResponse each : result) {
      outLinks.addAll(each.getOutLinks());
    }
    return outLinks;
  }

  private List<CrawlerResponse> awaitAndReturnCrawledBatch(
          List<Future<CrawlerResponse>> batchResponseFuture) {
    List<CrawlerResponse> result = new ArrayList<>();

    for (Future<CrawlerResponse> each : batchResponseFuture) {
      try {

        if (each == null) continue;
        CrawlerResponse returnedValue = each.get();
        // this happens if the link is revisited, in which case instead of adding more outLinks, a null is returned
        // this helps prevent a cycle
        if (returnedValue == null) continue;
        result.add(returnedValue);
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
    }
    return result;
  }


  // crawls the page, saves it on file, and returns the outlinks of this page
  private CrawlerResponse crawlAndSave(FrontierElement element) {
    CrawlerResponse crawlerResponse = null;
    if (urlAlreadyCrawled(element.getUrl())) {
      String parent = element.getCurrentParent();
      if (parent == null) {
        return null;
      }
      saveInLinks(element, parent, true);
      return null;
    }
    double relevanceScore = element.computeScore();
    if (isMetadataGood(getHttpHeaders(element.getUrl()), element.getUrl()) &&
            urlPassesFilter(element.getUrl(), element.getWaveNumber())) {
      while (element.getNextPoliteTimestamp() > System.currentTimeMillis()) {
        wait(1000);
      }
      WebPageContent content = getHttpDocumentContent(element.getUrl());

      crawlerResponse = getFilteredOutLinks(content, element.getWaveNumber() + 1, element.getUrl());
      // finally saving this url in the crawled url set and all urls set in redis
      setFrontierElementWithUrl(element.getUrl(), element, CRAWLED_URLS_DB);
      setFrontierElementWithUrl(element.getUrl(), element, ALL_URLS_DB);
    }

    return crawlerResponse;
  }

  private void writeWebPageContentToFile(Collection<CrawlerResponse> contents) throws IOException {
    // picking a random file
    int randomFileNumber = new Random().nextInt(TOTAL_CRAWL_FILES);
    Helper.initDirectory(CRAWLED_DOCS_DIR);
    String filename = CRAWLED_DOCS_DIR + "/crawledFile" + randomFileNumber + ".txt";
    BufferedWriter fileWriter = new BufferedWriter(new FileWriter(filename, true));
    for (CrawlerResponse response : contents) {
      WebPageContent content = response.getWebPageContent();
      String value = String.format("\n <DOC>\n <DOCNO> %s </DOCNO> \n <HEAD> %S </HEAD> \n <TEXT> %s </TEXT> \n </DOC>",
              content.getUrl(), content.getTitle(), content.getTextContent());

      fileWriter.write(value);
      fileWriter.flush();
    }
    fileWriter.close();
  }

  private CrawlerResponse getFilteredOutLinks(WebPageContent content, int waveNumber, String parentUrl) {
    Map<String, String> outLinkAnchorTextMap = content.getOutLinksAnchorTextMap();
    List<FrontierElement> outLinks = new ArrayList<>();
    for (Map.Entry<String, String> each : outLinkAnchorTextMap.entrySet()) {
      outLinks.add(new FrontierElement(waveNumber, each.getKey(), each.getValue(), parentUrl));
    }

    return new CrawlerResponse(outLinks, content);
  }

  private boolean urlPassesFilter(String url, int waveNumber) {
    return isPageGood(url, waveNumber) && urlDoesNotContainBlacklistedWords(url) && canCrawl(url);
  }

  private boolean urlDoesNotContainBlacklistedWords(String url) {
    for (String keyword : URL_KEYWORD_BLACKLIST) {
      if (url.toLowerCase().contains(keyword)) return false;
    }
    return true;
  }

  private void wait(int ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  private void saveOutLinks(List<CrawlerResponse> crawlerResponses) {
    for (CrawlerResponse eachResponse : crawlerResponses) {
      List<FrontierElement> outLinks = eachResponse.getOutLinks();
      FrontierElement parent = getFrontierElementFromUrl(eachResponse.getWebPageContent().getUrl(), ALL_URLS_DB);

      if (parent == null) {
        throw new IllegalStateException("Parent url not yet crawled but trying to save outLinks");
      }
      for (FrontierElement outLink : outLinks) {
        parent.addOutLink(outLink.getUrl());
      }
      // update data in redis
      // always update parent in both crawled and all urls db
      setFrontierElementWithUrl(parent.getUrl(), parent, ALL_URLS_DB);
      setFrontierElementWithUrl(parent.getUrl(), parent, CRAWLED_URLS_DB);
    }

  }

  private void saveInLinks(List<CrawlerResponse> crawlerResponses) {
    for (CrawlerResponse eachResponse : crawlerResponses) {
      List<FrontierElement> outLinks = eachResponse.getOutLinks();
      FrontierElement parent = getFrontierElementFromUrl(eachResponse.getWebPageContent().getUrl(), ALL_URLS_DB);

      if (parent == null) {
        throw new IllegalStateException("Parent url not yet crawled but trying to save inLinks");
      }
      for (FrontierElement outLink : outLinks) {
        outLink.addInLink(parent.getUrl());
        // update data in redis
        // not updating crawled db assuming this outLink is not crawled yet
        setFrontierElementWithUrl(outLink.getUrl(), outLink, ALL_URLS_DB);
      }
    }
  }

  private void saveInLinks(FrontierElement element, String parent, boolean isCrawled) {
    element.addInLink(parent);
    // update data in redis
    setFrontierElementWithUrl(element.getUrl(), element, ALL_URLS_DB);
    if(isCrawled) {
      // only updating crawled data if the current url is crawled
      setFrontierElementWithUrl(element.getUrl(), element, CRAWLED_URLS_DB);
    }
  }

  private static WebPageContent getHttpDocumentContent(String url) {
//    final long startTime = System.nanoTime();


    Map<String, String> outLinksAnchorTextMap = new HashMap<>();
    StringBuilder textContent = new StringBuilder();

    Document doc = null;
    try {
      doc = Jsoup.connect(url).get();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // Select all the hyperlink elements in the HTML content
    Elements links = doc.select("a");
    Elements paragraphs = doc.select("p");
    // Iterate over the hyperlink elements and print their href attribute values
    for (Element link : links) {
      String outLinkUrl = link.attr("href");
      if (outLinkUrl.equals("")) continue;
      try {
        outLinkUrl = Canonicalizer.canonicalize(url, outLinkUrl);
      } catch (URISyntaxException e) {
        System.out.println("Invalid URL " + outLinkUrl);
        continue;
      }
      outLinksAnchorTextMap.put(outLinkUrl, link.text());
    }
    // Iterate over all the paragraph tags to get all the text content in the document
    for (Element p : paragraphs) {
      textContent.append(p.text());
    }

//    final long endTime = System.nanoTime();

//    System.out.println("Parsing is taking about " + TimeUnit.NANOSECONDS.toSeconds(endTime - startTime));
    return new WebPageContent(outLinksAnchorTextMap, textContent.toString(), doc.title(), url);
  }

  private static boolean isPageGood(String url, int waveNumber) {
    FrontierElement element = getFrontierElementFromUrl(url, ALL_URLS_DB);
    if (element == null) {
      element = new FrontierElement(waveNumber, url);
    }

    var score = element.computeScore();
    return element.computeScore() >= 0;
  }

  private static boolean isMetadataGood(Map<String, List<String>> httpHeaders, String url) {
    return isHtml(httpHeaders) && isEnglish(httpHeaders, url);
  }

  private static boolean isEnglish(Map<String, List<String>> httpHeaders, String urlString) {
    List<String> languageHeaders = httpHeaders.get("content-language");
    if (languageHeaders == null) {
      languageHeaders = httpHeaders.get("lang");
    }

    if (languageHeaders != null) {
      for (String s : languageHeaders) {
        if (s.toLowerCase().contains("text/html")) {
          return true;
        }
        if (s.toLowerCase().contains("en")) {
          return true;
        }
      }
    }

    if (languageHeaders == null || languageHeaders.size() == 0) {
      LanguageDetector languageDetector = new OptimaizeLangDetector().loadModels();

      // Detect the language of the text
      LanguageResult result = languageDetector.detect(urlString);

      // Check if the detected language is English
      boolean isEnglish = result.getLanguage().equals("en");

      // Print whether the text is in English or not
      if (isEnglish) {
        return true;
      }
    }


    // if header does not exist try using the url to find if the page might be in english

    return false;
  }

  private static boolean isHtml(Map<String, List<String>> httpHeaders) {
    List<String> contentTypeHeaders = httpHeaders.get("content-type");
    if (contentTypeHeaders == null) {
      contentTypeHeaders = httpHeaders.get("Content-Type");
    }
    for (String s : contentTypeHeaders) {
      if (s.toLowerCase().contains("text/html") || s.toLowerCase().contains("text/plain")) {
        return true;
      }
    }
    return false;
  }

  private static boolean canCrawl(String url) {
    SimpleRobotRulesParser parser = new SimpleRobotRulesParser(2, 1);
    byte[] robotTxtContent = null;
    String robotTxtPath = Canonicalizer.getScheme(url) + "://" + Canonicalizer.getDomain(url) + "/robots.txt";
    try {
      robotTxtContent = getRobotTxtContent(robotTxtPath);
    } catch (IOException | InterruptedException e) {
      System.out.println("This url cannot be crawled " + url);
      return false;
    }
    SimpleRobotRules robotRules = parser.parseContent(robotTxtPath, robotTxtContent, "text/plain", "NEU-CS6200-HW3");
    boolean result = robotRules.isAllowed(url);
    return result;
  }

  private static byte[] getRobotTxtContent(String domain) throws IOException, InterruptedException {
    HttpClient httpClient = HttpClient.newHttpClient();

    // Creates HttpRequest object and set the URI to be requested,
    // when not defined the default request method is the GET request.
    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(domain))
            .GET()
            .build();

    // Sends the request and print out the returned response.
    HttpResponse<String> response = httpClient.send(request,
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

    return response.body().getBytes();
  }

  private static Map<String, List<String>> getHttpHeaders(String url) {

    Map<String, List<String>> headers = null;
    try {
      URL urlObject = new URL(url); // replace with the URL of the webpage you want to get the content-type header of
      HttpURLConnection conn = (HttpURLConnection) urlObject.openConnection();
      conn.setRequestMethod("HEAD");
      conn.connect();
      headers = conn.getHeaderFields();
      conn.disconnect();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return headers;
  }
}
