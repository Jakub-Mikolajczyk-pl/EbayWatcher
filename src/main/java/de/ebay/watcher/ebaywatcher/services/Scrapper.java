package de.ebay.watcher.ebaywatcher.services;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import de.ebay.watcher.ebaywatcher.model.EbayItem;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@Slf4j
public class Scrapper {
    private final Set<EbayItem> ebayItemsAlreadyScrapped = new HashSet<>();
    public List<String> proxiesToRemove = new ArrayList<>();

    public Set<EbayItem> scrapp(List<String> urlStrings, List<String> proxies) {
        //TODO: lista w bazie/zewnetrzna linkow
        Set<EbayItem> ebayItems = new HashSet<>();
        urlStrings.forEach(url -> ebayItems.addAll(scrappEbayItems(url, proxies)));
        Sets.SetView<EbayItem> added = Sets.difference(ebayItems, ebayItemsAlreadyScrapped);
        ImmutableSet<EbayItem> immutableAddedEbayItems = added.immutableCopy();
        int firstRun = ebayItemsAlreadyScrapped.size();
        ebayItemsAlreadyScrapped.addAll(ebayItems);
        return firstRun == 0 ? ebayItems : immutableAddedEbayItems;
    }

    public Set<EbayItem> scrappEbayItems(String urlStr, List<String> proxies) {
        Set<EbayItem> ebayItems = new HashSet<>();
        String fullIPAddress = proxies.get(new Random().nextInt(proxies.size()));
        String[] ipWithPort = parseIPAndPort(fullIPAddress);
        try {
            log.info("using ip:" + Arrays.toString(ipWithPort));
            var elements = getElements(urlStr, ipWithPort);
            elements.forEach(element -> addScrappedItemsToEbayItems(urlStr, ebayItems, element));
        } catch (IOException e) {
            log.error("==ERROR== Error when scrapping: " + urlStr + " - error description: " + e);
            proxiesToRemove.add(fullIPAddress);
        }
        return ebayItems;
    }

    private void addScrappedItemsToEbayItems(String urlStr, Set<EbayItem> ebayItems, org.jsoup.nodes.Element element) {
        String scrappedUrl = element.select("div.aditem-image").select("a").attr("href");
        String title = element.select("a.ellipsis").text();
        String time = element.select("div.aditem-main--top--right").text();
        String middleText = element.select("p.aditem-main--middle--description").text();
        if (checkTime(time)) {
            log.info("==SUCCESS== For " + urlStr + " found " + title);
            ebayItems.add(EbayItem.builder()
                    .url("https://www.ebay-kleinanzeigen.de/" + scrappedUrl)
                    .title(title)
                    .damaged(checkIfDamaged(title, middleText))
                    .addedTime(parseTime(time).toString())
                    .build());
        }
    }

    private Elements getElements(String urlStr, String[] ipWithPort) throws IOException {
        return Jsoup.connect(urlStr)
                .proxy(ipWithPort[0], Integer.parseInt(ipWithPort[1]))
                .userAgent(Constants.USER_AGENT)
                .get().select("li.ad-listitem");
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
                var adTime = parseTime(time);
                //TODO: Czas ustawiany z frontu
                return Duration.between(localTime, adTime).toMinutes() > -60;
            } catch (DateTimeParseException e) {
                log.error(e.getMessage());
            }
        }
        return false;
    }

    private LocalTime parseTime(String time) {
        return LocalTime.parse(time.substring(time.indexOf(",") + 2).trim());
    }

    private String[] parseIPAndPort(String fullAddress) {
        return fullAddress.split(":");
    }
}

