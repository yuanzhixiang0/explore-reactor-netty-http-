package reactor.netty.http.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.*;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.resolver.AddressResolverGroup;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.MonoSink;
import reactor.core.publisher.Operators;
import reactor.netty.Connection;
import reactor.netty.ConnectionObserver;
import reactor.netty.channel.ChannelMetricsRecorder;
import reactor.netty.channel.ChannelOperations;
import reactor.netty.internal.shaded.reactor.pool.InstrumentedPool;
import reactor.netty.internal.shaded.reactor.pool.PooledRef;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.PooledConnectionProvider;
import reactor.netty.transport.TransportConfig;
import reactor.util.annotation.Nullable;
import reactor.util.context.Context;

import java.io.IOException;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.function.Function;

import static reactor.netty.ReactorNetty.*;
import static reactor.netty.ReactorNetty.setChannelContext;
import static reactor.netty.http.client.HttpClientState.STREAM_CONFIGURED;
import static reactor.netty.http.client.HttpClientState.UPGRADE_REJECTED;

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

    static final class DisposableAcquire
            implements CoreSubscriber<PooledRef<Connection>>, ConnectionObserver, Disposable, GenericFutureListener<Future<Http2StreamChannel>> {
        final Disposable.Composite cancellations;
        final Context currentContext;
        final ConnectionObserver obs;
        final ChannelOperations.OnSetup opsFactory;
        final boolean acceptGzip;
        final ChannelMetricsRecorder metricsRecorder;
        final long pendingAcquireTimeout;
        final InstrumentedPool<Connection> pool;
        final boolean retried;
        final MonoSink<Connection> sink;
        final Function<String, String> uriTagValue;

        PooledRef<Connection> pooledRef;
        Subscription subscription;

        DisposableAcquire(
                ConnectionObserver obs,
                ChannelOperations.OnSetup opsFactory,
                boolean acceptGzip,
                @Nullable ChannelMetricsRecorder metricsRecorder,
                long pendingAcquireTimeout,
                InstrumentedPool<Connection> pool,
                MonoSink<Connection> sink,
                @Nullable Function<String, String> uriTagValue) {
            this.cancellations = Disposables.composite();
            this.currentContext = Context.of(sink.contextView());
            this.obs = obs;
            this.opsFactory = opsFactory;
            this.acceptGzip = acceptGzip;
            this.metricsRecorder = metricsRecorder;
            this.pendingAcquireTimeout = pendingAcquireTimeout;
            this.pool = pool;
            this.retried = false;
            this.sink = sink;
            this.uriTagValue = uriTagValue;
        }

        DisposableAcquire(DisposableAcquire parent) {
            this.cancellations = parent.cancellations;
            this.currentContext = parent.currentContext;
            this.obs = parent.obs;
            this.opsFactory = parent.opsFactory;
            this.acceptGzip = parent.acceptGzip;
            this.metricsRecorder = parent.metricsRecorder;
            this.pendingAcquireTimeout = parent.pendingAcquireTimeout;
            this.pool = parent.pool;
            this.retried = true;
            this.sink = parent.sink;
            this.uriTagValue = parent.uriTagValue;
        }

        @Override
        public Context currentContext() {
            return currentContext;
        }

        @Override
        public void dispose() {
            subscription.cancel();
        }

        @Override
        public void onComplete() {
            // noop
        }

        @Override
        public void onError(Throwable t) {
            sink.error(t);
        }

        @Override
        public void onNext(PooledRef<Connection> pooledRef) {
//            this.pooledRef = pooledRef;
//            Channel channel = pooledRef.poolable().channel();
//
//            ConnectionObserver current = channel.attr(OWNER)
//                    .getAndSet(this);
//
//            if (current instanceof PendingConnectionObserver) {
//                PendingConnectionObserver pending = (PendingConnectionObserver) current;
//                PendingConnectionObserver.Pending p;
//
//                while ((p = pending.pendingQueue.poll()) != null) {
//                    if (p.error != null) {
//                        onUncaughtException(p.connection, p.error);
//                    }
//                    else if (p.state != null) {
//                        onStateChange(p.connection, p.state);
//                    }
//                }
//            }
//
//            if (notHttp2()) {
//                return;
//            }
//
//            if (isH2cUpgrade()) {
//                return;
//            }
//
//            if (getChannelContext(channel) != null) {
//                setChannelContext(channel, null);
//            }
//            http2StreamChannelBootstrap(channel).open().addListener(this);
            throw new Error();
        }

        @Override
        public void onStateChange(Connection connection, State newState) {
//            if (newState == UPGRADE_REJECTED) {
//                invalidate(connection.channel().attr(OWNER).get());
//            }
//
//            obs.onStateChange(connection, newState);
            throw new Error();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (Operators.validate(subscription, s)) {
                this.subscription = s;
                cancellations.add(this);
                if (!retried) {
                    sink.onCancel(cancellations);
                }
                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onUncaughtException(Connection connection, Throwable error) {
            obs.onUncaughtException(connection, error);
        }

        @Override
        public void operationComplete(Future<Http2StreamChannel> future) {
//            Channel channel = pooledRef.poolable().channel();
//            Http2Pool.Http2PooledRef http2PooledRef = http2PooledRef(pooledRef);
//            ChannelHandlerContext frameCodec = http2PooledRef.slot.http2FrameCodecCtx();
//            if (future.isSuccess()) {
//                Http2StreamChannel ch = future.getNow();
//
//                if (!channel.isActive() || frameCodec == null ||
//                        ((Http2FrameCodec) frameCodec.handler()).connection().goAwayReceived() ||
//                        !((Http2FrameCodec) frameCodec.handler()).connection().local().canOpenStream()) {
//                    invalidate(this);
//                    if (!retried) {
//                        if (log.isDebugEnabled()) {
//                            log.debug(format(ch, "Immediately aborted pooled channel, max active streams is reached, " +
//                                    "re-acquiring a new channel"));
//                        }
//                        pool.acquire(Duration.ofMillis(pendingAcquireTimeout))
//                                .contextWrite(ctx -> ctx.put(CONTEXT_CALLER_EVENTLOOP, channel.eventLoop()))
//                                .subscribe(new DisposableAcquire(this));
//                    }
//                    else {
//                        sink.error(new IOException("Error while acquiring from " + pool + ". Max active streams is reached."));
//                    }
//                }
//                else {
//                    Http2ConnectionProvider.registerClose(ch, this);
//                    if (!currentContext().isEmpty()) {
//                        setChannelContext(ch, currentContext());
//                    }
//                    HttpClientConfig.addStreamHandlers(ch, obs.then(new HttpClientConfig.StreamConnectionObserver(currentContext())),
//                            opsFactory, acceptGzip, metricsRecorder, -1, uriTagValue);
//
//                    ChannelOperations<?, ?> ops = ChannelOperations.get(ch);
//                    if (ops != null) {
//                        obs.onStateChange(ops, STREAM_CONFIGURED);
//                        sink.success(ops);
//                    }
//
//                    if (log.isDebugEnabled()) {
//                        Http2Connection.Endpoint<Http2LocalFlowController> localEndpoint = ((Http2FrameCodec) frameCodec.handler()).connection().local();
//                        logStreamsState(ch, localEndpoint, "Stream opened");
//                    }
//                }
//            }
//            else {
//                invalidate(this);
//                sink.error(future.cause());
//            }
            throw new Error();
        }

        boolean isH2cUpgrade() {
//            Channel channel = pooledRef.poolable().channel();
//            Http2Pool.Http2PooledRef http2PooledRef = http2PooledRef(pooledRef);
//            if (http2PooledRef.slot.h2cUpgradeHandlerCtx() != null &&
//                    http2PooledRef.slot.http2MultiplexHandlerCtx() == null) {
//                ChannelOperations<?, ?> ops = ChannelOperations.get(channel);
//                if (ops != null) {
//                    sink.success(ops);
//                    return true;
//                }
//            }
//            return false;
            throw new Error();
        }

        boolean notHttp2() {
//            Channel channel = pooledRef.poolable().channel();
//            Http2Pool.Http2PooledRef http2PooledRef = http2PooledRef(pooledRef);
//            String applicationProtocol = http2PooledRef.slot.applicationProtocol;
//            if (applicationProtocol != null) {
//                if (ApplicationProtocolNames.HTTP_1_1.equals(applicationProtocol)) {
//                    // No information for the negotiated application-level protocol,
//                    // or it is HTTP/1.1, continue as an HTTP/1.1 request
//                    // and remove the connection from this pool.
//                    ChannelOperations<?, ?> ops = ChannelOperations.get(channel);
//                    if (ops != null) {
//                        sink.success(ops);
//                        invalidate(this);
//                        return true;
//                    }
//                }
//                else if (!ApplicationProtocolNames.HTTP_2.equals(applicationProtocol)) {
//                    channel.attr(OWNER).set(null);
//                    invalidate(this);
//                    sink.error(new IOException("Unknown protocol [" + applicationProtocol + "]."));
//                    return true;
//                }
//            }
//            else if (http2PooledRef.slot.h2cUpgradeHandlerCtx() == null &&
//                    http2PooledRef.slot.http2MultiplexHandlerCtx() == null) {
//                // It is not H2. There are no handlers for H2C upgrade/H2C prior-knowledge,
//                // continue as an HTTP/1.1 request and remove the connection from this pool.
//                ChannelOperations<?, ?> ops = ChannelOperations.get(channel);
//                if (ops != null) {
//                    sink.success(ops);
//                    invalidate(this);
//                    return true;
//                }
//            }
//            return false;
            throw new Error();
        }

        static Http2Pool.Http2PooledRef http2PooledRef(PooledRef<Connection> pooledRef) {
            return pooledRef instanceof Http2Pool.Http2PooledRef ?
                    (Http2Pool.Http2PooledRef) pooledRef :
                    (Http2Pool.Http2PooledRef) pooledRef.metadata();
        }

        static final AttributeKey<Http2StreamChannelBootstrap> HTTP2_STREAM_CHANNEL_BOOTSTRAP =
                AttributeKey.valueOf("http2StreamChannelBootstrap");

        static Http2StreamChannelBootstrap http2StreamChannelBootstrap(Channel channel) {
            Http2StreamChannelBootstrap http2StreamChannelBootstrap;

            for (;;) {
                http2StreamChannelBootstrap = channel.attr(HTTP2_STREAM_CHANNEL_BOOTSTRAP).get();
                if (http2StreamChannelBootstrap == null) {
                    http2StreamChannelBootstrap = new Http2StreamChannelBootstrap(channel);
                }
                else {
                    return http2StreamChannelBootstrap;
                }
                if (channel.attr(HTTP2_STREAM_CHANNEL_BOOTSTRAP)
                        .compareAndSet(null, http2StreamChannelBootstrap)) {
                    return http2StreamChannelBootstrap;
                }
            }
        }
    }

    static final String CONNECTION_PROVIDER_NAME = "http2";
    static final String NAME_SEPARATOR = ".";
}
