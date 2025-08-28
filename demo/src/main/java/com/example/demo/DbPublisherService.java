package com.example.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DbPublisherService {
  private final JdbcTemplate jdbc;
  private final JmsTemplate jmsTemplate;
  private final ObjectMapper mapper = new ObjectMapper();

  @Value("${app.queue.name}")
  private String queueName;

  public DbPublisherService(JdbcTemplate jdbc, JmsTemplate jmsTemplate) {
    this.jdbc = jdbc;
    this.jmsTemplate = jmsTemplate;
    // ensure queue mode (not topic)
    this.jmsTemplate.setPubSubDomain(false);
  }

  public int publishAllItems() throws Exception {
    List<Map<String,Object>> rows = jdbc.queryForList("SELECT * FROM sample_table");
    for (Map<String,Object> row : rows) {
      String json = mapper.writeValueAsString(row);
      jmsTemplate.convertAndSend(queueName, json);
    }
    return rows.size();
  }
}
