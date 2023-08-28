package reactor.netty.http.client;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.ByteBufMono;
import reactor.netty.Connection;
import reactor.netty.NettyOutbound;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.ClientTransport;
import reactor.util.annotation.Nullable;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

/**
 * An HttpClient allows building in a safe immutable way an http client that is
 * materialized and connecting when {@link HttpClient#connect()} is ultimately called.
 * {@code Transfer-Encoding: chunked} will be applied for those HTTP methods for which
 * a request body is expected. {@code Content-Length} provided via request headers
 * will disable {@code Transfer-Encoding: chunked}.
 * <p>
 * <p> Examples:
 * <pre>
 * {@code
 * HttpClient.create()
 *           .baseUrl("https://example.com")
 *           .get()
 *           .response()
 *           .block();
 * }
 * {@code
 * HttpClient.create()
 *           .post()
 *           .uri("https://example.com")
 *           .send(Flux.just(bb1, bb2, bb3))
 *           .responseSingle((res, content) -> Mono.just(res.status().code()))
 *           .block();
 * }
 * {@code
 * HttpClient.create()
 *           .baseUri("https://example.com")
 *           .post()
 *           .send(ByteBufFlux.fromString(flux))
 *           .responseSingle((res, content) -> Mono.just(res.status().code()))
 *           .block();
 * }
 * </pre>
 *
 * @author Stephane Maldini
 * @author Violeta Georgieva
 */
public abstract class HttpClient extends ClientTransport<HttpClient, HttpClientConfig> {

    /**
     * A URI configuration
     */
    public interface UriConfiguration<S extends UriConfiguration<?>> {

        /**
         * Configure URI to use for this request/response
         *
         * @param uri target URI, if starting with "/" it will be prepended with
         * {@link #baseUrl(String)} when available
         *
         * @return the appropriate sending or receiving contract
         */
        S uri(String uri);

        /**
         * Configure URI to use for this request/response on subscribe
         *
         * @param uri target URI, if starting with "/" it will be prepended with
         * {@link #baseUrl(String)} when available
         *
         * @return the appropriate sending or receiving contract
         */
        S uri(Mono<String> uri);

        /**
         * Configure URI to use for this request/response.
         * <p>Note: {@link #baseUrl(String)} will have no effect when this method is used for configuring an URI.
         *
         * @param uri target URI which is an absolute, fully constructed {@link URI}
         * @return the appropriate sending or receiving contract
         */
        S uri(URI uri);
    }

    public interface RequestSender extends ResponseReceiver<RequestSender> {

        /**
         * Configure a body to send on request.
         *
         * <p><strong>Note:</strong> The body {@code Publisher} passed in will be invoked also for redirect requests
         * when {@code followRedirect} is enabled. If you need to control what will be sent when
         * {@code followRedirect} is enabled then use {@link #send(BiFunction)}.
         * <p><strong>Note:</strong> For redirect requests, sensitive headers
         * {@link #followRedirect(boolean, Consumer) followRedirect} are removed
         * from the initialized request when redirecting to a different domain, they can be re-added globally via
         * {@link #followRedirect(boolean, Consumer)}/{@link #followRedirect(BiPredicate, Consumer)}
         * or alternatively for full control per redirect request, consider using {@link RedirectSendHandler}
         * with {@link #send(BiFunction)}
         *
         * @param body a body publisher that will terminate the request on complete
         *
         * @return a new {@link ResponseReceiver}
         */
        ResponseReceiver<?> send(Publisher<? extends ByteBuf> body);

