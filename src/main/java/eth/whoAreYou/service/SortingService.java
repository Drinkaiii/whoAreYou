package eth.whoAreYou.service;

import eth.whoAreYou.client.AnomalyDetectorClient;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SortingService {

    @Autowired
    @Qualifier("ethereumWeb3jHttp")
    private Web3j ethereumWeb3jHttp;

    @Autowired
    @Qualifier("baseWeb3jHttp")
    private Web3j baseWeb3jHttp;

    @Autowired
    private TokenFeatureService tokenFeatureService;

    private final TokenDetailsService tokenInfoService;
    private final NFTInfoService nftInfoService;
    private final TokenPriceService tokenPriceService;
    private final TokenIconService tokenIconService;
    private final AddressInteractionService addressInteractionService;
    private final TransactionFetcher fetcher;
    private final FeatureExtractor extractor;
    private final AnomalyDetectorClient detector;

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
            else
                blockchain = "ETHEREUM"; // default to Ethereum if no code found
        }
        Web3j web3j = getWeb3jByChain(blockchain);


        if (code == null || code.equals("0x") || code.equals("0x0")) {
            // 是 EOA，查詢兩條鏈的資訊
            List<AddressInfoDto> addressInfoList = new ArrayList<>();

            // 針對兩條鏈分別查詢
            List<String> chains = List.of("ETHEREUM", "BASE");

            for (String chain : chains) {
                AddressInfoDto.AddressInfoDtoBuilder builder = AddressInfoDto.builder()
                        .addressType("EOA")
                        .chain(chain)  // 設置鏈標識
                        .resolvedAddress(checksumAddress);

                // 建立一個 Map 來包含不同來源的資訊
                java.util.Map<String, Object> detailsMap = new java.util.HashMap<>();

                // 添加互動信息（如果有）
                if (selfAddress != null && !selfAddress.isBlank()) {
                    InteractionInfoDto interaction = addressInteractionService.getInteractionInfo(selfAddress, checksumAddress, chain);
                    detailsMap.put("interaction", interaction);
                }

                try {
                    // 提取特徵並進行異常檢測 - 使用對應鏈的端點
                    TransactionFetcher.FetchResult result = fetcher.fetchTransactions(checksumAddress, 1000, 1, chain);
                    eth.whoAreYou.dto.WalletFeatureDto features = extractor.extract(result.transactions(), checksumAddress, result.totalCount());
                    java.util.Map<String, Object> anomalyResult = detector.predict(features, chain);  // 傳入鏈參數
                    detailsMap.put("anomalyDetection", anomalyResult);
                } catch (Exception e) {
                    // 處理異常情況
                    detailsMap.put("anomalyDetection", java.util.Map.of("error", e.getMessage()));
                }

                builder.details(detailsMap);
                addressInfoList.add(builder.build());
            }

            // 返回包含兩條鏈資訊的結果
            return AddressResponseDto.builder().data(addressInfoList).build();
        }
        // Step 1: resolve proxy (EIP-1967)
        String implementation = resolveImplementationAddress(web3j, checksumAddress);
        String resolvedAddress = implementation != null ? implementation : checksumAddress;
        System.out.println("Resolved address: " + resolvedAddress);


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

            double price = tokenPriceService.getTokenPrice(checksumAddress).getOrDefault("price", -1.0);
            String icon = tokenIconService.getTokenIcon(checksumAddress, blockchain);

            TokenInfoDto tokenInfoDto = TokenInfoDto.builder()
                    .name(tokenDetailsDto.getName())
                    .symbol(tokenDetailsDto.getSymbol())
                    .price(price)
                    .iconUrl(icon)
                    .build();

            // 獲取代幣特徵和預測
            Map<String, Object> featureAndPrediction = tokenFeatureService.fetchTokenFeatureAndPredict(resolvedAddress, blockchain);

            // 如果有預測結果，添加到details中
            if (featureAndPrediction != null) {
                Map<String, Object> details = new HashMap<>();
                details.put("basicInfo", tokenInfoDto);
                details.put("features", featureAndPrediction);

                AddressInfoDto addressInfoDto = AddressInfoDto.builder()
                        .chain(blockchain)
                        .addressType("ERC-20")
                        .resolvedAddress(resolvedAddress)
                        .details(details)
                        .build();

                return AddressResponseDto.builder().data(List.of(addressInfoDto)).build();
            } else {
                // 如果無法獲取預測，只返回基本信息
                AddressInfoDto addressInfoDto = AddressInfoDto.builder()
                        .chain(blockchain)
                        .addressType("ERC-20")
                        .resolvedAddress(resolvedAddress)
                        .details(tokenInfoDto)
                        .build();

                return AddressResponseDto.builder().data(List.of(addressInfoDto)).build();
            }
        }

        // Step 5: Unknown 合約類型
        AddressInfoDto addressInfoDto = AddressInfoDto.builder()
                .chain(blockchain)
                .addressType("Unknown")
                .resolvedAddress(resolvedAddress)
                .details(null)
                .build();
        return AddressResponseDto.builder().data(List.of(addressInfoDto)).build();
    }

    private boolean hasERC20Interface(Web3j web3j, String address) {
        return callSimpleFunction(web3j, address, "decimals") ||
                callSimpleFunction(web3j, address, "symbol") ||
                callSimpleFunction(web3j, address, "name");
    }

    private boolean callSimpleFunction(Web3j web3j, String address, String methodName) {
        try {
            Function fn = new Function(methodName, List.of(), List.of(new TypeReference<org.web3j.abi.datatypes.Utf8String>() {}));
            if (methodName.equals("decimals")) {
                fn = new Function("decimals", List.of(), List.of(new TypeReference<Uint8>() {}));
            }
            String encoded = FunctionEncoder.encode(fn);
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
            String[] slotHexesToTry = {
                    "0x360894a13ba1a3210667c828492db98dca3e2076cc3735a920a3ca505d382bbc",  // EIP-1967
                    "0x5c60da1b27dd6c2cfa54c73ec8d58c6fdecf0f3c"                           // AdminUpgradeabilityProxy
            };

            for (String hexSlot : slotHexesToTry) {
                BigInteger slot = new BigInteger(hexSlot.substring(2), 16); // 去掉 0x 開頭再轉
                EthGetStorageAt storage = web3j.ethGetStorageAt(
                        proxyAddress,
                        slot,
                        DefaultBlockParameterName.LATEST
                ).send();

                String raw = storage.getData();
                if (raw != null && raw.length() >= 66 &&
                        !raw.equals("0x") &&
                        !raw.endsWith("0000000000000000000000000000000000000000")) {

                    String impl = "0x" + raw.substring(raw.length() - 40);
                    return Keys.toChecksumAddress(impl);
                }
            }

            // fallback: 嘗試直接 call `implementation()` 方法
            Function implFn = new Function("implementation", List.of(), List.of(new TypeReference<org.web3j.abi.datatypes.Address>() {}));
            String encoded = FunctionEncoder.encode(implFn);
            EthCall call = web3j.ethCall(Transaction.createEthCallTransaction(null, proxyAddress, encoded), DefaultBlockParameterName.LATEST).send();
            String value = call.getValue();
            if (value != null && value.length() >= 66 && !value.equals("0x")) {
                String impl = "0x" + value.substring(value.length() - 40);
                return Keys.toChecksumAddress(impl);
            }

        } catch (Exception e) {
            System.out.println("resolveImplementationAddress error: " + e.getMessage());
        }
        return null;
    }

    private Web3j getWeb3jByChain(String blockchain) {
        return switch (blockchain.toUpperCase()) {
            case "ETHEREUM" -> ethereumWeb3jHttp;
            case "BASE" -> baseWeb3jHttp;
            default -> throw new IllegalArgumentException("Unsupported blockchain: " + blockchain);
        };
    }

}