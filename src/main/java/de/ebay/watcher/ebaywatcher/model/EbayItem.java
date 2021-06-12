package de.ebay.watcher.ebaywatcher.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EbayItem {
    String url;
    String title;
    boolean damaged;
    String addedTime;
}
