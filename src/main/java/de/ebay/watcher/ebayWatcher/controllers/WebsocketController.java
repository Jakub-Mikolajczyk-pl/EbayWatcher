package de.ebay.watcher.ebayWatcher.controllers;

import de.ebay.watcher.ebayWatcher.services.Scrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Controller
public class WebsocketController {

    @Autowired
    SimpMessagingTemplate simpMessagingTemplate;
    @Autowired
    Scrapper scrapper;
    String destination = "/topic/messages";
    @Value("#{${arrayOfURLS}}")
    private List<String> listOfURLs;
    ExecutorService executorService = Executors.newFixedThreadPool(1);
    Future<?> submittedTask;

    @MessageMapping("/start")
    public void startTask() {
        if (submittedTask != null) {
            simpMessagingTemplate.convertAndSend(destination, "Task already started");
            return;
        }
        simpMessagingTemplate.convertAndSend(destination, "Started task");
        submittedTask = executorService.submit(() -> {
            while (true) {
                simpMessagingTemplate.convertAndSend(destination,
                        LocalDateTime.now() + ": doing some work" + scrapper.scrapp(listOfURLs).toString());
                Thread.sleep(10000);
            }
        });
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
}
