package com.wl2c.elswherebatchservice.domain.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Component
@RequiredArgsConstructor
@EnableScheduling
@Slf4j
public class DownloadExcelService {

    @Value("${kofia.api-path}")
    private String url;

    @Value("${chrome.web-driver.path}")
    private String driverPath;

    @Value("${folder.path}")
    private String fileDownloadPath;

    public void crawlProduct() {

        // Chrome 브라우저 설정
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", fileDownloadPath);

        // Chrome 웹 드라이버 설정
        System.setProperty("webdriver.chrome.driver", driverPath);
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless"); // 브라우저 창을 숨기고 실행
        options.setExperimentalOption("prefs", prefs); // 파일 저장 경로 설정

        // linux 환경에서 필요한 option
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        // 대기 시간을 Duration 객체로 변환
        Duration timeout = Duration.ofSeconds(10); // 10초로 설정

        // WebDriver 객체 생성
        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, timeout); // 최대 대기 시간 설정

        try {
            // 웹 페이지로 이동
            driver.get(url);

            // ELS 버튼 클릭 가능할 때까지 대기 후, 클릭
            WebElement elsButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("tab_btn2")));
            elsButton.click();

            TimeUnit.SECONDS.sleep(3);

            // 다운로드 버튼이 클릭 가능할 때까지 대기 후, 클릭
            WebElement downloadButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("excelDownLoadGrp")));
            downloadButton.click();

            // 파일 다운로드 후 다운로드 완료될 때까지 대기
            TimeUnit.SECONDS.sleep(3);

            // 성공 확인
            log.info("다운로드 완료");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // WebDriver 종료
            driver.quit();
        }
    }
}
