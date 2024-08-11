package com.wl2c.elswherebatchservice.domain.batch;

import com.wl2c.elswherebatchservice.domain.product.service.DownloadExcelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@EnableScheduling
@Slf4j
public class DownloadExcelScheduler {

    private final DownloadExcelService downloadExcelService;

    @Scheduled(cron = "${scheduler.download-excel.cron}")
    public void downloadExcel() {
        downloadExcelService.crawlProduct();
    }
}
