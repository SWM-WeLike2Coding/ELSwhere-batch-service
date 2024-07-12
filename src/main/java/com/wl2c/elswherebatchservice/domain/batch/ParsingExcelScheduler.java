package com.wl2c.elswherebatchservice.domain.batch;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import com.wl2c.elswherebatchservice.domain.product.service.ParsingExcelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@EnableScheduling
@Slf4j
public class ParsingExcelScheduler {

    private final ParsingExcelService parsingExcelService;

    @Scheduled(cron = "${scheduler.parsing-excel.cron}")
    public void parsingExcel() throws IOException, InvalidFormatException {
        parsingExcelService.parsingExcel();
    }
}
