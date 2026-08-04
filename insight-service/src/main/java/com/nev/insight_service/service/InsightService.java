package com.nev.insight_service.service;

import com.nev.insight_service.client.UsageClient;
import com.nev.insight_service.dto.DeviceDto;
import com.nev.insight_service.dto.InsightDto;
import com.nev.insight_service.dto.UsageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class InsightService {

    private final UsageClient usageClient;
    private OllamaChatModel ollamaChatModel;

    public InsightService(UsageClient usageClient,
                          OllamaChatModel ollamaChatModel){
        this.usageClient = usageClient;
        this.ollamaChatModel = ollamaChatModel;
    }
    public InsightDto getOverview(Long userId){
        final UsageDto usageData = usageClient.getXDaysUsageForUser(userId,3);
        log.info("Usage data received: {}", usageData);

        if (usageData != null) {
            log.info("Devices: {}", usageData.devices());
        }
        double totalUsage = usageData.devices().stream()
                .mapToDouble(DeviceDto::energyConsumed)
                .sum();
        log.info("calling Ollama for userid {} with total usage {}",userId,totalUsage);

        String prompt = new StringBuilder()
                .append("Analyse the following energy usage data and provide a "+
                        "concise overview with actionable insights.")
                .append("this data is the aggregated data of 3 days.")
                .append("Usage data: \n")
                .append(usageData.devices())
                .toString();

        ChatResponse response = ollamaChatModel.call(
                Prompt.builder()
                        .content(prompt)
                        .build());
        return InsightDto.builder()
                .userId(userId)
                .tips(response.getResult().getOutput().getText())
                .energyUsage(totalUsage)
                .build();
    }

    public InsightDto getSavingsTips(Long userId) {
        final UsageDto usageData = usageClient.getXDaysUsageForUser(userId,3);
        double totalUsage = usageData.devices().stream()
                .mapToDouble(DeviceDto::energyConsumed)
                .sum();
        log.info("calling Ollama for userid {} with total usage {}",userId,totalUsage);

        String prompt = new StringBuilder()
                .append("This is my total consumption for past 3 days. ")
                .append("how can i reduce my energy consumption and how does it compare to other households")
                .append("total energy used: \n")
                .append(usageData.devices())
                .toString();

        ChatResponse response = ollamaChatModel.call(
                Prompt.builder()
                        .content(prompt)
                        .build());
        return InsightDto.builder()
                .userId(userId)
                .tips(response.getResult().getOutput().getText())
                .energyUsage(totalUsage)
                .build();
    }
}
