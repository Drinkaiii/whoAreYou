package eth.whoAreYou.service;

import eth.whoAreYou.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.generated.Bytes4;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.abi.TypeReference;
import org.web3j.crypto.Keys;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SortingService {

    @Autowired
    @Qualifier("ethereumWeb3jHttp")
    private Web3j ethereumWeb3jHttp;

    @Autowired
    @Qualifier("baseWeb3jHttp")
    private Web3j baseWeb3jHttp;

    private final TokenDetailsService tokenInfoService;
    private final NFTInfoService nftInfoService;
    private final TokenPriceService tokenPriceService;
    private final TokenIconService tokenIconService;
    private final AddressInteractionService addressInteractionService;

    public Object classify(String targetAddress, String selfAddress) throws Exception {
        String blockchain="";
        String checksumAddress = Keys.toChecksumAddress(targetAddress);
        EthGetCode ethereumGetCode = ethereumWeb3jHttp.ethGetCode(checksumAddress, DefaultBlockParameterName.LATEST).send();
        String code = ethereumGetCode.getCode();
        if (code != null && !code.equals("0x") && !code.equals("0x0")){
            blockchain = "ETHEREUM";
        }
        else {
            EthGetCode baseGetCode = baseWeb3jHttp.ethGetCode(checksumAddress, DefaultBlockParameterName.LATEST).send();
            code = baseGetCode.getCode();
            if (code != null && !code.equals("0x") && !code.equals("0x0"))
                blockchain = "BASE";
        }
        Web3j web3j = getWeb3jByChain(blockchain);

        if (code == null || code.equals("0x") || code.equals("0x0")) {
            // 是 EOA，檢查是否與 selfAddress 有互動
            AddressInfoDto.AddressInfoDtoBuilder builder = AddressInfoDto.builder()
                    .addressType("EOA")
                    .resolvedAddress(checksumAddress);

            if (selfAddress != null && !selfAddress.isBlank()) {
                InteractionInfoDto interaction = addressInteractionService.getInteractionInfo(selfAddress, checksumAddress);
                builder.details(interaction);
            } else {
                builder.details(null);
            }

            return builder.build();
        }

        // Step 1: resolve proxy (EIP-1967)
        String implementation = resolveImplementationAddress(web3j, checksumAddress);
        String resolvedAddress = implementation != null ? implementation : checksumAddress;

        // Step 2: ERC-721 (Try ERC-165 interfaces)
        if (supportsInterface(web3j, resolvedAddress, "0x80ac58cd")) {
            AddressInfoDto addressInfoDto = AddressInfoDto.builder()
                    .chain(blockchain)
                    .addressType("ERC-721")
                    .resolvedAddress(resolvedAddress)
                    .details(nftInfoService.getNFTInfo(resolvedAddress,blockchain))
                    .build();
            return AddressResponseDto.builder().data(List.of(addressInfoDto)).build();
        }

        // Step 3: ERC-1155 (Try ERC-165 interfaces)
        if (supportsInterface(web3j, resolvedAddress, "0xd9b67a26")) {
            AddressInfoDto addressInfoDto = AddressInfoDto.builder()
                    .chain(blockchain)
                    .addressType("ERC-1155")
                    .resolvedAddress(resolvedAddress)
                    .details("ERC-1155 尚未實作 Service")
                    .build();
            return AddressResponseDto.builder().data(List.of(addressInfoDto)).build();
        }

        // Step 4: ERC-20 (check decimals/symbol/name)
        if (hasERC20Interface(web3j, resolvedAddress)) {
            TokenDetailsDto tokenDetailsDto = tokenInfoService.getTokenDetails(resolvedAddress, blockchain);
            double price = tokenPriceService.getTokenPrice(resolvedAddress).getOrDefault("price", -1.0);
            String icon = tokenIconService.getTokenIcon(resolvedAddress);
            TokenInfoDto tokenInfoDto = TokenInfoDto.builder()
                    .name(tokenDetailsDto.getName())
                    .symbol(tokenDetailsDto.getSymbol())
                    .price(price)
                    .iconUrl(icon)
                    .build();
            AddressInfoDto addressInfoDto = AddressInfoDto.builder()
                    .chain(blockchain)
                    .addressType("ERC-20")
                    .resolvedAddress(resolvedAddress)
                    .details(tokenInfoDto)
                    .build();
            return AddressResponseDto.builder().data(List.of(addressInfoDto)).build();
        }

        // Step 5: Unknown 合約類型
        AddressInfoDto addressInfoDto = AddressInfoDto.builder()
                .addressType("Unknown")
                .resolvedAddress(resolvedAddress)
                .details(null)
                .build();
        return AddressResponseDto.builder().data(List.of(addressInfoDto)).build();
    }

    private boolean hasERC20Interface(Web3j web3j, String address) {
        try {
            Function decimals = new Function("decimals", List.of(), List.of(new TypeReference<Uint8>() {}));
            String encoded = FunctionEncoder.encode(decimals);
            EthCall call = web3j.ethCall(Transaction.createEthCallTransaction(null, address, encoded), DefaultBlockParameterName.LATEST).send();
            return call.getValue() != null && !call.getValue().equals("0x");
        } catch (Exception e) {
            return false;
        }
    }

    private boolean supportsInterface(Web3j web3j, String address, String interfaceId) {
        try {
            Function supportsInterface = new Function("supportsInterface",
                    List.of(new Bytes4(Numeric.hexStringToByteArray(interfaceId))),
                    List.of(new TypeReference<Bool>() {}));
            String encoded = FunctionEncoder.encode(supportsInterface);
            EthCall call = web3j.ethCall(Transaction.createEthCallTransaction(null, address, encoded),
                    DefaultBlockParameterName.LATEST).send();
            return call.getValue() != null && call.getValue().endsWith("1");
        } catch (Exception e) {
            return false;
        }
    }

    private String resolveImplementationAddress(Web3j web3j, String proxyAddress) {
        try {
            BigInteger slot = new BigInteger("360894a13ba1a3210667c828492db98dca3e2076cc3735a920a3ca505d382bbc", 16);
            EthGetStorageAt storage = web3j.ethGetStorageAt(
                    proxyAddress,
                    slot,
                    DefaultBlockParameterName.LATEST
            ).send();

            String raw = storage.getData();
            if (raw == null || raw.equals("0x") || raw.equals("0x0000000000000000000000000000000000000000000000000000000000000000")) {
                return null;
            }

            if (raw.length() < 66) return null;

            String impl = "0x" + raw.substring(raw.length() - 40);
            return Keys.toChecksumAddress(impl);
        } catch (Exception e) {
            return null;
        }
    }

    private Web3j getWeb3jByChain(String blockchain) {
        return switch (blockchain.toUpperCase()) {
            case "ETHEREUM" -> ethereumWeb3jHttp;
            case "BASE" -> baseWeb3jHttp;
            default -> throw new IllegalArgumentException("Unsupported blockchain: " + blockchain);
        };
    }

}