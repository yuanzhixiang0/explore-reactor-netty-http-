package reactor.netty.http;

import reactor.netty.Connection;
import reactor.netty.ConnectionObserver;
import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;
import reactor.netty.channel.ChannelOperations;
import reactor.netty.http.logging.HttpMessageLogFactory;

/**
 * An HTTP ready {@link ChannelOperations} with state management for status and headers
 * (first HTTP response packet).
 *
 * @author Stephane Maldini
 */
public abstract class HttpOperations<INBOUND extends NettyInbound, OUTBOUND extends NettyOutbound>
        extends ChannelOperations<INBOUND, OUTBOUND> implements HttpInfos {

    volatile int statusAndHeadersSent;

    static final int READY        = 0;
    static final int HEADERS_SENT = 1;
    static final int BODY_SENT    = 2;

    final HttpMessageLogFactory httpMessageLogFactory;

    protected HttpOperations(HttpOperations<INBOUND, OUTBOUND> replaced) {
        super(replaced);
        this.httpMessageLogFactory = replaced.httpMessageLogFactory;
        this.statusAndHeadersSent = replaced.statusAndHeadersSent;
    }

    protected HttpOperations(Connection connection, ConnectionObserver listener, HttpMessageLogFactory httpMessageLogFactory) {
        super(connection, listener);
        this.httpMessageLogFactory = httpMessageLogFactory;
    }


}
