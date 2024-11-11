package com.wl2c.elswherebatchservice.domain.product.model;

/**
 * 기초자산 유형
 */
public enum UnderlyingAssetType {

    /**
     * 주가 지수
     */
    INDEX,

    /**
     * 종목
     */
    STOCK,

    /**
     * 혼합
     */
    MIX,

    /**
     * 확인 필요
     */
    NEED_TO_CHECK
}
