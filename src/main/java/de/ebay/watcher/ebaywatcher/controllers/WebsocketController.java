package de.ebay.watcher.ebaywatcher.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.ebay.watcher.ebaywatcher.services.Scrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Controller
@Slf4j
public class WebsocketController {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final Scrapper scrapper;
    private final ExecutorService executorService;
    String destination;
    Future<?> submittedTask;
    @Value("#{${arrayOfURLS}}")
    private List<String> listOfURLs;
    @Value("#{${arrayOfProxies}}")
    private List<String> proxies;


    public WebsocketController(SimpMessagingTemplate simpMessagingTemplate, Scrapper scrapper) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.scrapper = scrapper;
        this.destination = "/topic/messages";
        this.executorService = Executors.newFixedThreadPool(1);
    }

    @MessageMapping("/startDefault")
    public void startTask() {
        startScrappTask(6*10000L);
    }

    @MessageMapping("/start")
    public void startTask(long scanDelay) {
        startScrappTask(scanDelay);
    }

    @MessageMapping("/stop")
    public String stopTask() {
        if (submittedTask == null) {
            return "Task not running";
        }
        try {
            submittedTask.cancel(true);
        } catch (Exception ex) {
            ex.printStackTrace();
            return "Error occurred while stopping task due to: " + ex.getMessage();
        }
        return "Stopped task";
    }

    @MessageMapping("/addLink")
    public void addLinkToList(String url) {
        stopTask();
        addLinkToScrappList(url);
        startTask();
    }

    private void startScrappTask(long scanDelay) {
        if (submittedTask != null) {
            simpMessagingTemplate.convertAndSend(destination, "Task already started");
            return;
        }
        simpMessagingTemplate.convertAndSend(destination, "Started task");
        var objectMapper = new ObjectMapper();

        submittedTask = executorService.submit(() -> {
            while (true) {
                var payload = objectMapper.writeValueAsString(scrapper.scrapp(listOfURLs, proxies));
                simpMessagingTemplate.convertAndSend(destination,
                        payload);
                removeFromProxies();
                Thread.sleep(scanDelay);
            }
        });
    }

    private void removeFromProxies() {
        List<String> modifiableProxies = new ArrayList<>(proxies);
        modifiableProxies.removeAll(scrapper.proxiesToRemove);
        log.info("Deleted {} from proxy list", scrapper.proxiesToRemove);
        scrapper.proxiesToRemove.clear();
        this.proxies = modifiableProxies;
        log.info("Proxies left: {}", this.proxies.size());
    }

    private void addLinkToScrappList(String link) {
        List<String> modifiableLinks = new ArrayList<>(listOfURLs);
        modifiableLinks.add(link);
        log.info("Added link to scrapp list: {}", link);
        this.listOfURLs = modifiableLinks;
    }
}
