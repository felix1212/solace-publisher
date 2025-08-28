package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.demo.DbPublisherService;

@RestController
public class PublishController {
  private final DbPublisherService svc;
  public PublishController(DbPublisherService svc) { this.svc = svc; }

  @GetMapping("/publish")
  public String publish() throws Exception {
    int count = svc.publishAllItems();
    return "Published " + count + " records to Solace queue.";
  }
}
