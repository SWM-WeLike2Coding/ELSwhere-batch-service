package com.wl2c.elswherebatchservice.domain.product.model.entity;

import com.wl2c.elswherebatchservice.global.base.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductTickerSymbol extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_ticker_symbol_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_symbol_id")
    private TickerSymbol tickerSymbol;

    @NotNull
    private String equityName;

    @OneToOne(mappedBy = "productTickerSymbol")
    @PrimaryKeyJoinColumn
    private ProductEquityVolatility productEquityVolatility;

    @Builder
    public ProductTickerSymbol(Product product, TickerSymbol tickerSymbol) {
        this.product = product;
        this.tickerSymbol = tickerSymbol;
        this.equityName = tickerSymbol.getEquityName();
    }
}