        /**
         * Configure a body to send on request using the {@link NettyOutbound} sending
         * builder and returning a {@link Publisher} to signal end of the request.
         *
         * <p><strong>Note:</strong> The sender {@code BiFunction} passed in will be invoked also for redirect requests
         * when {@code followRedirect} is enabled. For redirect requests, sensitive headers
         * {@link #followRedirect(boolean, Consumer) followRedirect} are removed
         * from the initialized request when redirecting to a different domain, they can be re-added globally via
         * {@link #followRedirect(boolean, Consumer)}/{@link #followRedirect(BiPredicate, Consumer)}
         * or alternatively for full control per redirect request, consider using {@link RedirectSendHandler}.
         * {@link RedirectSendHandler} can be used to indicate explicitly that this {@code BiFunction} has special
         * handling for redirect requests.
         *
         * @param sender a bifunction given the outgoing request and the sending
         * {@link NettyOutbound}, returns a publisher that will terminate the request
         * body on complete
         *
         * @return a new {@link ResponseReceiver}
         */
        ResponseReceiver<?> send(BiFunction<? super HttpClientRequest, ? super NettyOutbound, ? extends Publisher<Void>> sender);

        /**
         * Prepare to send an HTTP Form including Multipart encoded Form which support
         * chunked file upload. It will by default be encoded as Multipart but can be
         * adapted via {@link HttpClientForm#multipart(boolean)}.
         *
         * <p><strong>Note:</strong> The HTTP Form passed in will be invoked also for redirect requests
         * when {@code followRedirect} is enabled. If you need to control what will be sent when
         * {@code followRedirect} is enabled use {@link HttpClientRequest#redirectedFrom()} to check the original
         * and any number of subsequent redirect(s), including the one that is in progress.
         * <p><strong>Note:</strong> For redirect requests, sensitive headers
         * {@link #followRedirect(boolean, Consumer) followRedirect} are removed
         * from the initialized request when redirecting to a different domain, they can be re-added globally via
         * {@link #followRedirect(boolean, Consumer)}/{@link #followRedirect(BiPredicate, Consumer)}.
         *
         * @param formCallback called when form generator is created
         *
         * @return a new {@link ResponseReceiver}
         */
        default ResponseReceiver<?> sendForm(BiConsumer<? super HttpClientRequest, HttpClientForm> formCallback) {
            return sendForm(formCallback, null);
        }

        /**
         * Prepare to send an HTTP Form including Multipart encoded Form which support
         * chunked file upload. It will by default be encoded as Multipart but can be
         * adapted via {@link HttpClientForm#multipart(boolean)}.
         *
         * <p><strong>Note:</strong> The HTTP Form passed in will be invoked also for redirect requests
         * when {@code followRedirect} is enabled. If you need to control what will be sent when
         * {@code followRedirect} is enabled use {@link HttpClientRequest#redirectedFrom()} to check the original
         * and any number of subsequent redirect(s), including the one that is in progress.
         * <p><strong>Note:</strong> For redirect requests, sensitive headers
         * {@link #followRedirect(boolean, Consumer) followRedirect} are removed
         * from the initialized request when redirecting to a different domain, they can be re-added globally via
         * {@link #followRedirect(boolean, Consumer)}/{@link #followRedirect(BiPredicate, Consumer)}.
         *
         * @param formCallback called when form generator is created
         * @param progress called after form is being sent and passed with a {@link Flux} of the latest in-flight or uploaded bytes
         *
         * @return a new {@link ResponseReceiver}
         */
        ResponseReceiver<?> sendForm(BiConsumer<? super HttpClientRequest, HttpClientForm> formCallback,
                                     @Nullable Consumer<Flux<Long>> progress);
    }

