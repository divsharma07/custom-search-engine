package util;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * URL canonicalizer that makes sure that URLs are uniform and child URLs get properly merged with parent URLs.
 */
public class Canonicalizer {

  public static String canonicalize(String parentUrl, String outLinkUrl) throws URISyntaxException {

    boolean isPath = false;
    // this part is done just to check if the outlink url is a path within the parent url.
    // if a syntax exception is thrown then the url is probably a path.
    try {
      URL outLink = new URL(outLinkUrl);
      outLink.toURI();
    } catch (MalformedURLException e) {
      isPath = true;
    }
    String canonicalUrl = parentUrl;
    if(isPath) {
      URI parentUri = new URI(parentUrl);
      URI childUri = parentUri.resolve(removeFragmentFromChild(outLinkUrl));
      canonicalUrl = childUri.normalize().toString();
    }
    URI uri = URI.create(canonicalUrl);
    String path = uri.getPath();
    if (path != null && path.contains("//")) {
      path = String.join("/", path.split("//"));
    }
    try{
      uri = new URI(uri.getScheme().toLowerCase(), uri.getHost().toLowerCase(), path, null);
    } catch (NullPointerException e) {
      System.out.println("canonical url is " + canonicalUrl);
    }
    return uri.toString();
  }

  public static String removeFragmentFromChild(String childUrl) {
    if(childUrl.contains("#")) {
      try {
        return childUrl.split("#")[0];
      } catch (ArrayIndexOutOfBoundsException e) {
        // this is the situation for paths that are just fragments
      }
    }
    return childUrl;
  }

  public static String getDomain(String url) {
    return URI.create(url).getHost();
  }

  public static String getScheme(String url) {
    return URI.create(url).getScheme();
  }
}
