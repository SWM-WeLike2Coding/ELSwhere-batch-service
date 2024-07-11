package com.wl2c.elswherebatchservice.domain.product.model.entity;

import com.wl2c.elswherebatchservice.global.base.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductEquityVolatility extends BaseEntity {

    @Id
    @Column(name = "product_equity_volatility_id", nullable = false)
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "product_equity_volatility_id", referencedColumnName = "product_equity_volatility_id")
    private ProductTickerSymbol productTickerSymbol;

    @NotNull
    @Column(scale = 16)
    private BigDecimal volatility;

    @Builder
    public ProductEquityVolatility(ProductTickerSymbol productTickerSymbol, BigDecimal volatility) {
        this.productTickerSymbol = productTickerSymbol;
        this.volatility = volatility;
    }
}
