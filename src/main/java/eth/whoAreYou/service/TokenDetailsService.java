package eth.whoAreYou.service;

import eth.whoAreYou.dto.TokenDetailsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.DefaultBlockParameterName;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TokenDetailsService {

    @Autowired
    @Qualifier("ethereumWeb3jHttp")
    private Web3j ethereumWeb3jHttp;

    @Autowired
    @Qualifier("baseWeb3jHttp")
    private Web3j baseWeb3jHttp;

    public TokenDetailsDto getTokenDetails(String contractAddress, String blockchain) throws Exception {
        Web3j web3j = resolveWeb3j(blockchain);

        String name = callContractMethod(web3j, contractAddress, "name");
        String symbol = callContractMethod(web3j, contractAddress, "symbol");

        return TokenDetailsDto.builder()
                .name(name)
                .symbol(symbol)
                .build();
    }

    private String callContractMethod(Web3j web3j, String contractAddress, String methodName) throws Exception {
        Function function = new Function(
                methodName,
                Collections.emptyList(),
                Collections.singletonList(new TypeReference<Utf8String>() {})
        );

        String encodedFunction = FunctionEncoder.encode(function);

        EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, contractAddress, encodedFunction),
                DefaultBlockParameterName.LATEST
        ).send();

        List<Type> results = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());

        return results.isEmpty() ? null : results.get(0).getValue().toString();
    }

    private Web3j resolveWeb3j(String blockchain) {
        if ("ETHEREUM".equalsIgnoreCase(blockchain)) {
            return ethereumWeb3jHttp;
        } else if ("BASE".equalsIgnoreCase(blockchain)) {
            return baseWeb3jHttp;
        } else {
            throw new IllegalArgumentException("Unsupported blockchain: " + blockchain);
        }
    }
}
