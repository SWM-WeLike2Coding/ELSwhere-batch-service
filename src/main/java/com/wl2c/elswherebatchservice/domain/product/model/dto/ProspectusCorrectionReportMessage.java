package com.wl2c.elswherebatchservice.domain.product.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@ToString
public class ProspectusCorrectionReportMessage {

    @NotNull
    private String productName;

    @NotNull
    private String prospectusLink;

    @Builder
    private ProspectusCorrectionReportMessage(String productName,
                                              String prospectusLink) {
        this.productName = productName;
        this.prospectusLink = prospectusLink;
    }
}
