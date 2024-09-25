package com.wl2c.elswherebatchservice.domain.product.service;

import com.wl2c.elswherebatchservice.domain.product.model.MaturityEvaluationDateType;
import com.wl2c.elswherebatchservice.domain.product.model.dto.ProspectusCorrectionReportMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParsingProspectusService {

    private final ProspectusCorrectionReportMessageSender prospectusCorrectionReportMessageSender;

    public Document fetchDocument(String url) throws IOException {

        int retries = 3;
        while (retries > 0) {
            try {
                return Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.1 Safari/605.1.15")
                        .timeout(10000)
                        .maxBodySize(0)
                        .get();
            } catch (SocketTimeoutException e) {
                retries--;
                if (retries == 0) {
                    throw e;
                }
                log.info("Retrying... attempts left: " + retries);
            }
        }

        return null;
    }

    // 정정신고
    public void findIsCorrectionReport(String name, String prospectusLink, Document document) throws IOException {
        Elements pTags = document.select("p");

        // p tag
        for (Element pTag : pTags) {
            if (pTag.text().contains("정 정 신 고") || pTag.text().contains("정 정 보 고") || pTag.text().contains("정정사항") || pTag.text().contains("정정대상")) {
                ProspectusCorrectionReportMessage prospectusCorrectionReportMessage = ProspectusCorrectionReportMessage.builder()
                        .productName(name)
                        .prospectusLink(prospectusLink)
                        .build();

                prospectusCorrectionReportMessageSender.send("prospectus-correction-report-alert", prospectusCorrectionReportMessage);
                log.warn("상품명 " + name + " : 투자설명서 정정신고된 상품");
                break;
            }
        }

    }

    // 최초기준가격평가일(최초기준가격 결정일)
    public LocalDate findInitialBasePriceEvaluationDate(String issuer, String targetProductSession, Document document) throws IOException {
        if (targetProductSession != null && document != null) {

            // 투자 설명서에서 해당 회차 상품이 몇 번째인지
            int number = findLocationOfProduct(targetProductSession, document);

            // 투자 설명서에서 모든 최초기준가격평가일 파싱
            List<String> initialBasePriceEvaluationDateList = findInitialBasePriceEvaluationDateList(issuer, document);

            if (initialBasePriceEvaluationDateList == null || initialBasePriceEvaluationDateList.isEmpty()) {
                return null;
            }

            // 해당 회차 상품의 최초기준가격평가일
            return convertToLocalDate(initialBasePriceEvaluationDateList.get(number - 1));

        } else {
            return null;
        }
    }

    // 만기평가일(만기상환평가일)
    public LocalDate findMaturityEvaluationDate(String issuer, String targetProductSession, Document document) throws IOException {
        if (targetProductSession != null && document != null) {

            // 투자 설명서에서 해당 회차 상품이 몇 번째인지
            int number = findLocationOfProduct(targetProductSession, document);

            // 투자 설명서에서 모든 만기평가일 파싱
            List<String> MaturityEvaluationDateList = findMaturityEvaluationDateList(issuer, document);

            if (MaturityEvaluationDateList == null || MaturityEvaluationDateList.isEmpty() || number < 0) {
                return null;
            }

            // 해당 회차 상품의 만기평가일
            return convertToLocalDate(MaturityEvaluationDateList.get(number - 1));

        } else {
            return null;
        }
    }

    // 만기평가일(만기상환평가일) 개수
    public MaturityEvaluationDateType findMaturityEvaluationDateCount(String issuer, String targetProductSession, Document document) throws IOException {
        if (targetProductSession != null && document != null) {

            // 투자 설명서에서 해당 회차 상품이 몇 번째인지
            int number = findLocationOfProduct(targetProductSession, document);

            // 투자 설명서에서 모든 만기평가일 파싱
            List<MaturityEvaluationDateType> MaturityEvaluationDateCountList = findMaturityEvaluationDateCountList(issuer, document);

            if (MaturityEvaluationDateCountList == null || MaturityEvaluationDateCountList.isEmpty() || number < 0) {
                return MaturityEvaluationDateType.UNKNOWN;
            }

            // 해당 회차 상품의 만기평가일 개수 확인
            return MaturityEvaluationDateCountList.get(number - 1);
        } else {
            return MaturityEvaluationDateType.UNKNOWN;
        }
    }

    // 기초자산가격 변동성
    public List<String> findVolatilities(String targetProductSession, Document document) throws IOException {
        if (targetProductSession != null && document != null) {

            // 투자 설명서에서 해당 회차 상품이 몇 번째인지
            int number = findLocationOfProduct(targetProductSession, document);

            // 투자 설명서에서 모든 기초자산가격 파싱
            List<List<String>> volatilitiesList = findVolatilitiesList(document);

            // 해당 회차 상품의 기초자산가격 변동성
            if (volatilitiesList.size() >= number && number > 0) {
                return volatilitiesList.get(number - 1);
            } else {
                return null;
            }

        } else {
            return null;
        }
    }

    // 자동조기상환평가일
    public List<String> findEarlyRepaymentEvaluationDates(String targetProductSession, Document document) throws IOException {
        if (targetProductSession != null && document != null) {

            // 투자 설명서에서 해당 회차 상품이 몇 번째인지
            int number = findLocationOfProduct(targetProductSession, document);

            // 투자 설명서에서 모든 자동조기상환평가일 파싱
            List<List<String>> earlyRepaymentEvaluationDateList = findEarlyRepaymentEvaluationDatesList(document);

            // 해당 회차 상품의 자동조기상환평가일
            if (earlyRepaymentEvaluationDateList.size() >= number && number > 0) {
                return earlyRepaymentEvaluationDateList.get(number - 1);
            } else {
                return null;
            }

        } else {
            return null;
        }
    }

    private List<List<String>> findEarlyRepaymentEvaluationDatesList(Document doc) {

        List<List<String>> result = new ArrayList<>();

        List<String> keywords = List.of("자동조기상환평가일");
        List<String> optionalKeywords = List.of("차수", "차 수      ", "차 수", "상환금액", "상환금액(USD)(세전)", "상환금액(세전)");
        String midKeyword = "중간기준가격 결정일";

        Elements tables = doc.select("table");

        for (Element table : tables) {
            List<List<String>> tableData = new ArrayList<>();
            Elements rows = table.select("tr");

            for (Element row : rows) {
                List<String> rowData = new ArrayList<>();
                Elements cells = row.select("th, td");

                for (Element cell : cells) {
                    rowData.add(cell.text().strip());
                }

                tableData.add(rowData);
            }

            if (!tableData.isEmpty()) {
                List<String> header = tableData.get(0);
                boolean hasAllKeywords = new HashSet<>(header).containsAll(keywords);
                boolean hasAnyOptionalKeywords = optionalKeywords.stream().anyMatch(header::contains);

                if (hasAllKeywords && hasAnyOptionalKeywords) {
                    List<String> formattedRow = new ArrayList<>();
                    for (int j = 1; j < tableData.size(); j++) {
                        List<String> row = tableData.get(j);
                        if (row.size() < 2 || !(row.get(1).contains("년") && row.get(1).contains("월") && row.get(1).contains("일"))) continue;

                        // 동일한 날짜의 형태인 1-1차 2-2차에 대해서 우선은 문자열 통일
                        if (Arrays.stream(row.get(0).split("-")).count() == 2) {
                            formattedRow.add(row.get(0).split("-")[0] + "차" + ": " + row.get(1));
                        } else {
                            formattedRow.add(row.get(0) + ": " + row.get(1));
                        }
                    }
                    result.add(formattedRow);
                } else {
                    for (List<String> row : tableData) {
                        String title = row.get(0);
                        if (title.contains(midKeyword) && row.size() == 2) {
                            String body = row.get(1);
                            Matcher matcher = Pattern.compile("\\d+차: \\d{4}년 \\d{2}월 \\d{2}일").matcher(body);
                            List<String> dates = new ArrayList<>();

                            if (title.contains("월수익 중간기준가격 결정일")) {
                                // 월지급식의 자동조기상환가격 결정일
                                int idx = 1, turn = 1;
                                while (matcher.find()) {
                                    if (idx % 6 == 0) {
                                        String originalMatch = matcher.group();
                                        String updatedMatch = originalMatch.replaceFirst("\\d+차", turn + "차");
                                        dates.add(updatedMatch);
                                        turn++;
                                    }
                                    idx++;
                                }
                            } else {
                                while (matcher.find()) {
                                    dates.add(matcher.group());
                                }
                            }
                            result.add(dates);
                            break;
                        }
                    }
                }
            }
        }

        return result;
    }

    public List<List<String>> findVolatilitiesList(Document doc) {
        List<List<String>> result = new ArrayList<>();

        List<String> keywords = List.of("항목", "항 목", "항  목", "항    목", "내용", "내 용", "내  용", "내   용", "내      용");

        Elements tables = doc.select("table");

        for (Element table : tables) {
            List<List<String>> tableData = new ArrayList<>();
            Elements rows = table.select("tr");

            for (Element row : rows) {
                List<String> rowData = new ArrayList<>();
                Elements cells = row.select("th, td");

                for (Element cell : cells) {
                    rowData.add(cell.text().strip());
                }

                tableData.add(rowData);
            }

            if (!tableData.isEmpty()) {
                List<String> header = tableData.get(0);
                boolean hasAnyKeywords = keywords.stream().anyMatch(header::contains);

                if (hasAnyKeywords) {
                    for (int j = 1; j < tableData.size(); j++) {
                        List<String> row = tableData.get(j);
                        if (row.get(0).equals("기초자산가격 변동성")) {

                            List<String> formattedRow = new ArrayList<>();
                            Pattern pattern = Pattern.compile("-?\\s*\\[?([\\w가-힣&()0-9.,\\s]+?)]?\\s*:\\s*(변동성(?:지수)?\\s*)?([\\d.]+)%");

                            Matcher matcher = pattern.matcher(row.get(1));
                            StringBuilder reconstitution = new StringBuilder();

                            while (matcher.find()) {
                                if (!reconstitution.isEmpty()) {
                                    reconstitution.append(" / ");
                                }
                                /**
                                 * 첫 번째 그룹 : ([\\w가-힣&()0-9.,\\s]+?)
                                 * 두 번째 그룹 : (변동성지수\s*)?
                                 * 세 번째 그룹 : ([\\d.]+)
                                 */
                                String name = matcher.group(1).trim()
                                                            .replaceAll("보통주", "")
                                                            .replaceAll("\\(\\d+\\)", "")
                                                            .trim();
                                String value = matcher.group(3).trim();
                                reconstitution.append(name).append(" : ").append(value).append("%");
                            }

                            if (!reconstitution.isEmpty())
                                formattedRow.add(String.valueOf(reconstitution));

                            result.add(formattedRow);
                        }
                    }
                }
            }
        }

        return result;
    }

    public List<String> findInitialBasePriceEvaluationDateList(String issuer, Document doc) {

        List<String> result = new ArrayList<>();

        String datePattern = "\\d{4}년 \\d{2}월 \\d{2}일";
        Pattern pattern = Pattern.compile(datePattern);

        // 삼성증권 - 최초기준가격 결정일 (예정)
        if (issuer.equals("삼성증권")) {

            Elements tables = doc.select("table");

            for (Element table : tables) {
                // <table> 태그 내부의 모든 <td> 태그를 선택
                Elements tds = table.select("td");

                // "최초기준가격 결정일 (예정)"이 포함된 <td>를 찾기
                boolean containsTargetString = tds.stream()
                        .anyMatch(td -> td.text().contains("최초기준가격 결정일 (예정)"));

                if (containsTargetString) {
                    // 각 <td>의 텍스트를 검사하여 날짜 패턴과 일치하는 문자열을 찾기
                    for (Element td : tds) {
                        Matcher matcher = pattern.matcher(td.text());
                        if (matcher.find()) {
                            // 날짜 문자열을 출력
                            result.add(matcher.group());
                            break;
                        }
                    }
                }
            }

            return result;
        }

        Elements pTags = doc.select("p");

        Elements tables = doc.select("table");

        // p tag
        for (Element pTag : pTags) {
            if (pTag.text().contains("최초기준가격평가일")) {

                String pText = pTag.text();

                Matcher matcher = pattern.matcher(pText);
                if (matcher.find()) {
                    String date = matcher.group();
                    result.add(date);
                }
            }
        }

        // table
        for (Element table : tables) {
            // <table> 태그 내부의 모든 <td> 태그를 선택
            Elements tds = table.select("td");

            // "최초기준가격평가일"이 포함된 <td>를 찾기
            boolean containsTargetString = tds.stream()
                    .anyMatch(td -> td.text().contains("최초기준가격평가일"));

            if (containsTargetString) {
                // 각 <td>의 텍스트를 검사하여 날짜 패턴과 일치하는 문자열을 찾기
                for (Element td : tds) {
                    Matcher matcher = pattern.matcher(td.text());
                    if (matcher.find()) {
                        // 날짜 문자열을 출력
                        result.add(matcher.group());
                    }
                }
            }
        }

        return result;
    }

    // 보통 각 상품당 만기 평가일은 하나 이나, 발행사에서 교란일 등으로 인해 예상 평가일을 여러 날짜로 표기한 경우
    // 그중에 최초 만기 평가일을 가져오는 것으로 함
    public List<String> findMaturityEvaluationDateList(String issuer, Document doc) {
        List<String> result = new ArrayList<>();

        String datePattern = "\\d{4}년 \\d{2}월 \\d{2}일";
        Pattern pattern = Pattern.compile(datePattern);

        // 삼성증권 - 만기평가일 (예정)
        if (issuer.equals("삼성증권")) {

            Elements rows = doc.select("tr");

            for (Element row : rows) {
                Elements tds = row.select("td");
                for (Element td : tds) {
                    if (td.text().contains("만기평가일 (예정)")) {
                        Element dateTd = tds.get(tds.indexOf(td) + 1); // 같은 tr의 다음 td
                        String dates = dateTd.text();
                        Matcher matcher = pattern.matcher(dates);
                        if (matcher.find()) {
                            // 날짜 문자열을 출력
                            result.add(matcher.group());
                            break;
                        }
                    }
                }
            }

            return result;
        }

        // 교보증권 - 만기평가일
        if (issuer.equals("교보증권")) {

            Elements tables = doc.select("table");

            for (Element table : tables) {
                // <table> 태그 내부의 모든 <td> 태그를 선택
                Elements tds = table.select("td");

                // "만기평가일"이 포함된 <td>를 찾기
                boolean containsTargetString = tds.stream()
                        .anyMatch(td -> td.text().contains("만기평가일"));

                if (containsTargetString) {
                    // 각 <td>의 텍스트를 검사하여 날짜 패턴과 일치하는 문자열을 찾기
                    for (Element td : tds) {
                        Matcher matcher = pattern.matcher(td.text());
                        if (matcher.find()) {
                            // 날짜 문자열을 출력
                            result.add(matcher.group());
                            break;
                        }
                    }
                }
            }

            return result;
        }

        Elements pTags = doc.select("p");

        Elements tables = doc.select("table");

        // p tag
        for (Element pTag : pTags) {
            if (pTag.text().contains("만기평가일 :") || pTag.text().contains("만기상환평가일 :")) {

                String pText = pTag.text();

                Matcher matcher = pattern.matcher(pText);
                if (matcher.find()) {
                    String date = matcher.group();
                    result.add(date);
                }

                Element table = pTag.nextElementSibling();
                if (table == null)   continue;

                if (table.tagName().equals("table")) {
                    Elements tds = table.select("td");
                    for (Element td : tds) {
                        matcher = pattern.matcher(td.text());
                        if (matcher.find()) {
                            result.add(matcher.group());
                            break;
                        }
                    }
                }
            }
        }

        // table
        for (Element table : tables) {
            // <table> 태그 내부의 모든 <td> 태그를 선택
            Elements tds = table.select("td");

            // "만기상환평가일 :"이 포함된 <td>를 찾기
            boolean containsTargetString = tds.stream()
                    .anyMatch(td -> td.text().contains("만기상환평가일 :"));

            if (containsTargetString) {
                // 각 <td>의 텍스트를 검사하여 날짜 패턴과 일치하는 문자열을 찾기
                for (Element td : tds) {
                    Matcher matcher = pattern.matcher(td.text());
                    if (matcher.find()) {
                        // 날짜 문자열을 출력
                        result.add(matcher.group());
                        break;
                    }
                }
            }
        }

        return result;
    }

    public List<MaturityEvaluationDateType> findMaturityEvaluationDateCountList(String issuer, Document doc) {
        List<MaturityEvaluationDateType> result = new ArrayList<>();

        String datePattern = "\\d{4}년 \\d{2}월 \\d{2}일";
        Pattern pattern = Pattern.compile(datePattern);

        // 삼성증권 - 만기평가일 (예정)
        if (issuer.equals("삼성증권")) {
            Elements rows = doc.select("tr");

            int count;
            for (Element row : rows) {
                Elements tds = row.select("td");
                for (Element td : tds) {
                    if (td.text().contains("만기평가일 (예정)")) {
                        count = 0;

                        Element dateTd = tds.get(tds.indexOf(td) + 1); // 같은 tr의 다음 td
                        String dates = dateTd.text();
                        Matcher matcher = pattern.matcher(dates);
                        while (matcher.find()) {
                            count++;
                        }

                        if (count > 1)  result.add(MaturityEvaluationDateType.MULTIPLE);
                        else if (count == 1) result.add(MaturityEvaluationDateType.SINGLE);
                        else result.add(MaturityEvaluationDateType.UNKNOWN);
                    }
                }
            }

            return result;
        }

        // 교보증권 - 만기평가일
        if (issuer.equals("교보증권")) {

            Elements tables = doc.select("table");

            int count;
            for (Element table : tables) {
                // <table> 태그 내부의 모든 <td> 태그를 선택
                Elements tds = table.select("td");

                // "만기평가일"이 포함된 <td>를 찾기
                boolean containsTargetString = tds.stream()
                        .anyMatch(td -> td.text().contains("만기평가일"));

                if (containsTargetString) {
                    // 각 <td>의 텍스트를 검사하여 날짜 패턴과 일치하는 문자열을 찾기
                    for (Element td : tds) {
                        count = 0;
                        Matcher matcher = pattern.matcher(td.text());
                        while (matcher.find()) {
                            count++;
                        }

                        if (count > 1)  result.add(MaturityEvaluationDateType.MULTIPLE);
                        else if (count == 1) result.add(MaturityEvaluationDateType.SINGLE);
                    }
                }
            }

            return result;
        }

        // 키움증권 - 만기평가일
        if (issuer.equals("키움증권")) {
            Elements pTags = doc.select("p");

            int count;
            for (Element pTag : pTags) {

                if (pTag.text().contains("만기평가일 :")) {

                    String pText = pTag.text();

                    count = 0;
                    Matcher matcher = pattern.matcher(pText);
                    while (matcher.find()) {
                        count++;
                    }

                    if (count > 1)  result.add(MaturityEvaluationDateType.MULTIPLE);
                    else if (count == 1) result.add(MaturityEvaluationDateType.SINGLE);
                }
            }
            return result;
        }

        Elements pTags = doc.select("p");

        Elements tables = doc.select("table");

        // p tag
        int count;
        for (Element pTag : pTags) {
            if (pTag.text().contains("만기평가일 :") || pTag.text().contains("만기상환평가일 :")) {

                String pText = pTag.text();

                count = 0;
                Matcher matcher = pattern.matcher(pText);
                if (matcher.find()) {
                    count++;
                }

                Element table = pTag.nextElementSibling();
                if (table == null)   continue;

                if (table.tagName().equals("table")) {
                    Elements tds = table.select("td");
                    for (Element td : tds) {
                        matcher = pattern.matcher(td.text());
                        while (matcher.find()) {
                            count++;
                        }
                    }
                }

                if (count > 1)  result.add(MaturityEvaluationDateType.MULTIPLE);
                else if (count == 1) result.add(MaturityEvaluationDateType.SINGLE);
            }
        }

        // table
        for (Element table : tables) {
            count = 0;
            // <table> 태그 내부의 모든 <td> 태그를 선택
            Elements tds = table.select("td");

            // "만기상환평가일 :"이 포함된 <td>를 찾기
            boolean containsTargetString = tds.stream()
                    .anyMatch(td -> td.text().contains("만기상환평가일 :"));

            if (containsTargetString) {
                // 각 <td>의 텍스트를 검사하여 날짜 패턴과 일치하는 문자열을 찾기
                for (Element td : tds) {
                    Matcher matcher = pattern.matcher(td.text());
                    if (matcher.find()) {
                        count++;
                    }
                }
            }

            if (count > 1)  result.add(MaturityEvaluationDateType.MULTIPLE);
            else if (count == 1) result.add(MaturityEvaluationDateType.SINGLE);
        }

        return result;
    }

    private int findLocationOfProduct(String targetProductSession, Document doc) {

        int result = 0;

        Elements trElements = doc.select("tr:has(td:matchesOwn(주식회사|증권|증\\s권|주\\s식\\s회\\s사))");

        if (!trElements.isEmpty()) {

            // 찾은 tr 태그의 인덱스를 구함
            int indexOfTargetTr = Objects.requireNonNull(trElements.first()).elementSiblingIndex();
            Element nextTr = trElements.get(indexOfTargetTr);

            String innerHtml = nextTr.html();
            String[] parts = innerHtml.split("<br>");

            List<String> productSessionList = new ArrayList<>();
            for (String part : parts) {
                Matcher matcher = Pattern.compile("(?<=\\s|제|회|호|^)\\d+(?=\\s|제|회|호|$)").matcher(part);

                while (matcher.find()) {
                    productSessionList.add(matcher.group());
                }
            }
            if (productSessionList.isEmpty()) {
                // 제29589-29598회 형태
                Matcher matcher = Pattern.compile("\\d+-\\d+").matcher(nextTr.text());
                if (matcher.find()) {
                    String found = matcher.group();
                    String[] foundList = found.split("-");
                    int number1 = Integer.parseInt(foundList[0]);

                    result = Integer.parseInt(targetProductSession) - number1 + 1;
                }
            } else {
                result = productSessionList.indexOf(targetProductSession) + 1;
            }


        } else {
            log.error("findLocationOfProduct : 해당 조건을 만족하는 tr 태그를 찾을 수 없습니다.");
        }

        return result;
    }

    private LocalDate convertToLocalDate(String dateString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");
        return LocalDate.parse(dateString, formatter);
    }
}
