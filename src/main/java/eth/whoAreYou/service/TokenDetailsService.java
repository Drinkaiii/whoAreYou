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
    @Qualifier("web3jHttp")
    private Web3j web3j;

    public String callContractMethod(String contractAddress, String methodName) throws Exception {
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

    public TokenDetailsDto getTokenDetails(String contractAddress) throws Exception {
        String name = callContractMethod(contractAddress, "name");
        String symbol = callContractMethod(contractAddress, "symbol");
        return TokenDetailsDto.builder()
                .name(name)
                .symbol(symbol)
                .build();
    }
}
