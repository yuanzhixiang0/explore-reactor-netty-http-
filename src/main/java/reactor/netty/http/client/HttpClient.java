package reactor.netty.http.client;

import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.ClientTransport;

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

}
