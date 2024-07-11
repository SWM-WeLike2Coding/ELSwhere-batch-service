package com.wl2c.elswherebatchservice.domain.product.model.entity;

import com.wl2c.elswherebatchservice.global.base.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EarlyRepaymentEvaluationDates extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "early_repayment_evaluation_dates_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @NotNull
    private LocalDate earlyRepaymentEvaluationDate;

    @Builder
    public EarlyRepaymentEvaluationDates(Product product, LocalDate earlyRepaymentEvaluationDate) {
        this.product = product;
        this.earlyRepaymentEvaluationDate = earlyRepaymentEvaluationDate;
    }
}
