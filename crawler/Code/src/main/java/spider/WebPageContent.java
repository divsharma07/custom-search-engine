package spider;

import java.util.Map;

public class WebPageContent {
  private Map<String, String> outLinkAnchorTextMap;
  private String url;
  private String textContent;
  private String title;


  public WebPageContent(Map<String, String> outLinkAnchorTextMap, String textContent, String title, String url) {
    this.outLinkAnchorTextMap = outLinkAnchorTextMap;
    this.textContent = textContent;
    this.title = title;
    this.url = url;
  }

  public Map<String,String> getOutLinksAnchorTextMap() {
    return outLinkAnchorTextMap;
  }

  public String getTextContent() {
    return textContent;
  }

  public String getTitle() {
    return title;
  }

  public String getUrl() {
    return url;
  }
}
