package com.aquariux.technical.assessment.trade.controller;

import com.aquariux.technical.assessment.trade.dto.request.TradeRequest;
import com.aquariux.technical.assessment.trade.dto.response.TradeResponse;
import com.aquariux.technical.assessment.trade.exception.ValidationException;
import com.aquariux.technical.assessment.trade.service.TradeServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/trades")
@Tag(name = "Trade", description = "Trading operations")
@RequiredArgsConstructor
public class TradeController {

    private final TradeServiceInterface tradeService;
    // Add additional beans here if needed for your implementation

    @PostMapping(value = "/execute", produces = "application/json")
    @Operation(summary = "Execute trade", description = "Execute a buy or sell trade for cryptocurrency pairs")
    public ResponseEntity<TradeResponse> executeTrade(@RequestBody TradeRequest tradeRequest) {
        // TODO: How should a trading API endpoint behave?
        if (tradeRequest.getUserId() == null) {
            throw new ValidationException("userId is required");
        }

        if (tradeRequest.getTradeType() == null) {
            throw new ValidationException("tradeType is required");
        }

        if (tradeRequest.getPairName() == null) {
            throw new ValidationException("pairName is required");
        }

        if (tradeRequest.getQuantity() == null || tradeRequest.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("quantity should be positive");
        }

        return ResponseEntity.ok(tradeService.executeTrade(tradeRequest));
    }
}