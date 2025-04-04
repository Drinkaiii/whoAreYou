package eth.whoAreYou.service;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.ReadonlyTransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TokenInfoService {

    @Autowired
    @Qualifier("web3jHttp")
    private Web3j web3j;

    public String callContractMethod(String contractAddress, String methodName) throws Exception {
        Function function = new Function(
                methodName,
                Collections.emptyList(),
                Collections.singletonList(new org.web3j.abi.TypeReference<Utf8String>() {})
        );

        String encodedFunction = FunctionEncoder.encode(function);

        EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, contractAddress, encodedFunction),
                org.web3j.protocol.core.DefaultBlockParameterName.LATEST
        ).send();

        List<Type> results = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());

        return results.isEmpty() ? null : results.get(0).getValue().toString();
    }

    public TokenInfo getTokenInfo(String contractAddress) throws Exception {
        String name = callContractMethod(contractAddress, "name");
        String symbol = callContractMethod(contractAddress, "symbol");
        return new TokenInfo(name, symbol);
    }

    public record TokenInfo(String name, String symbol) {}
}


