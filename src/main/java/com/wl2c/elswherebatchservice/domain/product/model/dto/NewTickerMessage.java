package com.wl2c.elswherebatchservice.domain.product.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@ToString
public class NewTickerMessage {

    @NotNull
    private String productName;

    @NotNull
    private String equity;

    @Builder
    private NewTickerMessage(String productName,
                             String equity) {
        this.productName = productName;
        this.equity = equity;
    }
}
