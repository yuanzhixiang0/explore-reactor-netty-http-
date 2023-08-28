package reactor.netty.http.client;

import io.netty.channel.ChannelOption;
import io.netty.util.AsciiString;
import io.netty.util.NetUtil;
import reactor.netty.transport.AddressUtils;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.function.BiFunction;

class HttpClientConnect extends HttpClient {

    final HttpClientConfig config;

    HttpClientConnect(HttpConnectionProvider provider) {
        this.config = new HttpClientConfig(
                provider,
                Collections.singletonMap(ChannelOption.AUTO_READ, false),
                () -> AddressUtils.createUnresolved(NetUtil.LOCALHOST.getHostAddress(), DEFAULT_PORT));
    }

    HttpClientConnect(HttpClientConfig config) {
        this.config = config;
    }

    @Override
    public HttpClientConfig configuration() {
        throw new Error();
    }

    @Override
    protected HttpClient duplicate() {
        throw new Error();
    }

    static final AsciiString ALL = new AsciiString("*/*");

    static final int DEFAULT_PORT = System.getenv("PORT") != null ? Integer.parseInt(System.getenv("PORT")) : 80;

    static final Logger log = Loggers.getLogger(HttpClientConnect.class);

    static final BiFunction<String, Integer, InetSocketAddress> URI_ADDRESS_MAPPER = AddressUtils::createUnresolved;
}
