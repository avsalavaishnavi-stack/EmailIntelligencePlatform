package com.emailintel.backend.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InboxWatcher {
    private final EmailService service;
    public InboxWatcher(EmailService service){this.service=service;}
    @Scheduled(fixedDelay=3000)
    public void watch(){service.scanInbox();}
}
