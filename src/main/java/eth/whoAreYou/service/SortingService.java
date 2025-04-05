package eth.whoAreYou.service;

import eth.whoAreYou.dto.TokenDetailsDto;
import eth.whoAreYou.dto.TokenInfoDto;
import eth.whoAreYou.dto.InteractionInfoDto;
import eth.whoAreYou.dto.AddressInfoDto;
import lombok.RequiredArgsConstructor;
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

    private final Web3j web3jHttp;
    private final TokenDetailsService tokenInfoService;
    private final NFTInfoService nftInfoService;
    private final TokenPriceService tokenPriceService;
    private final TokenIconService tokenIconService;
    private final AddressInteractionService addressInteractionService;

    public AddressInfoDto classify(String targetAddress, String selfAddress) throws Exception {
        String checksumAddress = Keys.toChecksumAddress(targetAddress);
        EthGetCode ethGetCode = web3jHttp.ethGetCode(checksumAddress, DefaultBlockParameterName.LATEST).send();

        String code = ethGetCode.getCode();

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
        String implementation = resolveImplementationAddress(checksumAddress);
        String resolvedAddress = implementation != null ? implementation : checksumAddress;

        // Step 2: ERC-721 (Try ERC-165 interfaces)
        if (supportsInterface(resolvedAddress, "0x80ac58cd")) {
            return AddressInfoDto.builder()
                    .addressType("ERC-721")
                    .resolvedAddress(resolvedAddress)
                    .details(nftInfoService.getNFTInfo(resolvedAddress))
                    .build();
        }

        // Step 3: ERC-1155 (Try ERC-165 interfaces)
        if (supportsInterface(resolvedAddress, "0xd9b67a26")) {
            return AddressInfoDto.builder()
                    .addressType("ERC-1155")
                    .resolvedAddress(resolvedAddress)
                    .details("ERC-1155 尚未實作 Service")
                    .build();
        }

        // Step 4: ERC-20 (check decimals/symbol/name)
        if (hasERC20Interface(resolvedAddress)) {
            TokenDetailsDto tokenDetailsDto = tokenInfoService.getTokenDetails(resolvedAddress);
            double price = tokenPriceService.getTokenPrice(resolvedAddress).getOrDefault("price", -1.0);
            String icon = tokenIconService.getTokenIcon(resolvedAddress);
            TokenInfoDto tokenInfoDto = TokenInfoDto.builder()
                    .name(tokenDetailsDto.getName())
                    .symbol(tokenDetailsDto.getSymbol())
                    .price(price)
                    .iconUrl(icon)
                    .build();
            return AddressInfoDto.builder()
                    .addressType("ERC-20")
                    .resolvedAddress(resolvedAddress)
                    .details(tokenInfoDto)
                    .build();
        }

        // Step 5: Unknown 合約類型
        return AddressInfoDto.builder()
                .addressType("Unknown")
                .resolvedAddress(resolvedAddress)
                .details(null)
                .build();
    }

    private boolean hasERC20Interface(String address) {
        try {
            Function decimals = new Function("decimals", List.of(), List.of(new TypeReference<Uint8>() {}));
            String encoded = FunctionEncoder.encode(decimals);
            EthCall call = web3jHttp.ethCall(Transaction.createEthCallTransaction(null, address, encoded), DefaultBlockParameterName.LATEST).send();
            return call.getValue() != null && !call.getValue().equals("0x");
        } catch (Exception e) {
            return false;
        }
    }

    private boolean supportsInterface(String address, String interfaceId) {
        try {
            Function supportsInterface = new Function("supportsInterface",
                    List.of(new Bytes4(Numeric.hexStringToByteArray(interfaceId))),
                    List.of(new TypeReference<Bool>() {}));
            String encoded = FunctionEncoder.encode(supportsInterface);
            EthCall call = web3jHttp.ethCall(Transaction.createEthCallTransaction(null, address, encoded),
                    DefaultBlockParameterName.LATEST).send();
            return call.getValue() != null && call.getValue().endsWith("1");
        } catch (Exception e) {
            return false;
        }
    }

    private String resolveImplementationAddress(String proxyAddress) {
        try {
            BigInteger slot = new BigInteger("360894a13ba1a3210667c828492db98dca3e2076cc3735a920a3ca505d382bbc", 16);
            EthGetStorageAt storage = web3jHttp.ethGetStorageAt(proxyAddress, slot, DefaultBlockParameterName.LATEST).send();

            String raw = storage.getData();
            // EIP-1967 null is 0x000000...（66 char: 0x + 64*0）
            if (raw == null || raw.equals("0x") || raw.equals("0x0000000000000000000000000000000000000000000000000000000000000000")) {
                return null; // if it isn't proxy, system will use origin address
            }

            if (raw.length() < 66) return null;
            String impl = "0x" + raw.substring(raw.length() - 40);
            return Keys.toChecksumAddress(impl);
        } catch (Exception e) {
            return null;
        }
    }
}