    public interface ResponseReceiver<S extends ResponseReceiver<?>>
            extends UriConfiguration<S> {

        /**
         * Return the response status and headers as {@link HttpClientResponse}
         * <p> Will automatically close the response if necessary.
         * <p>Note: Will automatically close low-level network connection after returned
         * {@link Mono} terminates or is being cancelled.
         *
         * @return the response status and headers as {@link HttpClientResponse}
         */
        Mono<HttpClientResponse> response();

        /**
         * Extract a response flux from the given {@link HttpClientResponse} and body
         * {@link ByteBufFlux}.
         * <p> Will automatically close the response if necessary after the returned
         * {@link Flux} terminates or is being cancelled.
         *
         * @param receiver extracting receiver
         * @param <V> the extracted flux type
         *
         * @return a {@link Flux} forwarding the returned {@link Publisher} sequence
         */
        <V> Flux<V> response(BiFunction<? super HttpClientResponse, ? super ByteBufFlux, ? extends Publisher<V>> receiver);

        /**
         * Extract a response flux from the given {@link HttpClientResponse} and
         * underlying {@link Connection}.
         * <p> The connection will not automatically {@link Connection#dispose()} and
         * manual interaction with this method might be necessary if the remote never
         * terminates itself.
         *
         * @param receiver extracting receiver
         * @param <V> the extracted flux type
         *
         * @return a {@link Flux} forwarding the returned {@link Publisher} sequence
         */
        <V> Flux<V> responseConnection(BiFunction<? super HttpClientResponse, ? super Connection, ? extends Publisher<V>> receiver);

        /**
         * Return the response body chunks as {@link ByteBufFlux}.
         *
         * <p> Will automatically close the response if necessary after the returned
         * {@link Flux} terminates or is being cancelled.
         *
         * @return the response body chunks as {@link ByteBufFlux}.
         */
        ByteBufFlux responseContent();

        /**
         * Extract a response mono from the given {@link HttpClientResponse} and
         * aggregated body {@link ByteBufMono}.
         *
         * <p> Will automatically close the response if necessary after the returned
         * {@link Mono} terminates or is being cancelled.
         *
         * @param receiver extracting receiver
         * @param <V> the extracted mono type
         *
         * @return a {@link Mono} forwarding the returned {@link Mono} result
         */
        <V> Mono<V> responseSingle(BiFunction<? super HttpClientResponse, ? super ByteBufMono, ? extends Mono<V>> receiver);
    }


    public static HttpClient create(ConnectionProvider connectionProvider) {
        Objects.requireNonNull(connectionProvider, "connectionProvider");
        return new HttpClientConnect(new HttpConnectionProvider(connectionProvider));
    }

    /**
     * Specifies the maximum duration allowed between each network-level read operation while reading a given response
     * (resolution: ms). In other words, {@link io.netty.handler.timeout.ReadTimeoutHandler} is added to the channel
     * pipeline after sending the request and is removed when the response is fully received.
     * If the {@code maxReadOperationInterval} is {@code null}, any previous setting will be removed and no
     * {@code maxReadOperationInterval} will be applied.
     * If the {@code maxReadOperationInterval} is less than {@code 1ms}, then {@code 1ms} will be the
     * {@code maxReadOperationInterval}.
     * The {@code maxReadOperationInterval} setting on {@link HttpClientRequest} level overrides any
     * {@code maxReadOperationInterval} setting on {@link HttpClient} level.
     *
     * @param maxReadOperationInterval the maximum duration allowed between each network-level read operations
     *                                 (resolution: ms).
     * @return a new {@link HttpClient}
     * @since 0.9.11
     * @see io.netty.handler.timeout.ReadTimeoutHandler
     */
    public final HttpClient responseTimeout(@Nullable Duration maxReadOperationInterval) {
        if (Objects.equals(maxReadOperationInterval, configuration().responseTimeout)) {
            return this;
        }
        HttpClient dup = duplicate();
        dup.configuration().responseTimeout = maxReadOperationInterval;
        return dup;
    }

    /**
     * Use the passed HTTP method to connect the {@link HttpClient}.
     *
     * @param method the HTTP method to send
     *
     * @return a {@link RequestSender} ready to finalize request and consume for response
     */
    public RequestSender request(HttpMethod method) {
        Objects.requireNonNull(method, "method");
        HttpClientFinalizer dup = new HttpClientFinalizer(new HttpClientConfig(configuration()));
        dup.configuration().method = method;
        return dup;
    }

}
