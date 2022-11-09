package org.cucumber.kobisscrapper;

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
    private Map<LocalDate, BoxOfficeData[]> boxOfficeData;

    public class BoxOfficeData{
        private int rank;
        private String title;
        private int code;

        public BoxOfficeData(int rank, String title, int code) {
            this.rank = rank;
            this.title = title;
            this.code = code;
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

        @Override
        public String toString() {
            return "BoxOfficeData{" +
                    "rank=" + rank +
                    ", title='" + title + '\'' +
                    ", code=" + code +
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
                        return new BoxOfficeData(rank, title, code);
                    }).toArray(BoxOfficeData[]::new);
                }).toArray(BoxOfficeData[][]::new);


        var dates = document.select(".rst_sch > div > h4")
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

    public BoxOfficeData[] getBoxOfficesByDate(LocalDate date) {
        return boxOfficeData.get(date);
    }

    public enum ImageType {
        POSTER,
        STILL_CUT
    }

    public String[] getImageUrlsByCode(int code, ImageType imageType) throws IOException {
        String url = "https://www.kobis.or.kr/kobis/business/mast/mvie/searchMovieDtl.do";
        Document document = Jsoup.connect(url)
                .data("code", code + "")
                .data("sType", "")
                .data("titleYN", "Y")
                .data("etcParam", "")
                .data("isOuterReq", "false")
                .post();
        Elements info2 = document.select("div.info2");
        return info2.get(imageType == ImageType.POSTER ? 0 : 1).select("img")
                .stream()
                .map(img -> img.attr("src"))
                .map(src -> "https://www.kobis.or.kr" + src.replaceFirst("thumb_x\\d\\d\\d", "thumb_x640"))
                .toArray(String[]::new);
    }
}
