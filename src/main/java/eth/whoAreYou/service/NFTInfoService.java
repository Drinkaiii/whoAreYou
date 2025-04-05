package eth.whoAreYou.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eth.whoAreYou.dto.NFTInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.DefaultBlockParameterName;

import java.math.BigInteger;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NFTInfoService {

    @Autowired
    @Qualifier("ethereumWeb3jHttp")
    private Web3j ethereumWeb3jHttp;

    @Autowired
    @Qualifier("baseWeb3jHttp")
    private Web3j baseWeb3jHttp;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public NFTInfoDto getNFTInfo(String contractAddress, String blockchain) throws Exception {
        Web3j web3j = resolveWeb3j(blockchain);

        String name = callStringMethod(web3j, contractAddress, "name");
        BigInteger totalSupply = callUintMethod(web3j, contractAddress, "totalSupply");

        // use tokenId = 0 metadata
        String tokenUri = callTokenURIMethod(web3j, contractAddress, BigInteger.ZERO);
        String imageUrl = extractImageFromMetadata(tokenUri);

        return new NFTInfoDto(name, totalSupply, imageUrl);
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

    private String callStringMethod(Web3j web3j, String contractAddress, String methodName) throws Exception {
        Function function = new Function(
                methodName,
                Collections.emptyList(),
                Collections.singletonList(new TypeReference<Utf8String>() {})
        );

        String encoded = FunctionEncoder.encode(function);
        EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, contractAddress, encoded),
                DefaultBlockParameterName.LATEST
        ).send();

        List<Type> result = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        return result.isEmpty() ? null : result.get(0).getValue().toString();
    }

    private BigInteger callUintMethod(Web3j web3j, String contractAddress, String methodName) throws Exception {
        Function function = new Function(
                methodName,
                Collections.emptyList(),
                Collections.singletonList(new TypeReference<Uint256>() {})
        );

        String encoded = FunctionEncoder.encode(function);
        EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, contractAddress, encoded),
                DefaultBlockParameterName.LATEST
        ).send();

        List<Type> result = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        return result.isEmpty() ? BigInteger.ZERO : (BigInteger) result.get(0).getValue();
    }

    private String callTokenURIMethod(Web3j web3j, String contractAddress, BigInteger tokenId) throws Exception {
        Function function = new Function(
                "tokenURI",
                Arrays.asList(new Uint256(tokenId)),
                Collections.singletonList(new TypeReference<Utf8String>() {})
        );

        String encoded = FunctionEncoder.encode(function);
        EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, contractAddress, encoded),
                DefaultBlockParameterName.LATEST
        ).send();

        List<Type> result = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        return result.isEmpty() ? null : result.get(0).getValue().toString();
    }

    private String extractImageFromMetadata(String metadataUrl) throws Exception {
        if (metadataUrl.startsWith("ipfs://")) {
            metadataUrl = metadataUrl.replace("ipfs://", "https://ipfs.io/ipfs/");
        }

        JsonNode json = objectMapper.readTree(new URL(metadataUrl));

        if (json.has("image")) {
            return fixIpfsLink(json.get("image").asText());
        } else if (json.has("image_url")) {
            return fixIpfsLink(json.get("image_url").asText());
        } else {
            return null;
        }
    }

    private String fixIpfsLink(String url) {
        if (url.startsWith("ipfs://")) {
            return url.replace("ipfs://", "https://ipfs.io/ipfs/");
        }
        return url;
    }
}
