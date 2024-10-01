package com.wl2c.elswherebatchservice.domain.product.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wl2c.elswherebatchservice.domain.product.model.dto.NewIssuerMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewIssuerMessageSender {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void send(String topic, NewIssuerMessage newIssuerMessage) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        try {
            String stringMessage = objectMapper.writeValueAsString(newIssuerMessage);
            log.info("new-issuer-alert Message Created : " + stringMessage);

            kafkaTemplate.send(topic, stringMessage);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}