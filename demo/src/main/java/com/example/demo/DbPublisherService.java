package com.example.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import jakarta.jms.Message;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;

// import java.util.List;
// import java.util.Map;

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
    this.jmsTemplate.setPubSubDomain(false);
    }

    // ---- OTel API bits (no SDK) ----
    private static final Tracer TRACER =
    GlobalOpenTelemetry.get().getTracer("poc.solace.jms");

    // Inject W3C headers into JMS message properties
    private static final TextMapSetter<Message> JMS_SETTER = (msg, key, value) -> {
        try { if (msg != null && key != null && value != null) msg.setStringProperty(key, value); }
        catch (Exception ignore) {}
    };

    public int publishAllItems() throws Exception {
        var rows = jdbc.queryForList("SELECT * FROM sample_table");
    
        for (var row : rows) {
          String json = mapper.writeValueAsString(row);
    
          // Create PRODUCER span and set messaging attributes
          Span span = TRACER.spanBuilder("solace publish")
              .setSpanKind(SpanKind.PRODUCER)
              .setAttribute("messaging.system", "solace")
              .setAttribute("messaging.destination.name", queueName)
              .setAttribute("current.method","publishAllItems")
              .setAttribute("current.instrumentation","otel.only")
              .startSpan();
    
          try (Scope scope = span.makeCurrent()) {
            // inject context into the JMS Message via a MessagePostProcessor
            jmsTemplate.convertAndSend(queueName, json, message -> {
              GlobalOpenTelemetry.get()
                  .getPropagators()
                  .getTextMapPropagator()
                  .inject(Context.current(), message, JMS_SETTER);
              return message;
            });
          } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
          } finally {
            span.end();
          }
        }
        return rows.size();
    }

    // public int publishAllItems() throws Exception {
    // List<Map<String,Object>> rows = jdbc.queryForList("SELECT * FROM sample_table");
    // for (Map<String,Object> row : rows) {
    //     String json = mapper.writeValueAsString(row);
    //     jmsTemplate.convertAndSend(queueName, json);
    // }
    // return rows.size();
    // }
}
