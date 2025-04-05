package eth.whoAreYou.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.websocket.WebSocketService;

import java.io.IOException;

@Configuration
public class Web3jConfig {

    @Value("${NODIT_ETHEREUM_WEBSOCKET_URL}")
    private String noditEthereumWebsocketUrl;

    @Value("${NODIT_ETHEREUM_HTTP_URL}")
    private String noditEthereumHttpUrl;

    @Value("${NODIT_BASE_WEBSOCKET_URL}")
    private String noditBaseWebsocketUrl;

    @Value("${NODIT_BASE_HTTP_URL}")
    private String noditBaseHttpUrl;

    // Ethereum WebSocket
    @Bean(name = "ethereumWeb3jWebSocket")
    @Lazy
    public Web3j ethereumWeb3jWebSocket() throws IOException {
        WebSocketService webSocketService = new WebSocketService(noditEthereumWebsocketUrl, true);
        webSocketService.connect();
        return Web3j.build(webSocketService);
    }

    // Ethereum HTTP
    @Bean(name = "ethereumWeb3jHttp")
    public Web3j ethereumWeb3jHttp() {
        return Web3j.build(new HttpService(noditEthereumHttpUrl));
    }

    // Base WebSocket
    @Bean(name = "baseWeb3jWebSocket")
    @Lazy
    public Web3j baseWeb3jWebSocket() throws IOException {
        WebSocketService webSocketService = new WebSocketService(noditBaseWebsocketUrl, true);
        webSocketService.connect();
        return Web3j.build(webSocketService);
    }

    // Base HTTP
    @Bean(name = "baseWeb3jHttp")
    public Web3j baseWeb3jHttp() {
        return Web3j.build(new HttpService(noditBaseHttpUrl));
    }
}
