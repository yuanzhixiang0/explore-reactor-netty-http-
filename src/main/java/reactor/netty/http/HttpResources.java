package reactor.netty.http;

import io.netty.resolver.AddressResolverGroup;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpResources;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

public final class HttpResources extends TcpResources {


    /**
     * Return the global HTTP resources for event loops and pooling
     *
     * @return the global HTTP resources for event loops and pooling
     */
    public static HttpResources get() {
        return getOrCreate(httpResources, null, null, ON_HTTP_NEW, "http");
    }

    final AtomicReference<ConnectionProvider> http2ConnectionProvider;

    HttpResources(LoopResources loops, ConnectionProvider provider) {
        super(loops, provider);
        http2ConnectionProvider = new AtomicReference<>();
    }

    @Override
    public AddressResolverGroup<?> getOrCreateDefaultResolver() {
        return super.getOrCreateDefaultResolver();
    }

    static final BiFunction<LoopResources, ConnectionProvider, HttpResources> ON_HTTP_NEW;

    static final AtomicReference<HttpResources> httpResources;

    static {
        ON_HTTP_NEW = HttpResources::new;
        httpResources = new AtomicReference<>();
    }
}
