package meerkat.java.httpservice.netty;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.UnsupportedEncodingException;

import meerkat.java.utils.APersistentMapUtils;
import clojure.lang.APersistentMap;
import clojure.lang.Keyword;

public class Response {

  private static final Keyword BODY = Keyword.intern("body");
  private static final Keyword HEADERS = Keyword.intern("headers");
  private static final Keyword STATUS = Keyword.intern("status");

  private final ChannelHandlerContext channelHandlerContext;
  private final ChannelPromise promise;
  private volatile Object context;
  private volatile boolean headersWritten;

  public Response(ChannelHandlerContext channelHandlerContext) {
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

  public Object getContext() {
    return context;
  }

  public void setContext(Object context) {
    this.context = context;
  }

  @SuppressWarnings("unchecked")
  public void writeHeaders(APersistentMap response) {
    if (!headersWritten) {
      synchronized (this) {
        if (!headersWritten) {
          final int status = APersistentMapUtils.getIntValue(response, STATUS);
          HttpResponse httpResponse = 
              new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.valueOf(status));
          APersistentMap headers = (APersistentMap)APersistentMapUtils.getValue(response, HEADERS);
          headers.forEach(
              (header, value) -> httpResponse.headers().set(((Keyword)header).getName(), value));
          channelHandlerContext.write(httpResponse);
        }
      }
    }
  }

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

  public void flush() {
    channelHandlerContext.flush();
  }

  public synchronized void complete() {
    headersWritten = false;
    channelHandlerContext.write(LastHttpContent.EMPTY_LAST_CONTENT);
  }

  public void close() {
    channelHandlerContext.close();
  }
}
