import org.cucumber.kobisscrapper.KobisScrapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;

public class KobisScrapperTest {
    @Test
    void getSynopsisByCode() throws IOException {
        Assertions.assertTrue(KobisScrapper.getSynopsisByCode(20226254).startsWith("“와칸다를 지켜라!” 거대한 두 세계의 충돌,"));
    }

    @Test
    void getMainPosterByCode() throws IOException {
        String url = "https://www.kobis.or.kr/common/mast/movie/2022/11/5fc3cbc27da64a1983c9abc90599d185.jpg";
        Assertions.assertEquals(KobisScrapper.getMainPosterByCode(20226254), url);
    }
}
