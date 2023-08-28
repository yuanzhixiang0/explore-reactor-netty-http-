package reactor.netty.http.client;

import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.ClientTransport;
import reactor.util.annotation.Nullable;

import java.time.Duration;
import java.util.Objects;

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

}
