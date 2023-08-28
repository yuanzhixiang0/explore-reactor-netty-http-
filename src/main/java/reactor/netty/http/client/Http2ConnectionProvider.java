package reactor.netty.http.client;

import io.netty.resolver.AddressResolverGroup;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.MonoSink;
import reactor.netty.Connection;
import reactor.netty.ConnectionObserver;
import reactor.netty.internal.shaded.reactor.pool.InstrumentedPool;
import reactor.netty.internal.shaded.reactor.pool.PooledRef;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.PooledConnectionProvider;
import reactor.netty.transport.TransportConfig;

import java.net.SocketAddress;

final class Http2ConnectionProvider extends PooledConnectionProvider<Connection> {
    final ConnectionProvider parent;

    Http2ConnectionProvider(ConnectionProvider parent) {
        super(initConfiguration(parent));
        this.parent = parent;
        if (parent instanceof PooledConnectionProvider) {
            ((PooledConnectionProvider<?>) parent).onDispose(disposeLater());
        }
    }

    static Builder initConfiguration(ConnectionProvider parent) {
        String name = parent.name() == null ? CONNECTION_PROVIDER_NAME : CONNECTION_PROVIDER_NAME + NAME_SEPARATOR + parent.name();
        Builder builder = parent.mutate();
        if (builder != null) {
            return builder.name(name).pendingAcquireMaxCount(-1);
        } else {
            // this is the case when there is no pool
            // only one connection is created and used for all requests
            return ConnectionProvider.builder(name)
                    .maxConnections(parent.maxConnections())
                    .pendingAcquireMaxCount(-1);
        }
    }

    @Override
    protected CoreSubscriber<PooledRef<Connection>> createDisposableAcquire(TransportConfig transportConfig, ConnectionObserver connectionObserver, long l, InstrumentedPool<Connection> instrumentedPool, MonoSink<Connection> monoSink) {
        throw new Error();
    }

    @Override
    protected InstrumentedPool<Connection> createPool(TransportConfig config, PoolFactory<Connection> poolFactory, SocketAddress remoteAddress, AddressResolverGroup<?> resolverGroup) {
        throw new Error();
    }

    static final String CONNECTION_PROVIDER_NAME = "http2";
    static final String NAME_SEPARATOR = ".";
}
