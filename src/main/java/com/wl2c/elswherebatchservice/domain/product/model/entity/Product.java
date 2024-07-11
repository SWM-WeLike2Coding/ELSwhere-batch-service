package com.wl2c.elswherebatchservice.domain.product.model.entity;

import com.wl2c.elswherebatchservice.domain.product.model.ProductState;
import com.wl2c.elswherebatchservice.domain.product.model.ProductType;
import com.wl2c.elswherebatchservice.global.base.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.EnumType.STRING;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @NotNull
    private String publisher;

    @NotNull
    private String name;

    @NotNull
    private String equities;

    private int equityCount;

    private Integer knockIn;

    @NotNull
    private LocalDate issuedDate;

    @NotNull
    private LocalDate maturityDate;

    @NotNull
    @Column(precision = 8, scale = 5)
    private BigDecimal yieldIfConditionsMet;

    @NotNull
    @Column(precision = 8, scale = 5)
    private BigDecimal maximumLossRate;

    @NotNull
    private LocalDate subscriptionStartDate;

    @NotNull
    private LocalDate subscriptionEndDate;

    @NotNull
    @Enumerated(STRING)
    private ProductType type;

    @NotNull
    private String productFullInfo;

    private String productInfo;

    @NotNull
    private String link;

    @NotNull
    private String remarks;

    private String summaryInvestmentProspectusLink;

    private String earlyRepaymentEvaluationDates;

    private String volatilites;

    private LocalDate initialBasePriceEvaluationDate;

    @NotNull
    @ColumnDefault("'INACTIVE'")
    @Enumerated(STRING)
    private ProductState productState;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductTickerSymbol> productTickerSymbols = new ArrayList<>();

    @Builder
    private Product (@NonNull String publisher,
                     @NonNull String name,
                     @NonNull String equities,
                     int equityCount,
                     @NonNull LocalDate issuedDate,
                     @NonNull LocalDate maturityDate,
                     @NonNull BigDecimal yieldIfConditionsMet,
                     @NonNull BigDecimal maximumLossRate,
                     @NonNull LocalDate subscriptionStartDate,
                     @NonNull LocalDate subscriptionEndDate,
                     @NonNull String productFullInfo,
                     String productInfo,
                     @NonNull String link,
                     @NonNull String remarks,
                     Integer knockIn,
                     String summaryInvestmentProspectusLink,
                     String earlyRepaymentEvaluationDates,
                     String volatilites,
                     LocalDate initialBasePriceEvaluationDate,
                     ProductType productType,
                     ProductState productState) {
        this.publisher = publisher;
        this.name = name;
        this.equities = equities;
        this.equityCount = equityCount;
        this.knockIn = knockIn;
        this.issuedDate = issuedDate;
        this.maturityDate = maturityDate;
        this.yieldIfConditionsMet = yieldIfConditionsMet;
        this.maximumLossRate = maximumLossRate;
        this.subscriptionStartDate = subscriptionStartDate;
        this.subscriptionEndDate = subscriptionEndDate;
        this.type = productType;
        this.productFullInfo = productFullInfo;
        this.productInfo = productInfo;
        this.link = link;
        this.remarks = remarks;
        this.summaryInvestmentProspectusLink = summaryInvestmentProspectusLink;
        this.earlyRepaymentEvaluationDates = earlyRepaymentEvaluationDates;
        this.volatilites = volatilites;
        this.initialBasePriceEvaluationDate = initialBasePriceEvaluationDate;
        this.productState = productState;
    }

    public void changeProductState() {
        if (productState.equals(ProductState.ACTIVE)) {
            this.productState = ProductState.INACTIVE;
        } else {
            this.productState = ProductState.ACTIVE;
        }
    }
}
