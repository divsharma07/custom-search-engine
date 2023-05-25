package config;

import java.util.ArrayList;
import java.util.List;

public class Configuration {
  public final static String HOST = "localhost";

  public final static String OUTLINKS_QUEUE = "outlinks_queue";

  public final static String CRAWLED_DOCS_DIR = "CrawledDocs";

  public final static String LINKS_FILES_DIR = "LinksFiles";
  public final static int PORT = 6379;
  public final static int QUEUE_MAP_DB = 1;
  public final static int FRONT_QUEUE_DB = 2;
  public final static int CRAWLED_URLS_DB = 3;
  public final static int ALL_URLS_DB = 4;
  public static final int TOTAL_CRAWL_FILES = 40;
  public static final int BACK_QUEUE_COUNT = 30;

  public static final String INDEX_NAME = "worldwar2_crawled_data_index";
  public static final int FRONT_QUEUE_COUNT = 5;

  public static final String INDEX_SETTINGS = "mappings={\"settings\":{\"number_of_shards\":1,\"number_of_replicas\":1,\"highlight\":{\"max_analyzed_offset\":\"2000000\"},\"analysis\":{\"filter\":{},\"analyzer\":{\"stopped\":{\"type\":\"custom\",\"tokenizer\":\"standard\",}}}},\"mappings\":{\"properties\":{\"docid\":{\"type\":\"text\"},\"title\":{\"type\":\"text\"},\"content\":{\"type\":\"text\",\"fielddata\":True,\"analyzer\":\"stopped\",\"index_options\":\"positions\"},\"inLinks\":{\"type\":\"text\"},\"outLinks\":{\"type\":\"text\"},\"author\":{\"type\":\"text\"}}}}";

  public static final String ELASTIC_PASSWORD = "Q4Tsj19NmmkbBflo75pDXCWC";

  public static final String ELASTIC_HOSTNAME = "worldwar2-cluster.es.us-east4.gcp.elastic-cloud.com";

  public static final List<String> RELEVANT_KEYWORDS = new ArrayList<>(List.of("attack", "bomb", "battle", "world", "war", "siege",
           "allies", "united", "states", "pacific", "1939", "1941", "1941", "1943", "1945",
          "campaign", "army", "navy", "won", "usa", "tarawa", "saipan", "II",
          "force", "air", "japan", "graignes", "dragoon", "biak", "angaur", "jima",
          "liberation", "cause", "atlantic", "guettar", "gela", "salerno", "normandy", "graignes", "metz", "aachen", "nuremberg", "guam", "okinawa"));

  public static final List<String> URL_KEYWORD_BLACKLIST = new ArrayList<>(List.of(".jpg", ".svg", ".png", ".pdf", ".gif",
          "youtube", "edit", "footer", "sidebar", "cite",
          "special", "mailto", "books.google", "tel:",
          "javascript", "facebook", "instagram", ".ogv", "amazon", "quiz",
          ".webm"));

  public static final double[] RELEVANCE_THRESHOLDS = new double[]{5.0, 10.0, 12.0, 15.0, 20.0};
}
