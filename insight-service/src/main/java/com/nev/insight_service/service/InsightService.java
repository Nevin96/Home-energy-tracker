package com.nev.insight_service.service;

import com.nev.insight_service.dto.InsightDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class InsightService {

    private final UsageClient usageClient;

    public InsightService(UsageClient usageClient){
        this.usageClient = usageClient;
    }
    public InsightDto getOverview(Long userId){
        final UsageDto usageData = usageClient.getXDaysUsageForUser(userId,3);

    }
}
