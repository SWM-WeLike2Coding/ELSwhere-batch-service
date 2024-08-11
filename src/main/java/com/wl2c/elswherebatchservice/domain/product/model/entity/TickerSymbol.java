package com.wl2c.elswherebatchservice.domain.product.model.entity;

import com.wl2c.elswherebatchservice.global.base.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TickerSymbol extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticker_symbol_id")
    private Long id;

    @NotNull
    private String tickerSymbol;

    @NotNull
    private String equityName;

    @OneToMany(mappedBy = "tickerSymbol", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductTickerSymbol> productTickerSymbols = new ArrayList<>();

    @Builder
    private TickerSymbol (@NonNull String tickerSymbol,
                          @NonNull String equityName) {
        this.tickerSymbol = tickerSymbol;
        this.equityName = equityName;
    }
}
