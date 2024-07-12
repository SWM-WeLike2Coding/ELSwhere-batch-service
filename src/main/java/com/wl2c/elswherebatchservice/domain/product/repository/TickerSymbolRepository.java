package com.wl2c.elswherebatchservice.domain.product.repository;

import com.wl2c.elswherebatchservice.domain.product.model.entity.TickerSymbol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TickerSymbolRepository extends JpaRepository<TickerSymbol, Long> {

    @Query("select t from TickerSymbol t where t.equityName = :equityName ")
    Optional<TickerSymbol> findTickerSymbolByEquityName(@Param("equityName") String equityName);

    @Query("select t from TickerSymbol t where t.equityName = :equityName and t.tickerSymbol = :tickerSymbol")
    Optional<TickerSymbol> findTickerSymbolByEquityNameAndTickerSymbol(@Param("equityName") String equityName,
                                                        @Param("tickerSymbol") String tickerSymbol);
}
