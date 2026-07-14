package com.nev.ingestion_service.service;

import com.nev.ingestion_service.kafka.event.EnergyUsageEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IngestionService {
    private final KafkaTemplate<String, EnergyUsageEvent> kafkaTemplate;
    public IngestionService(KafkaTemplate<String, EnergyUsageEvent> kafkaTemplate){
        this.kafkaTemplate = kafkaTemplate;
    }
}
