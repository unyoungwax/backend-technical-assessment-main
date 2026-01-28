package com.aquariux.technical.assessment.trade.service.impl;

import com.aquariux.technical.assessment.trade.dto.internal.UserWalletDto;
import com.aquariux.technical.assessment.trade.dto.request.TradeRequest;
import com.aquariux.technical.assessment.trade.dto.response.TradeResponse;
import com.aquariux.technical.assessment.trade.entity.CryptoPair;
import com.aquariux.technical.assessment.trade.entity.CryptoPrice;
import com.aquariux.technical.assessment.trade.entity.Trade;
import com.aquariux.technical.assessment.trade.enums.TradeType;
import com.aquariux.technical.assessment.trade.exception.EntityNotFoundException;
import com.aquariux.technical.assessment.trade.exception.InsufficientFundsException;
import com.aquariux.technical.assessment.trade.mapper.CryptoPairMapper;
import com.aquariux.technical.assessment.trade.mapper.CryptoPriceMapper;
import com.aquariux.technical.assessment.trade.mapper.TradeMapper;
import com.aquariux.technical.assessment.trade.mapper.UserWalletMapper;
import com.aquariux.technical.assessment.trade.service.TradeServiceInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TradeServiceImpl implements TradeServiceInterface {

    private final TradeMapper tradeMapper;
    private final UserWalletMapper userWalletMapper;
    private final CryptoPairMapper cryptoPairMapper;
    private final CryptoPriceMapper cryptoPriceMapper;
    // Add additional beans here if needed for your implementation

    @Override
    @Transactional
    public TradeResponse executeTrade(TradeRequest tradeRequest) {
        // TODO: Implement the core trading engine
        // What should happen when a user executes a trade?

        CryptoPair cryptoPair = cryptoPairMapper.findByPairName(tradeRequest.getPairName());

        CryptoPrice latestPrice = cryptoPriceMapper.findLatestPrices()
                .stream()
                .filter((item) -> item.getCryptoPairId().equals(cryptoPair.getId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Crypto pair not found"));

        List<UserWalletDto> wallets = userWalletMapper.findByUserId(tradeRequest.getUserId());

        Optional<UserWalletDto> baseWallet = wallets.stream()
                .filter((item) -> item.getSymbolId().equals(cryptoPair.getBaseSymbolId()))
                .findFirst();

        Optional<UserWalletDto> quoteWallet = wallets.stream()
                .filter((item) -> item.getSymbolId().equals(cryptoPair.getQuoteSymbolId()))
                .findFirst();

        Trade trade;

        if (tradeRequest.getTradeType() == TradeType.BUY) {
            trade = handleBuy(tradeRequest, cryptoPair, latestPrice, baseWallet, quoteWallet);
        } else {
            trade = handleSell(tradeRequest, cryptoPair, latestPrice, baseWallet, quoteWallet);
        }

        return mapToResponse(tradeRequest, trade.getPrice());
    }

    private Trade handleBuy(
        TradeRequest tradeRequest,
        CryptoPair cryptoPair,
        CryptoPrice latestPrice,
        Optional<UserWalletDto> baseWallet,
        Optional<UserWalletDto> quoteWallet
    ) {
        BigDecimal tradePrice = latestPrice.getAskPrice();
        BigDecimal totalAmount = tradeRequest.getQuantity().multiply(tradePrice);

        BigDecimal quoteBalance = quoteWallet.map(UserWalletDto::getBalance).orElse(BigDecimal.ZERO);

        if (quoteBalance.compareTo(totalAmount) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }

        baseWallet.ifPresentOrElse((wallet) -> {
            wallet.setBalance(wallet.getBalance().add(tradeRequest.getQuantity()));
            wallet.setUpdatedAt(LocalDateTime.now());

            userWalletMapper.updateUserWallet(wallet);
        }, () -> {
            UserWalletDto userWalletDto = new UserWalletDto();

            userWalletDto.setUserId(tradeRequest.getUserId());
            userWalletDto.setSymbolId(cryptoPair.getBaseSymbolId());
            userWalletDto.setBalance(tradeRequest.getQuantity());

            userWalletMapper.insertUserWallet(userWalletDto);
        });

        quoteWallet.ifPresent((wallet) -> {
            wallet.setBalance(wallet.getBalance().subtract(totalAmount));
            wallet.setUpdatedAt(LocalDateTime.now());

            userWalletMapper.updateUserWallet(wallet);
        });

        Trade trade = new Trade();

        trade.setUserId(tradeRequest.getUserId());
        trade.setCryptoPairId(cryptoPair.getId());
        trade.setTradeType(String.valueOf(tradeRequest.getTradeType()));
        trade.setQuantity(tradeRequest.getQuantity());
        trade.setPrice(tradePrice);
        trade.setTotalAmount(totalAmount);

        tradeMapper.insertTrade(trade);

        return trade;
    }

    private Trade handleSell(
            TradeRequest tradeRequest,
            CryptoPair cryptoPair,
            CryptoPrice latestPrice,
            Optional<UserWalletDto> baseWallet,
            Optional<UserWalletDto> quoteWallet
    ) {
        BigDecimal tradePrice = latestPrice.getBidPrice();
        BigDecimal totalAmount = tradeRequest.getQuantity().multiply(tradePrice);

        BigDecimal baseBalance = baseWallet.map(UserWalletDto::getBalance).orElse(BigDecimal.ZERO);

        if (baseBalance.compareTo(tradeRequest.getQuantity()) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }

        baseWallet.ifPresent((wallet) -> {
            wallet.setBalance(wallet.getBalance().subtract(tradeRequest.getQuantity()));
            wallet.setUpdatedAt(LocalDateTime.now());

            userWalletMapper.updateUserWallet(wallet);
        });

        quoteWallet.ifPresentOrElse((wallet) -> {
            wallet.setBalance(wallet.getBalance().add(totalAmount));
            wallet.setUpdatedAt(LocalDateTime.now());

            userWalletMapper.updateUserWallet(wallet);
        }, () -> {
            UserWalletDto userWalletDto = new UserWalletDto();

            userWalletDto.setUserId(tradeRequest.getUserId());
            userWalletDto.setSymbolId(cryptoPair.getBaseSymbolId());
            userWalletDto.setBalance(totalAmount);

            userWalletMapper.insertUserWallet(userWalletDto);
        });

        Trade trade = new Trade();

        trade.setUserId(tradeRequest.getUserId());
        trade.setCryptoPairId(cryptoPair.getId());
        trade.setTradeType(String.valueOf(tradeRequest.getTradeType()));
        trade.setQuantity(tradeRequest.getQuantity());
        trade.setPrice(tradePrice);
        trade.setTotalAmount(totalAmount);

        tradeMapper.insertTrade(trade);

        return trade;
    }

    private TradeResponse mapToResponse(TradeRequest tradeRequest, BigDecimal tradePrice) {
        TradeResponse tradeResponse = new TradeResponse();

        tradeResponse.setUserId(tradeRequest.getUserId());
        tradeResponse.setTradeType(tradeRequest.getTradeType());
        tradeResponse.setPairName(tradeRequest.getPairName());
        tradeResponse.setQuantity(tradeRequest.getQuantity());
        tradeResponse.setPrice(tradePrice);
        return tradeResponse;
    }
}