package com.nev.usage_service.model;

import lombok.Builder;

@Builder
public record DeviceEnergy(
        Long deviceId,
        double energyConsumed,
        Long userId) {}
