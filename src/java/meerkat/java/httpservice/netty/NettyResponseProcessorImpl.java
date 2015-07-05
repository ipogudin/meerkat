package meerkat.java.httpservice.netty;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import meerkat.java.utils.APersistentMapUtils;
import clojure.lang.APersistentMap;
import clojure.lang.Keyword;

public class NettyResponseProcessorImpl implements NettyResponseProcessor {

  private static final Keyword BODY = Keyword.intern("body");
  private static final Keyword HEADERS = Keyword.intern("headers");
  private static final Keyword STATUS = Keyword.intern("status");

  private final ChannelHandlerContext channelHandlerContext;
  private final ChannelPromise promise;
  private volatile boolean headersWritten;

  public NettyResponseProcessorImpl(ChannelHandlerContext channelHandlerContext) {
    this.channelHandlerContext = channelHandlerContext;
    promise = this.channelHandlerContext.newPromise();
    promise.addListener(new GenericFutureListener<Future<? super Void>>() {
      @Override
      public void operationComplete(Future<? super Void> future)
          throws Exception {
        System.err.println(future.isSuccess());
      }
    });
  }

  @SuppressWarnings("unchecked")
  protected void writeHeaders(APersistentMap response) {
    if (!headersWritten) {
      synchronized (this) {
        if (!headersWritten) {
          final int status = APersistentMapUtils.getIntValue(response, STATUS);
          HttpResponse httpResponse = 
              new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.valueOf(status));
          APersistentMap headers = (APersistentMap)APersistentMapUtils.getValue(response, HEADERS);
          headers.forEach(
              (header, value) -> httpResponse.headers().set(
                  upperCaseForFirstCharacters(((Keyword)header).getName()), value));
          channelHandlerContext.write(httpResponse);
        }
      }
    }
  }

  @Override
  public void write(APersistentMap response) {
    writeHeaders(response);

    Object body = APersistentMapUtils.getValue(response, BODY);
    ByteBuf bodyBuffer = null;
    if (body instanceof byte[]) {
      bodyBuffer = Unpooled.wrappedBuffer((byte[])body);
    }
    else if (body instanceof String) {
      try {
        bodyBuffer = Unpooled.wrappedBuffer(((String)body).getBytes("UTF-8"));
      } catch (UnsupportedEncodingException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    channelHandlerContext.write(bodyBuffer);
  }

  @Override
  public void flush() {
    channelHandlerContext.flush();
  }

  @Override
  public synchronized void complete() {
    headersWritten = false;
    channelHandlerContext.write(LastHttpContent.EMPTY_LAST_CONTENT);
  }

  @Override
  public void close() {
    channelHandlerContext.close();
  }

  private final static Map<String, String> UPPER_CASE_STRINGS_CACHE = new ConcurrentHashMap<>();
  private final static int MAX_UPPER_CASE_STRINGS = 2048;
  /**
   * This method assumes that the set of unique strings are pretty small.
   * @param s
   * @return
   */
  public static String upperCaseForFirstCharacters(String s) {
    String resultString = UPPER_CASE_STRINGS_CACHE.get(s);
    if (resultString == null) {
      synchronized (UPPER_CASE_STRINGS_CACHE) {
        StringBuilder sb = new StringBuilder();
        boolean upper = true;
        for (int i = 0; i < s.length(); i++) {
          char c = s.charAt(i);
          
          if (upper) {
            c = Character.toUpperCase(c);
            upper = false;
          }
          if (c == '-') {
            upper = true;
          }
          sb.append(c);
        }
        resultString = sb.toString();
        if (UPPER_CASE_STRINGS_CACHE.size() > MAX_UPPER_CASE_STRINGS) {
          //cache overflow, full eviction.
          UPPER_CASE_STRINGS_CACHE.clear();
        }
        UPPER_CASE_STRINGS_CACHE.put(s, resultString);
      }
    }
    return resultString;
  }
}
