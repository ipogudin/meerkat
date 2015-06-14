package meerkat.java.httpservice.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import meerkat.java.utils.APersistentMapUtils;
import clojure.lang.AFn;
import clojure.lang.APersistentMap;
import clojure.lang.IPersistentMap;
import clojure.lang.IPersistentVector;
import clojure.lang.Keyword;
import clojure.lang.PersistentArrayMap;
import clojure.lang.PersistentHashMap;
import clojure.lang.PersistentVector;

public class ContextFactoryImpl implements ContextFactory {

  public static final String CONTENT_TYPE = "Content-Type";
  public static final String WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
  public static final String MULTIPART = "multipart/form-data";

  private static final Keyword METHOD = Keyword.intern("method");
  private static final Keyword HEADERS = Keyword.intern("headers");
  private static final Keyword PARAMETERS = Keyword.intern("parameters");
  private static final Keyword BODY = Keyword.intern("body");

  private static final Keyword REQUEST = Keyword.intern("request");
  private static final Keyword RESPONSE = Keyword.intern("response");

  private static final Keyword WRITE = Keyword.intern("write");
  private static final Keyword FLUSH = Keyword.intern("flush");
  private static final Keyword WRITE_AND_FLUSH = Keyword.intern("write-and-flush");
  private static final Keyword COMPLETE = Keyword.intern("complete");
  private static final Keyword CLOSE = Keyword.intern("close");

  @Override
  public IPersistentMap build(FullHttpRequest httpRequest,
      ChannelHandlerContext channelHandlerContext) {
    Map<Keyword, Object> context = new HashMap<>();
    
    context.put(REQUEST, buildRequest(httpRequest));
    
    final NettyResponseProcessor nettyResponseProcessor = new NettyResponseProcessorImpl(channelHandlerContext);
    
    context.put(
        WRITE,
        new AFn() {
          @Override
          public Object invoke(Object arg1) {
            nettyResponseProcessor.write(
                (APersistentMap) APersistentMapUtils.getValue((APersistentMap) arg1, RESPONSE));
            return null;
          }
        });
    context.put(
        FLUSH,
        new AFn() {
          @Override
          public Object invoke() {
            nettyResponseProcessor.flush();
            return null;
          }
        });
    context.put(
        WRITE_AND_FLUSH,
        new AFn() {
          @Override
          public Object invoke(Object arg1) {
            nettyResponseProcessor.write(
                (APersistentMap) APersistentMapUtils.getValue((APersistentMap) arg1, RESPONSE));
            nettyResponseProcessor.flush();
            return null;
          }
        });
    context.put(
        COMPLETE,
        new AFn() {
          @Override
          public Object invoke() {
            nettyResponseProcessor.complete();
            return null;
          }
        });
    context.put(
        CLOSE,
        new AFn() {
          @Override
          public Object invoke() {
            nettyResponseProcessor.close();
            return null;
          }
        });
    
    return PersistentHashMap.create(context);
  }

  public IPersistentMap buildRequest(FullHttpRequest httpRequest) {
    Map<Keyword, Object> transformedRequest = new HashMap<>();
    transformedRequest.put(METHOD, Keyword.find(httpRequest.method().name()));
    
    //transformation headers
    final Map<Keyword, String> headers = new HashMap<>();
    httpRequest.headers().forEach(e -> headers.put(Keyword.intern(e.getKey().toLowerCase()), e.getValue()));
    transformedRequest.put(HEADERS, PersistentArrayMap.create(headers));

    //parsing query string parameters
    Map<Keyword, IPersistentVector> parameters = new HashMap<>();
    new QueryStringDecoder(httpRequest.uri()).parameters().entrySet()
        .forEach((e) -> {
            Keyword name = Keyword.intern(e.getKey());
                parameters.put(name, PersistentVector.create(e.getValue()));
            });
    
    if (HttpMethod.POST.equals(httpRequest.method())) {
        String contentType = "";
        if (httpRequest.headers().contains(CONTENT_TYPE)) {
            contentType = httpRequest.headers().get(CONTENT_TYPE);
        }
    
        if (contentType.startsWith(WWW_FORM_URLENCODED)
                || contentType.startsWith(MULTIPART)) {
            HttpPostRequestDecoder postDecoder = new HttpPostRequestDecoder(
                    new DefaultHttpDataFactory(false), httpRequest);
            try {
                postDecoder.getBodyHttpDatas().stream().forEach((data) -> {
                        if (data.getHttpDataType() == HttpDataType.Attribute) {
                            Attribute attribute = (Attribute) data;
                            try {
                                parameters.put(
                                    Keyword.intern(data.getName()), 
                                    PersistentVector.create(
                                        new String(
                                            attribute.getByteBuf().array(), 
                                            attribute.getCharset())));
                            } catch (IOException e) {
                                
                            }
                        }
                    });
            } finally {
                postDecoder.destroy();
            }
        } else {
            transformedRequest.put(BODY, httpRequest.content().copy().array());
        }
    }
    transformedRequest.put(PARAMETERS, PersistentHashMap.create(parameters));
    return PersistentHashMap.create(transformedRequest);
  }
}
