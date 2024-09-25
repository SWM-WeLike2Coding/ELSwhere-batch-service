package com.wl2c.elswherebatchservice.domain.product.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@ToString
public class NewIssuerMessage {

    @NotNull
    private String productName;

    @NotNull
    private String issuer;

    @Builder
    private NewIssuerMessage(String productName,
                             String issuer) {
        this.productName = productName;
        this.issuer = issuer;
    }
}
