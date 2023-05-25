package spider;

import java.util.List;

import frontier.FrontierElement;

public class CrawlerResponse {
  private List<FrontierElement> outLinks;
  private WebPageContent webPageContent;

  public CrawlerResponse(List<FrontierElement> outLinks, WebPageContent webPageContent) {
    this.outLinks = outLinks;
    this.webPageContent = webPageContent;
  }

  public WebPageContent getWebPageContent() {
    return webPageContent;
  }

  public List<FrontierElement> getOutLinks() {
    return outLinks;
  }
}
