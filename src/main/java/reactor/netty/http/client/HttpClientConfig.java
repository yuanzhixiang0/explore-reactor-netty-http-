package reactor.netty.http.client;

import io.netty.channel.ChannelOption;
import io.netty.handler.logging.LoggingHandler;
import io.netty.resolver.AddressResolverGroup;
import reactor.netty.channel.ChannelMetricsRecorder;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;
import reactor.netty.transport.ClientTransportConfig;

import java.net.SocketAddress;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Encapsulate all necessary configuration for HTTP client transport. The public API is read-only.
 *
 * @author Stephane Maldini
 * @author Violeta Georgieva
 */
public final class HttpClientConfig extends ClientTransportConfig<HttpClientConfig> {

    protected HttpClientConfig(ConnectionProvider connectionProvider, Map<ChannelOption<?>, ?> options, Supplier<? extends SocketAddress> remoteAddress) {
        super(connectionProvider, options, remoteAddress);
    }

    protected HttpClientConfig(ClientTransportConfig<HttpClientConfig> parent) {
        super(parent);
    }

    @Override
    protected AddressResolverGroup<?> defaultAddressResolverGroup() {
        throw new Error();
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
}
