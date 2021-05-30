package de.ebay.watcher.ebayWatcher.services;

import de.ebay.watcher.ebayWatcher.model.EbayItem;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class Scrapper {
    public List<EbayItem> scrapp() throws IOException {
        //TODO: lista w bazie/zewnetrzna linkow
        String urlStr = "https://www.ebay-kleinanzeigen.de/s-zu-verschenken/lauenau/anbieter:privat/anzeige:angebote" +
                "/geschirrsp%C3%BCler-sp%C3%BClmaschine/k0c192l13461r50";
        List<EbayItem> ebayItems = new ArrayList<>();
        var elements = Jsoup.connect(urlStr).userAgent(Constants.USER_AGENT).get().select("li.ad-listitem");
        elements.forEach(element -> {
            String scrappedUrl = element.select("div.aditem-image").select("a").attr("href");
            String title = element.select("a.ellipsis").text();
            String time = element.select("div.aditem-main--top--right").text();
            String middleText = element.select("p.aditem-main--middle--description").text();
            if (checkTime(time)) {
                //TODO: Wrzucaj to na baze, żeby nie wyświetlać duplikatów
                ebayItems.add(EbayItem.builder().url("https://www.ebay-kleinanzeigen.de/" + scrappedUrl)
                                .title(title).damaged(checkIfDamaged(title, middleText)).build());
            }
        });
        return ebayItems;
    }

    private boolean checkIfDamaged(String title, String middleText) {
        return title.toLowerCase().contains("defekt")
                || title.toLowerCase().contains("nicht funktionsfähig")
                || middleText.toLowerCase().contains("defekt")
                || middleText.toLowerCase().contains("nicht funktionsfähig");
    }

    private boolean checkTime(String time) {
        if (time.toLowerCase().contains("heute")) {
            try {
                var localTime = LocalTime.now();
                var adTime = LocalTime.parse(time.substring(time.indexOf(",") + 2).trim());
                //TODO: Czas ustawiany z frontu
                return Duration.between(localTime, adTime).toMinutes() > -60L;
            } catch (DateTimeParseException e) {
                log.error(e.getMessage());
            }
        }
        return false;
    }
}

