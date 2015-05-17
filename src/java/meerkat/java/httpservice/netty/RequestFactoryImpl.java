package meerkat.java.httpservice.netty;

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

import clojure.lang.IPersistentMap;
import clojure.lang.IPersistentVector;
import clojure.lang.Keyword;
import clojure.lang.PersistentArrayMap;
import clojure.lang.PersistentHashMap;
import clojure.lang.PersistentVector;

public class RequestFactoryImpl implements RequestFactory {

  public static final String CONTENT_TYPE = "Content-Type";
  public static final String WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
  public static final String MULTIPART = "multipart/form-data";

  private static final Keyword METHOD = Keyword.intern("method");
  private static final Keyword HEADERS = Keyword.intern("headers");
  private static final Keyword PARAMETERS = Keyword.intern("parameters");
  private static final Keyword BODY = Keyword.intern("body");

  @Override
  public IPersistentMap build(FullHttpRequest httpRequest) {
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
                                parameters.put(Keyword.intern(data.getName()), PersistentVector.create(attribute.getByteBuf().array()));
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
        transformedRequest.put(PARAMETERS, PersistentHashMap.create(parameters));
    }
    return PersistentHashMap.create(transformedRequest);
  }
}