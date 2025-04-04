package eth.whoAreYou.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.NetVersion;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.websocket.WebSocketService;

import java.io.IOException;

@Configuration
public class Web3jConfig {

    @Value("${NODIT_ETHEREUM_WEBSOCKET_URL}")
    private String noditEthereumWebsocketUrl;

    @Value("${NODIT_ETHEREUM_HTTP_URL}")
    private String noditEthereumHttpUrl;

    @Value("${NODIT_SEPOLIA_WEBSOCKET_URL}")
    private String noditSepoliaWebsocketUrl;

    @Value("${NODIT_SEPOLIA_HTTP_URL}")
    private String noditSepoliaHttpUrl;

    @Value("${blockchain:ethereum}")
    private String blockchain;

    @Bean
    @Lazy
    public Web3j web3jWebsocket() throws IOException {
        WebSocketService webSocketService;
        switch (blockchain) {
            case "ethereum":
                webSocketService = new WebSocketService(noditEthereumWebsocketUrl, true);

                break;
            case "sepolia":
                webSocketService = new WebSocketService(noditSepoliaWebsocketUrl, true);
                break;
            default:
                webSocketService = new WebSocketService(noditEthereumWebsocketUrl, true);
        }
        webSocketService.connect();
        return Web3j.build(webSocketService);
    }

    @Bean
    public Web3j web3jHttp() {
        Web3j web3j;
        switch (blockchain) {
            case "ethereum":
                web3j = Web3j.build(new HttpService(noditEthereumHttpUrl));
                break;
            case "sepolia":
                web3j = Web3j.build(new HttpService(noditSepoliaHttpUrl));
                break;
            default:
                web3j = Web3j.build(new HttpService(noditEthereumHttpUrl));
        }
        return web3j;
    }

}


