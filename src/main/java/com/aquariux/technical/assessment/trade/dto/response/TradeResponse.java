package com.aquariux.technical.assessment.trade.dto.response;

import com.aquariux.technical.assessment.trade.enums.TradeType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TradeResponse {
    private Long userId;
    private TradeType tradeType;
    private String pairName;
    private BigDecimal quantity;
    private BigDecimal price;

    // TODO: What should you return after a trade is executed?
}