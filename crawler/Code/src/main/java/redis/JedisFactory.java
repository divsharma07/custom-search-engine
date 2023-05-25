package redis;

import config.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class JedisFactory {
  private static JedisPool jedisPool;
  private static JedisFactory instance;

  public JedisFactory() {
    JedisPoolConfig poolConfig = new JedisPoolConfig();
    poolConfig.setMaxTotal(20);
    jedisPool = new JedisPool(
            poolConfig,
            Configuration.HOST,
            Configuration.PORT
    );
  }

  public JedisPool getJedisPool() {
    return jedisPool;
  }

  public static JedisFactory getInstance() {
    if (instance == null) {
      instance = new JedisFactory();
    }
    return instance;
  }
}