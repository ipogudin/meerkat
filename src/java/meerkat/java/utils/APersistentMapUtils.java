package meerkat.java.utils;

import clojure.lang.APersistentMap;
import clojure.lang.Keyword;

public final class APersistentMapUtils {

  public static int getIntValue(APersistentMap map, Keyword key) {
    return ((Long) map.get(key)).intValue();
  }

  public static long getLongValue(APersistentMap map, Keyword key) {
    return (Long) map.get(key);
  }

  public static Object getValue(APersistentMap map, Keyword key) {
    return map.get(key);
  }
}
