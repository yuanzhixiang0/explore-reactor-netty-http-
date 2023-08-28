package reactor.netty.http.client;

import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.resolver.AddressResolverGroup;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.NettyOutbound;
import reactor.netty.channel.ChannelMetricsRecorder;
import reactor.netty.http.Http2SettingsSpec;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.HttpResources;
import reactor.netty.http.logging.HttpMessageLogFactory;
import reactor.netty.http.logging.ReactorNettyHttpMessageLogFactory;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.SslProvider;
import reactor.netty.transport.ClientTransportConfig;

import java.net.SocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.function.*;

/**
 * Encapsulate all necessary configuration for HTTP client transport. The public API is read-only.
 *
 * @author Stephane Maldini
 * @author Violeta Georgieva
 */
public final class HttpClientConfig extends ClientTransportConfig<HttpClientConfig> {

    // Protected/Package private write API

    boolean acceptGzip;
    String baseUrl;
    BiFunction<? super HttpClientRequest, ? super NettyOutbound, ? extends Publisher<Void>> body;
    Function<? super Mono<? extends Connection>, ? extends Mono<? extends Connection>> connector;
    ClientCookieDecoder cookieDecoder;
    ClientCookieEncoder cookieEncoder;
    HttpResponseDecoderSpec decoder;
    Function<Mono<HttpClientConfig>, Mono<HttpClientConfig>> deferredConf;
    BiConsumer<? super HttpClientRequest, ? super Connection> doAfterRequest;
    BiConsumer<? super HttpClientResponse, ? super Connection> doAfterResponseSuccess;
    BiConsumer<? super HttpClientResponse, ? super Connection> doOnRedirect;
    BiConsumer<? super HttpClientRequest, ? super Connection> doOnRequest;
    BiConsumer<? super HttpClientRequest, ? super Throwable> doOnRequestError;
    BiConsumer<? super HttpClientResponse, ? super Connection> doOnResponse;
    BiConsumer<? super HttpClientResponse, ? super Throwable> doOnResponseError;
    BiPredicate<HttpClientRequest, HttpClientResponse> followRedirectPredicate;
    HttpHeaders headers;
    Http2SettingsSpec http2Settings;
    HttpMessageLogFactory httpMessageLogFactory;
    HttpMethod method;
    HttpProtocol[] protocols;
    int _protocols;
    BiConsumer<HttpHeaders, HttpClientRequest> redirectRequestBiConsumer;
    Consumer<HttpClientRequest> redirectRequestConsumer;
    Duration responseTimeout;
    boolean retryDisabled;
    SslProvider sslProvider;
    URI uri;
    String uriStr;
    Function<String, String> uriTagValue;
    WebsocketClientSpec websocketClientSpec;

    HttpClientConfig(HttpConnectionProvider connectionProvider, Map<ChannelOption<?>, ?> options,
                     Supplier<? extends SocketAddress> remoteAddress) {
        super(connectionProvider, options, remoteAddress);
        this.acceptGzip = false;
        this.cookieDecoder = ClientCookieDecoder.STRICT;
        this.cookieEncoder = ClientCookieEncoder.STRICT;
        this.decoder = new HttpResponseDecoderSpec();
        this.headers = new DefaultHttpHeaders();
        this.httpMessageLogFactory = ReactorNettyHttpMessageLogFactory.INSTANCE;
        this.method = HttpMethod.GET;
        this.protocols = new HttpProtocol[]{HttpProtocol.HTTP11};
        this._protocols = h11;
        this.retryDisabled = false;
    }

    HttpClientConfig(HttpClientConfig parent) {
        super(parent);
        this.acceptGzip = parent.acceptGzip;
        this.baseUrl = parent.baseUrl;
        this.body = parent.body;
        this.connector = parent.connector;
        this.cookieDecoder = parent.cookieDecoder;
        this.cookieEncoder = parent.cookieEncoder;
        this.decoder = parent.decoder;
        this.deferredConf = parent.deferredConf;
        this.doAfterRequest = parent.doAfterRequest;
        this.doAfterResponseSuccess = parent.doAfterResponseSuccess;
        this.doOnRedirect = parent.doOnRedirect;
        this.doOnRequest = parent.doOnRequest;
        this.doOnRequestError = parent.doOnRequestError;
        this.doOnResponse = parent.doOnResponse;
        this.doOnResponseError = parent.doOnResponseError;
        this.followRedirectPredicate = parent.followRedirectPredicate;
        this.headers = parent.headers;
        this.http2Settings = parent.http2Settings;
        this.httpMessageLogFactory = parent.httpMessageLogFactory;
        this.method = parent.method;
        this.protocols = parent.protocols;
        this._protocols = parent._protocols;
        this.redirectRequestBiConsumer = parent.redirectRequestBiConsumer;
        this.redirectRequestConsumer = parent.redirectRequestConsumer;
        this.responseTimeout = parent.responseTimeout;
        this.retryDisabled = parent.retryDisabled;
        this.sslProvider = parent.sslProvider;
        this.uri = parent.uri;
        this.uriStr = parent.uriStr;
        this.uriTagValue = parent.uriTagValue;
        this.websocketClientSpec = parent.websocketClientSpec;
    }

    static final int h2 = 0b010;

    static final int h2c = 0b001;

    static final int h11 = 0b100;

    /**
     * Provides a global {@link AddressResolverGroup} from {@link HttpResources}
     * that is shared amongst all HTTP clients. {@link AddressResolverGroup} uses the global
     * {@link LoopResources} from {@link HttpResources}.
     *
     * @return the global {@link AddressResolverGroup}
     */
    @Override
    public AddressResolverGroup<?> defaultAddressResolverGroup() {
        return HttpResources.get().getOrCreateDefaultResolver();
    }
    @Override
    protected LoggingHandler defaultLoggingHandler() {
        throw new Error();
    }

    @Override
    protected LoopResources defaultLoopResources() {
        throw new Error();
    }

    @Override
    protected ChannelMetricsRecorder defaultMetricsRecorder() {
        throw new Error();
    }

    /**
     * Returns true if that {@link HttpClient} secured via SSL transport
     *
     * @return true if that {@link HttpClient} secured via SSL transport
     */
    public boolean isSecure() {
        return sslProvider != null;
    }
}
