package org.cucumber.kobisscrapper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

public class KobisScrapper {
    private static final Gson gson = new Gson();

    public static class NotScrappedDateException extends Exception {

    }

    private final Map<LocalDate, BoxOfficeData[]> boxOfficeData;

    public static class BoxOfficeData {
        /**
         * 박스오피스 순위
         */
        private final int rank;

        /**
         * 영화 제목
         */
        private final String title;

        /**
         * 영화정보통합관리 표준코드(FIMS코드)
         */
        private final int code;

        private final LocalDate date;

        public BoxOfficeData(int rank, String title, int code, LocalDate date) {
            this.rank = rank;
            this.title = title;
            this.code = code;
            this.date = date;
        }

        public int getRank() {
            return rank;
        }

        public String getTitle() {
            return title;
        }

        public int getCode() {
            return code;
        }

        public LocalDate getDate() {
            return date;
        }

        @Override
        public String toString() {
            return "BoxOfficeData{" +
                    "rank=" + rank +
                    ", title='" + title + '\'' +
                    ", code=" + code +
                    ", date=" + date +
                    '}';
        }
    }


    public KobisScrapper(LocalDate start, LocalDate end) throws IOException {
        String url = "https://www.kobis.or.kr/kobis/business/stat/boxs/findDailyBoxOfficeList.do";
        Document document = Jsoup.connect(url)
                .data("loadEnd", "0")
                .data("sMultiMovieYn", "")
                .data("sRepNationCd", "")
                .data("sSearchFrom", start.toString())
                .data("sSearchTo", end.toString())
                .data("sWideAreaCd", "")
                .data("searchType", "search")
                .post();

        BoxOfficeData[][] tables = document.select(".rst_sch > div > table")
                .stream()
                .map(table -> {
                    Elements rows = table.select("tbody").first().select("tr");
                    return rows.stream().map(row -> {
                        Elements cols = row.select("td");
                        Element a = cols.get(1).select("a").first();
                        int code = Integer.parseInt(
                                Objects.requireNonNull(a)
                                        .attr("onclick")
                                        .split("','")[1]
                                        .split(Pattern.quote("');"))[0]);
                        int rank = Integer.parseInt(cols.get(0).text());
                        String title = a.attr("title");
                        LocalDate date = LocalDate.parse(cols.get(2).text());
                        return new BoxOfficeData(rank, title, code, date);
                    }).toArray(BoxOfficeData[]::new);
                }).toArray(BoxOfficeData[][]::new);


        LocalDate[] dates = document.select(".rst_sch > div > h4")
                .stream()
                .map(h4 -> h4.text().trim())
                .map(text -> text.substring(0, text.length() - 3))
                .map(dstr -> LocalDate.parse(dstr, DateTimeFormatter.ofPattern("uuuu년 MM월 dd일")))
                .toArray(LocalDate[]::new);

        boxOfficeData = new HashMap<>();
        for (int i = 0; i < tables.length; i++) {
            boxOfficeData.put(dates[i], tables[i]);
        }
    }

    /**
     * 입력된 날짜에 해당하는 박스오피스 데이터를 가져오는 함수
     *
     * @param date 박스오피스 순위를 매긴 기준일
     * @return 박스오피스 랭킹 및 코드를 담은 객체들의 배열을 반환
     * @throws NotScrappedDateException 입력된 날짜가 긁어오지 않은 날짜에 해당하는 경우 예외를 발생
     */
    public BoxOfficeData[] getBoxOfficesByDate(LocalDate date) throws NotScrappedDateException {
        if (!boxOfficeData.containsKey(date)) throw new NotScrappedDateException();
        return boxOfficeData.get(date);
    }

    public enum ImageType {
        POSTER,
        STILL_CUT
    }

    private static final HashMap<Integer, Document> popupCache = new HashMap<>();

    private static Document loadPopup(int code) throws IOException {
        if (popupCache.containsKey(code)) return popupCache.get(code);
        String url = "https://www.kobis.or.kr/kobis/business/mast/mvie/searchMovieDtl.do";
        Document document = Jsoup.connect(url)
                .data("code", code + "")
                .data("sType", "")
                .data("titleYN", "Y")
                .data("etcParam", "")
                .data("isOuterReq", "false")
                .post();
        popupCache.put(code, document);
        return document;
    }

    public static void clearPopupCache() {
        popupCache.clear();
    }

    public static String[] getImageUrlsByCode(int code, ImageType imageType, boolean thumbnail) throws IOException {
        Document document = loadPopup(code);
        Elements info2 = document.select("div.info2");
        return info2.get(imageType == ImageType.POSTER ? 0 : 1).select("img")
                .stream()
                .map(img -> img.attr("src"))
                .map(src -> "https://www.kobis.or.kr" +
                        (thumbnail ?
                                src.replaceFirst("thumb_x\\d\\d\\d", "thumb_x640") :
                                src.replaceFirst("thumb_x\\d\\d\\d/thn_", "")))
                .toArray(String[]::new);
    }

    public static String getMainPosterByCode(int code) throws IOException {
        return "https://www.kobis.or.kr" + Objects.requireNonNull(
                loadPopup(code).selectFirst("a.fl.thumb")
        ).attr("href");
    }

    public static String getSynopsisByCode(int code) throws IOException {
        Document document = loadPopup(code);
        Elements info2s = document.select(".info2");
        return info2s.stream()
                .filter(info2 -> info2.selectFirst("strong").text().trim().equals("시놉시스"))
                .map(info2 -> info2.selectFirst(".desc_info").text().trim())
                .findFirst()
                .get();
    }

    public static List<Actor> getActorList(int code) throws IOException {
        String url = "https://www.kobis.or.kr/kobis/business/mast/mvie/searchMovActorLists.do";
        String json = Jsoup.connect(url)
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .data("movieCd", code + "").ignoreContentType(true).post().body().text();
        return gson.fromJson(json, new TypeToken<List<Actor>>() {
        }.getType());
    }

    public class Actor {
        private String peopleNm;

        private String cast;

        private String actorGb;


        public String getActorName() {
            return peopleNm;
        }

        public String getCharacterName() {
            return cast;
        }

        public String getType() {
            switch (Integer.parseInt(actorGb)) {
                case 1:
                    return "주연";
                case 2:
                    return "조연";
                case 3:
                    return "특별출연";
                case 5:
                    return "단역";
                default:
                    return null;
            }
        }
    }
}
