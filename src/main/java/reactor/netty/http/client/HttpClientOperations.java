package reactor.netty.http.client;

import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.codec.http.cookie.Cookie;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.*;
import reactor.netty.http.Cookies;
import reactor.netty.http.HttpOperations;
import reactor.netty.http.logging.HttpMessageLogFactory;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.context.ContextView;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

class HttpClientOperations extends HttpOperations<NettyInbound, NettyOutbound>
        implements HttpClientResponse, HttpClientRequest {

    final boolean isSecure;
    final HttpRequest nettyRequest;
    final HttpHeaders requestHeaders;
    final ClientCookieEncoder cookieEncoder;
    final ClientCookieDecoder cookieDecoder;
    final Sinks.One<HttpHeaders> trailerHeaders;

    Supplier<String>[] redirectedFrom = EMPTY_REDIRECTIONS;
    String resourceUrl;
    String path;
    Duration responseTimeout;

    volatile ResponseState responseState;

    boolean started;
    boolean retrying;
    boolean is100Continue;
    RedirectClientException redirecting;

    BiPredicate<HttpClientRequest, HttpClientResponse> followRedirectPredicate;
    Consumer<HttpClientRequest> redirectRequestConsumer;
    HttpHeaders previousRequestHeaders;
    BiConsumer<HttpHeaders, HttpClientRequest> redirectRequestBiConsumer;

    final static String INBOUND_CANCEL_LOG = "Http client inbound receiver cancelled, closing channel.";

    HttpClientOperations(HttpClientOperations replaced) {
        super(replaced);
        this.started = replaced.started;
        this.retrying = replaced.retrying;
        this.redirecting = replaced.redirecting;
        this.redirectedFrom = replaced.redirectedFrom;
        this.redirectRequestConsumer = replaced.redirectRequestConsumer;
        this.previousRequestHeaders = replaced.previousRequestHeaders;
        this.redirectRequestBiConsumer = replaced.redirectRequestBiConsumer;
        this.isSecure = replaced.isSecure;
        this.nettyRequest = replaced.nettyRequest;
        this.responseState = replaced.responseState;
        this.followRedirectPredicate = replaced.followRedirectPredicate;
        this.requestHeaders = replaced.requestHeaders;
        this.cookieEncoder = replaced.cookieEncoder;
        this.cookieDecoder = replaced.cookieDecoder;
        this.resourceUrl = replaced.resourceUrl;
        this.path = replaced.path;
        this.responseTimeout = replaced.responseTimeout;
        this.is100Continue = replaced.is100Continue;
        this.trailerHeaders = replaced.trailerHeaders;
    }

    HttpClientOperations(Connection c, ConnectionObserver listener, ClientCookieEncoder encoder,
                         ClientCookieDecoder decoder, HttpMessageLogFactory httpMessageLogFactory) {
        super(c, listener, httpMessageLogFactory);
        this.isSecure = c.channel()
                .pipeline()
                .get(NettyPipeline.SslHandler) != null;
        this.nettyRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
        this.requestHeaders = nettyRequest.headers();
        this.cookieDecoder = decoder;
        this.cookieEncoder = encoder;
        this.trailerHeaders = Sinks.unsafe().one();
    }


    @Override
    public Map<CharSequence, Set<Cookie>> cookies() {
        throw new Error();
    }

    @Override
    public String fullPath() {
        throw new Error();
    }

    @Override
    public String requestId() {
        throw new Error();
    }

    @Override
    public boolean isKeepAlive() {
        return false;
    }

    @Override
    public boolean isWebsocket() {
        return false;
    }

    @Override
    public HttpMethod method() {
        throw new Error();
    }

    @Override
    public String uri() {
        throw new Error();
    }

    @Override
    public HttpVersion version() {
        throw new Error();
    }

    @Override
    public ContextView currentContextView() {
        throw new Error();
    }

    @Override
    public String[] redirectedFrom() {
        return new String[0];
    }

    @Override
    public HttpHeaders requestHeaders() {
        throw new Error();
    }

    @Override
    public String resourceUrl() {
        throw new Error();
    }

    @Override
    public HttpClientRequest addCookie(Cookie cookie) {
        throw new Error();
    }

    @Override
    public HttpClientRequest addHeader(CharSequence name, CharSequence value) {
        throw new Error();
    }

    @Override
    public HttpClientRequest header(CharSequence name, CharSequence value) {
        throw new Error();
    }

    @Override
    public HttpClientRequest headers(HttpHeaders headers) {
        throw new Error();
    }

    @Override
    public boolean isFollowRedirect() {
        return false;
    }

    @Override
    public HttpClientRequest responseTimeout(Duration maxReadOperationInterval) {
        throw new Error();
    }

    @Override
    public HttpHeaders responseHeaders() {
        ResponseState responseState = this.responseState;
        if (responseState != null) {
            return responseState.headers;
        }
        throw new IllegalStateException("Response headers cannot be accessed without " + "server response");
    }


    @Override
    public HttpResponseStatus status() {
        throw new Error();
    }

    @Override
    public Mono<HttpHeaders> trailerHeaders() {
        throw new Error();
    }

    static final class ResponseState {

        final HttpResponse response;
        final HttpHeaders  headers;
        final Cookies      cookieHolder;

        ResponseState(HttpResponse response, HttpHeaders headers, ClientCookieDecoder decoder) {
            this.response = response;
            this.headers = headers;
            this.cookieHolder = Cookies.newClientResponseHolder(headers, decoder);
        }
    }

    static final int                    MAX_REDIRECTS      = 50;
    @SuppressWarnings({"unchecked", "rawtypes"})
    static final Supplier<String>[]     EMPTY_REDIRECTIONS = (Supplier<String>[]) new Supplier[0];
    static final Logger log                = Loggers.getLogger(HttpClientOperations.class);
}
