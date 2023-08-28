package reactor.netty.http.client;

import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.ByteBufMono;
import reactor.netty.Connection;
import reactor.netty.NettyOutbound;

import java.net.URI;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Configures the HTTP request before calling one of the terminal,
 * {@link Publisher} based, {@link ResponseReceiver} API.
 *
 * @author Stephane Maldini
 * @author Violeta Georgieva
 */
final class HttpClientFinalizer extends HttpClientConnect implements HttpClient.RequestSender {

    HttpClientFinalizer(HttpClientConfig config) {
        super(config);
    }

    @Override
    public HttpClient.RequestSender uri(String uri) {
        Objects.requireNonNull(uri, "uri");
        HttpClient dup = duplicate();
        dup.configuration().uriStr = uri;
        dup.configuration().uri = null;
        return (HttpClientFinalizer) dup;
    }

    @Override
    public RequestSender uri(Mono<String> uri) {
        throw new Error();
    }

    @Override
    public RequestSender uri(URI uri) {
        throw new Error();
    }

    @Override
    public ResponseReceiver<?> send(Publisher<? extends ByteBuf> body) {
        throw new Error();
    }

    @Override
    public ResponseReceiver<?> send(BiFunction<? super HttpClientRequest, ? super NettyOutbound, ? extends Publisher<Void>> sender) {
        throw new Error();
    }

    @Override
    public ResponseReceiver<?> sendForm(BiConsumer<? super HttpClientRequest, HttpClientForm> formCallback, Consumer<Flux<Long>> progress) {
        throw new Error();
    }

    @Override
    public Mono<HttpClientResponse> response() {
        throw new Error();
    }

    @Override
    public <V> Flux<V> response(BiFunction<? super HttpClientResponse, ? super ByteBufFlux, ? extends Publisher<V>> receiver) {
        throw new Error();
    }

    @Override
    public <V> Flux<V> responseConnection(BiFunction<? super HttpClientResponse, ? super Connection, ? extends Publisher<V>> receiver) {
        throw new Error();
    }

    @Override
    public ByteBufFlux responseContent() {
        throw new Error();
    }

    @Override
    public <V> Mono<V> responseSingle(BiFunction<? super HttpClientResponse, ? super ByteBufMono, ? extends Mono<V>> receiver) {
        throw new Error();
    }

    @Override
    protected HttpClient duplicate() {
        return new HttpClientFinalizer(new HttpClientConfig(config));
    }
}
