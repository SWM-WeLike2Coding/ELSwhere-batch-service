package com.wl2c.elswherebatchservice.domain.product.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wl2c.elswherebatchservice.domain.product.model.MaturityEvaluationDateType;
import com.wl2c.elswherebatchservice.domain.product.model.ProductState;
import com.wl2c.elswherebatchservice.domain.product.model.ProductType;
import com.wl2c.elswherebatchservice.domain.product.model.dto.NewTickerMessage;
import com.wl2c.elswherebatchservice.domain.product.model.entity.*;
import com.wl2c.elswherebatchservice.domain.product.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParsingExcelService {

    @Value("${file.excel.path}")
    private String fileDownloadPath;

    @Value("${file.krx.path}")
    private String krxPath;

    private final ProductRepository productRepository;
    private final TickerSymbolRepository tickerSymbolRepository;
    private final ProductTickerSymbolRepository productTickerSymbolRepository;
    private final ProductEquityVolatilityRepository productEquityVolatilityRepository;
    private final EarlyRepaymentEvaluationDatesRepository earlyRepaymentEvaluationDatesRepository;

    private final ParsingProspectusService parsingProspectusService;
    private final NewTickerMessageSender newTickerMessageSender;

    // 발행사 리스트
    private final List<String> issuers = List.of(
            "신한", "KB", "kb", "한화", "삼성", "미래에셋", "유안타", "키움",
                "교보", "NH", "SK", "대신", "메리츠", "하나",
                "현대차", "한국투자", "트루", "대신", "신영", "유진",
                "하이투자", "비엔케이", "BNK", "IBK", "아이비케이", "DB"
            );

    // No-Knock-In 키워드 리스트
    private final List<String> noKiKeywords = List.of(
            "노낙인", "noKI", "NoKI",
            "NOKI", "NO_KI", "KI 없음",
            "no ki", "NO KI", "NO  KI", "--no knock in"
    );

    // Knock-In 패턴 정규 표현식 리스트
    private final List<String> kiPatterns = List.of(
            "KI(\\d+)", "(\\d+)KI", "KI (\\d+)", "KI (\\d+)%", "KI_(\\d+)", "--knock in (\\d+)"
    );

    // Knock-In 패턴 추가적인 정규 표현식
    String additionalKiPatterns = ".*(\\d+)/\\s*(\\d+).*|,\\s*(\\d+)%-.+";

    @Transactional
    public void parsingExcel() throws IOException, InvalidFormatException {

        File file = new File(fileDownloadPath);

        if (!file.exists()) {
            log.error("File does not exist");
            return;
        }

        try (InputStream inputStream = new FileInputStream(file)) {
            Workbook workbook = null;
            Row row = null;

            // 엑셀 97 ~ 2003 까지는 HSSF(xls), 엑셀 2007 이상은 XSSF(xlsx)
            if (fileDownloadPath.endsWith(".xls")) {
                workbook = new HSSFWorkbook(inputStream);
            } else if (fileDownloadPath.endsWith(".xlsx")) {
                try (OPCPackage opcPackage = OPCPackage.open(inputStream)) {
                    workbook = new XSSFWorkbook(opcPackage);
                }
            } else {
                log.error("Unsupported Excel file format");
            }

            // 엑셀 파일에서 첫 번째 시트 불러오기
            Sheet sheet = workbook.getSheetAt(0);

            int rows = sheet.getPhysicalNumberOfRows();
            log.info("엑셀 행 개수 " + rows);

            int cntForSleep = 1;
            for (int r = 1; r < rows; r++) {
                if (cntForSleep % 10 == 0) {
                    try {
                        Thread.sleep(5000); // 5초 일시 중지
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("Thread interrupted: ", e);
                    }
                }

                row = sheet.getRow(r);

                Cell bCell = row.getCell(1);    // 발행 회사
                Cell cCell = row.getCell(2);    // 신용등급
                Cell dCell = row.getCell(3);    // 상품명
                Cell eCell = row.getCell(4);    // 기초자산
                Cell fCell = row.getCell(5);    // 발행일
                Cell gCell = row.getCell(6);    // 만기일
                Cell hCell = row.getCell(7);    // 조건 충족 시 수익률(연, %)
                Cell iCell = row.getCell(8);    // 최대손실률(%)
                Cell jCell = row.getCell(9);    // 청약시작일
                Cell kCell = row.getCell(10);   // 청약종료일
                Cell lCell = row.getCell(11);   // 상품유형
                Cell nCell = row.getCell(13);   // 홈페이지
                Cell pCell = row.getCell(15);   // 비고

                log.info(dCell.getStringCellValue());
                log.info(r+1 + " - 낙인:" + findKnockIn(lCell.getStringCellValue()));
                log.info(r+1 + " - 유형:" + findProductType(bCell.getStringCellValue(), lCell.getStringCellValue()));
                log.info(r+1 + " - 유형2:" + findProductInfo(lCell.getStringCellValue()));

                // 이미 존재하면 패스
                if (productRepository.findProductByName(dCell.getStringCellValue()).isPresent())    continue;

                if (findProspectusLink(findProductSession(dCell.getStringCellValue()), findIssuer(dCell.getStringCellValue())) != null) {

                    Document doc = parsingProspectusService.fetchDocument(findProspectusLink(findProductSession(dCell.getStringCellValue()), findIssuer(dCell.getStringCellValue())));

                    parsingProspectusService.findVolatilitiesList(doc);
                    log.info(r+1 + " - 변동성:" + parsingProspectusService.findVolatilities(findProductSession(dCell.getStringCellValue()),doc));
                    Product product = Product.builder()
                            .issuer(bCell.getStringCellValue())
                            .name(dCell.getStringCellValue())
                            .equities(eCell.getStringCellValue().replace("<br/>", " / "))
                            .equityCount(eCell.getStringCellValue().split("<br/>").length)
                            .issuedDate(convertToLocalDate(fCell.getStringCellValue()))
                            .maturityEvaluationDate(parsingProspectusService.findMaturityEvaluationDate(bCell.getStringCellValue(), findProductSession(dCell.getStringCellValue()), doc))
                            .maturityEvaluationDateType(parsingProspectusService.findMaturityEvaluationDateCount(bCell.getStringCellValue(), findProductSession(dCell.getStringCellValue()), doc))
                            .maturityDate(convertToLocalDate(gCell.getStringCellValue()))
                            .yieldIfConditionsMet(BigDecimal.valueOf(hCell.getNumericCellValue()))
                            .maximumLossRate(BigDecimal.valueOf(iCell.getNumericCellValue()))
                            .subscriptionStartDate(convertToLocalDate(jCell.getStringCellValue()))
                            .subscriptionEndDate(convertToLocalDate(kCell.getStringCellValue()))
                            .productFullInfo(lCell.getStringCellValue())
                            .productInfo(findProductInfo(lCell.getStringCellValue()))
                            .link(nCell.getStringCellValue())
                            .remarks(pCell.getStringCellValue())
                            .knockIn(findKnockIn(lCell.getStringCellValue()))
                            .summaryInvestmentProspectusLink(findProspectusLink(findProductSession(dCell.getStringCellValue()), findIssuer(dCell.getStringCellValue())))
                            .earlyRepaymentEvaluationDates(Optional.ofNullable(
                                            parsingProspectusService.findEarlyRepaymentEvaluationDates(
                                                    findProductSession(dCell.getStringCellValue()),
                                                    doc
                                            )
                                    ).map(dates -> String.join(", ", dates))
                                    .orElse(null)
                            )
                            .volatilites(parsingProspectusService.findVolatilities(findProductSession(dCell.getStringCellValue()), doc).get(0))
                            .initialBasePriceEvaluationDate(parsingProspectusService.findInitialBasePriceEvaluationDate(bCell.getStringCellValue(), findProductSession(dCell.getStringCellValue()), doc))
                            .productType(findProductType(bCell.getStringCellValue(), lCell.getStringCellValue()))
                            .productState(ProductState.ACTIVE)
                            .build();
                    productRepository.save(product);

                    // 기초자산 db
                    String[] equities = eCell.getStringCellValue().split("<br/>");
                    String volatilites = parsingProspectusService.findVolatilities(findProductSession(dCell.getStringCellValue()), doc).get(0);
                    for (String equity : equities) {
                        Optional<TickerSymbol> tickerSymbol = tickerSymbolRepository.findTickerSymbolByEquityName(equity);
                        ProductTickerSymbol productTickerSymbol;

                        if (tickerSymbol.isPresent() && !Objects.equals(tickerSymbol.get().getTickerSymbol(), "NEED_TO_CHECK")) {
                            productTickerSymbol = ProductTickerSymbol.builder()
                                    .product(product)
                                    .tickerSymbol(tickerSymbol.get())
                                    .build();
                            productTickerSymbolRepository.save(productTickerSymbol);
                        } else {
                            log.warn(dCell + " : " + "기초자산 " +equity + " 에 대해서 Ticker가 존재하지 않음 업데이트 필요");
                            NewTickerMessage newTickerMessage = NewTickerMessage.builder()
                                    .productId(product.getId())
                                    .productName(dCell.getStringCellValue())
                                    .equity(equity)
                                    .build();
                            newTickerMessageSender.send("new-ticker-alert", newTickerMessage);

                            Optional<TickerSymbol> checkTickerSymbol = tickerSymbolRepository.findTickerSymbolByEquityNameAndTickerSymbol(equity, "NEED_TO_CHECK");
                            if (checkTickerSymbol.isPresent())    continue;

                            TickerSymbol temporaryTickerSymbol = TickerSymbol.builder()
                                    .tickerSymbol("NEED_TO_CHECK")
                                    .equityName(equity)
                                    .build();
                            tickerSymbolRepository.save(temporaryTickerSymbol);

                            productTickerSymbol = ProductTickerSymbol.builder()
                                    .product(product)
                                    .tickerSymbol(temporaryTickerSymbol)
                                    .build();
                            productTickerSymbolRepository.save(productTickerSymbol);

                            product.setInActiveProductState();
                        }

                        if (volatilites != null) {
                            for (String volatility : volatilites.split(" / ")) {

                                String[] subParts = volatility.split(" : "); // subParts[0] : 기초자산명, subParts[1] : 변동성(%)

                                log.info(subParts[0]);
                                if (subParts[0].startsWith("[") && subParts[0].endsWith("]")) {
                                    subParts[0]= subParts[0].substring(1, subParts[0].length() - 1);
                                }
                                String percentagePart = subParts[1].replace("%", "");

                                Optional<TickerSymbol> prospectusTickerSymbol = tickerSymbolRepository.findTickerSymbolByEquityName(subParts[0]);
                                if (prospectusTickerSymbol.isPresent() && tickerSymbol.isPresent()) {
                                    if (prospectusTickerSymbol.get().getTickerSymbol().equals(tickerSymbol.get().getTickerSymbol())) {
                                        ProductEquityVolatility productEquityVolatility = ProductEquityVolatility.builder()
                                                .productTickerSymbol(productTickerSymbol)
                                                .volatility(new BigDecimal(percentagePart))
                                                .build();
                                        productEquityVolatilityRepository.save(productEquityVolatility);
                                    }
                                }
                            }
                        }
                    }

                    // 조기상환일 db
                    List<String> earlyRepaymentEvaluationDateList = parsingProspectusService.findEarlyRepaymentEvaluationDates(findProductSession(dCell.getStringCellValue()),doc);
                    if (earlyRepaymentEvaluationDateList != null) {
                        for (String earlyRepaymentEvaluationDateStr : earlyRepaymentEvaluationDateList) {
                            EarlyRepaymentEvaluationDates earlyRepaymentEvaluationDates = EarlyRepaymentEvaluationDates.builder()
                                    .product(product)
                                    .earlyRepaymentEvaluationDate(convertToLocalDateFromKoreanFormat(earlyRepaymentEvaluationDateStr.split(": ")[1]))
                                    .build();
                            earlyRepaymentEvaluationDatesRepository.save(earlyRepaymentEvaluationDates);
                        }
                    }
                } else {
                    Product product = Product.builder()
                            .issuer(bCell.getStringCellValue())
                            .name(dCell.getStringCellValue())
                            .equities(eCell.getStringCellValue().replace("<br/>", " / "))
                            .equityCount(eCell.getStringCellValue().split("<br/>").length)
                            .issuedDate(convertToLocalDate(fCell.getStringCellValue()))
                            .maturityEvaluationDateType(MaturityEvaluationDateType.UNKNOWN)
                            .maturityDate(convertToLocalDate(gCell.getStringCellValue()))
                            .yieldIfConditionsMet(BigDecimal.valueOf(hCell.getNumericCellValue()))
                            .maximumLossRate(BigDecimal.valueOf(iCell.getNumericCellValue()))
                            .subscriptionStartDate(convertToLocalDate(jCell.getStringCellValue()))
                            .subscriptionEndDate(convertToLocalDate(kCell.getStringCellValue()))
                            .productFullInfo(lCell.getStringCellValue())
                            .productInfo(findProductInfo(lCell.getStringCellValue()))
                            .link(nCell.getStringCellValue())
                            .remarks(pCell.getStringCellValue())
                            .productType(findProductType(bCell.getStringCellValue(), lCell.getStringCellValue()))
                            .productState(ProductState.INACTIVE)
                            .build();
                    productRepository.save(product);

                    // 기초자산 db
                    String[] equities = eCell.getStringCellValue().split("<br/>");
                    for (String equity : equities) {
                        Optional<TickerSymbol> tickerSymbol = tickerSymbolRepository.findTickerSymbolByEquityName(equity);
                        if (tickerSymbol.isEmpty()) {
                            log.warn(dCell + " : " + "기초자산 " +equity + " 에 대해서 Ticker가 존재하지 않음 업데이트 필요");
                            NewTickerMessage newTickerMessage = NewTickerMessage.builder()
                                    .productId(product.getId())
                                    .productName(dCell.getStringCellValue())
                                    .equity(equity)
                                    .build();
                            newTickerMessageSender.send("new-ticker-alert", newTickerMessage);

                            Optional<TickerSymbol> checkTickerSymbol = tickerSymbolRepository.findTickerSymbolByEquityNameAndTickerSymbol(equity, "NEED_TO_CHECK");
                            if (checkTickerSymbol.isPresent())    continue;

                            TickerSymbol temporaryTickerSymbol = TickerSymbol.builder()
                                    .tickerSymbol("NEED_TO_CHECK")
                                    .equityName(equity)
                                    .build();
                            tickerSymbolRepository.save(temporaryTickerSymbol);
                        }
                    }
                }
                cntForSleep++;
            }

        } catch (IOException | InvalidFormatException e) {
            log.error("Error processing Excel file: ", e);
        }

    }

    private String findProductSession(String name) {

        String number = null;

        /**
         * 정규식 패턴을 정의
         *
         * 숫자 앞에 공백 문자, "제", "회", "호" 또는 문자열의 시작이 있어야 하고,
         * 숫자 뒤에 공백 문자, "제", "회", "호" 또는 문자열의 끝이 있어야 함
         */
        String regex = "(?<=\\s|제|회|호|^)\\d+(?=\\s|제|회|호|$)";

        // 정규식 패턴을 컴파일
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(name);

        // 패턴에 매칭되는 회차(숫자)를 찾기
        while (matcher.find()) {
            number = matcher.group();
        }
        return number;
    }

    private String findIssuer(String name) {

        // 단어가 포함되어 있는지 확인하고, 해당 단어를 반환
        Optional<String> result = issuers.stream()
                .filter(name::contains)
                .findFirst();

        return result.orElseThrow();

    }

    private String findProductInfo(String str) {

        // 패턴 정의
        // (?:\.\d+)?는 소수점 반영을 위한 패턴
        String pattern1 = "\\b\\d{2,3}(?:\\.\\d+)?(?:.*?-\\d{2,3}(?:\\.\\d+)?)+\\b"; // 90-90-85-85-80-75 or 90-90(40m)-85-85-80-75 or 90(~60)-90-85-85-80-75와 같은 패턴
        String pattern2 = "\\b\\d{2,3}(?:\\.\\d+)?(?:.*?/\\d{2,3}(?:\\.\\d+)?)+\\b"; // 90(L85)/90(L80)/85/80/75/70 패턴
        String pattern3 = "\\b\\d{1,3}(?:\\.\\d+)?(?:,\\d{1,3}(?:\\.\\d+)?)+\\b"; // 92.5,90,90,85,80,75 or 85,85,80,80,75,75 패턴
        // 정규식 컴파일
        Pattern regex1 = Pattern.compile(pattern1);
        Pattern regex2 = Pattern.compile(pattern2);
        Pattern regex3 = Pattern.compile(pattern3);

        Matcher matcher1 = regex1.matcher(str);
        Matcher matcher2 = regex2.matcher(str);
        Matcher matcher3 = regex3.matcher(str);

        if (matcher1.find()) {
            String matched = matcher1.group();

            int lastBrIndex = matched.lastIndexOf("<br/>");

            if (lastBrIndex != -1) {
                return matched.substring(lastBrIndex + 5);
            } else {

                int lastSlashIndex = matched.lastIndexOf("/");

                if (lastSlashIndex != -1) {
                    return matched.substring(lastSlashIndex + 1);
                } else {

                    if (matched.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*")) {
                        String regex = "\\b\\d+(?:-\\d+)+\\b";

                        Pattern pattern = Pattern.compile(regex);
                        Matcher matcher = pattern.matcher(matched);

                        while (matcher.find()) {
                            return matcher.group();
                        }

                    } else {
                        return matched;
                    }
                }
            }
        } else if (matcher2.find()) {

            // "/"를 "-"로 변환
            String matched = matcher2.group();
            return matched.replace("/", "-");

        } else if (matcher3.find()) {

            // ","를 "-"로 변환
            String matched = matcher3.group();
            return matched.replace(",", "-");

        }

        return null;
    }

    private Integer findKnockIn(String str) {
        for (String keyword : noKiKeywords) {
            if (str.contains(keyword)) {
                return null;
            }
        }

        for (String pattern : kiPatterns) {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(str);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        }

        Pattern additionalPattern = Pattern.compile(additionalKiPatterns);
        Matcher additionalMatcher = additionalPattern.matcher(str);
        if (additionalMatcher.find()) {
            if (additionalMatcher.group(2) != null) {
                return Integer.parseInt(additionalMatcher.group(2));
            } else if (additionalMatcher.group(3) != null) {
                return Integer.parseInt(additionalMatcher.group(3));
            }
        }

        return null;
    }

    private ProductType findProductType(String issuer, String str) {
        return switch (issuer) {
            case "NH투자증권" -> findProductTypeForNH(str);
            case "미래에셋증권" -> findProductTypeForMiraeAsset(str);
            case "하나증권" -> findProductTypeForHana(str);
            default -> findProductTypeDefault(str);
        };
    }

    private ProductType findProductTypeForNH(String str) {
        if (str.contains("쿠폰베리어")) {
            return ProductType.MONTHLY_PAYMENT;
        }
        if (str.contains("(") || str.contains(")")) {
            return ProductType.ETC;
        }

        String[] patterns = {
                "(\\d+-)+\\d+/\\d+KI",
                "(\\d+-)+\\d+/NOKI",
                "(\\d+-)+\\d+/noKI"
        };
        for (String pattern : patterns) {
            if (Pattern.compile(pattern).matcher(str).find()) {
                return ProductType.STEP_DOWN;
            }
        }

        return ProductType.ETC;
    }

    private ProductType findProductTypeForMiraeAsset(String str) {
        if (str.contains("월지급")) {
            return ProductType.MONTHLY_PAYMENT;
        }
        if (str.contains("리자드")) {
            return ProductType.LIZARD;
        }

        String[] patterns = {
                "(\\d+-)+\\d+, KI\\d+",
                "(\\d+-)+\\d+, noKI"
        };
        for (String pattern : patterns) {
            if (Pattern.compile(pattern).matcher(str).find()) {
                return ProductType.STEP_DOWN;
            }
        }

        return ProductType.ETC;
    }

    private ProductType findProductTypeForHana(String str) {
        if (str.contains("월지급")) {
            return ProductType.MONTHLY_PAYMENT;
        }

        String pattern = "\\d+y/\\d+m (\\d+-)+\\d+";
        if (Pattern.compile(pattern).matcher(str).find()) {
            return ProductType.STEP_DOWN;
        }

        return ProductType.ETC;
    }

    private ProductType findProductTypeDefault(String str) {
        String[] stepDownPatterns = {"Step-Down", "Step Down", "StepDown", "Stepdown", "stepdown", "스텝다운"};
        String[] lizardPatterns = {"리자드", "Lizard", "LizardStepDown"};

        if (str.contains("월지급")) {
            return ProductType.MONTHLY_PAYMENT;
        }

        for (String pattern : lizardPatterns) {
            if (str.contains(pattern)) {
                return ProductType.LIZARD;
            }
        }

        for (String pattern : stepDownPatterns) {
            int index = str.indexOf(pattern);
            while (index != -1) {
                if (index == 0 || !Character.isLetter(str.charAt(index - 1))) {
                    return ProductType.STEP_DOWN;
                }
                index = str.indexOf(pattern, index + 1);
            }
        }

        String[] patterns = {
                "(\\d+-)+\\d+ /?\\s*KI(_\\d+)?",
                "(\\d+-)+\\d+ / NO KI",
                "(\\d+-)+\\d+ / KI \\d+",
                "(\\d+-)+\\d+/ KI \\d+",
                "(\\d+-)+\\d+/ KI 없음",
                "(\\d+-)+\\d+, NO_KI",
                "(\\d+-)+\\d+, KI_\\d+"
        };
        for (String pattern : patterns) {
            if (Pattern.compile(pattern).matcher(str).find() && (!str.contains("Ultra") || !str.contains("Safezone") || !str.contains("Power"))) {
                return ProductType.STEP_DOWN;
            }
        }

        return ProductType.ETC;
    }

    private String findProspectusLink(String session, String issuer) {

        String prospectusLink = null;

        if (session != null) {
            // JSON 파일을 읽고 처리하기 위한 ObjectMapper 생성
            ObjectMapper objectMapper = new ObjectMapper();

            try {
                // JSON 파일을 JsonNode 배열로 변환
                JsonNode jsonArray = objectMapper.readTree(new File(krxPath));

                // "ISU_NM" 키가 session 문자열을 포함하고 issuer 문자열도 포함하는 항목을 찾기
                for (JsonNode jsonNode : jsonArray) {
                    String isuNm = jsonNode.get("ISU_NM").asText();

                    // issuer가 포함되어 있고 session이 단어 단위로 존재하는지 확인하는 정규식
                    String regex = "\\b" + session + "\\b";

                    if (isuNm.contains(issuer) && isuNm.matches(".*" + regex + ".*")) {
                        prospectusLink = jsonNode.get("ISU_DISCLS_URL").asText();
                        break;
                    }
                }

            } catch (IOException e) {
                log.error("Error processing json file: ", e);
            }
        }

        return prospectusLink;
    }

    private LocalDate convertToLocalDate(String dateString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        return LocalDate.parse(dateString, formatter);
    }

    private LocalDate convertToLocalDateFromKoreanFormat(String dateString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");
        return LocalDate.parse(dateString, formatter);
    }
}
