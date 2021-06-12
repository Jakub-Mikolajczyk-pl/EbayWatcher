package de.ebay.watcher.ebaywatcher.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.ebay.watcher.ebaywatcher.services.Scrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Controller
public class WebsocketController {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final Scrapper scrapper;
    private final ExecutorService executorService;
    String destination;
    Future<?> submittedTask;
    @Value("#{${arrayOfURLS}}")
    private List<String> listOfURLs;

    public WebsocketController(SimpMessagingTemplate simpMessagingTemplate, Scrapper scrapper) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.scrapper = scrapper;
        this.destination = "/topic/messages";
        this.executorService = Executors.newFixedThreadPool(1);
    }

    @MessageMapping("/startDefault")
    public void startTask() {
        startScrappTask(10000);
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
        listOfURLs.add(url);
        stopTask();
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
                var payload = objectMapper.writeValueAsString(scrapper.scrapp(listOfURLs));
                simpMessagingTemplate.convertAndSend(destination,
                        payload);
                Thread.sleep(scanDelay);
            }
        });
    }
}
