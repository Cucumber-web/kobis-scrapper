import org.cucumber.kobisscrapper.KobisScrapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;

public class KobisScrapperTest {
    @Test
    void getSynopsisByCode() throws IOException {
        var scrapper = new KobisScrapper(LocalDate.of(2022, 11, 9),
                LocalDate.of(2022, 11, 11));
        Assertions.assertTrue(scrapper.getSynopsisByCode(20226254).startsWith("“와칸다를 지켜라!” 거대한 두 세계의 충돌,"));
    }
}
