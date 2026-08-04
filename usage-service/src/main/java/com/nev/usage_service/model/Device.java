package com.nev.usage_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Device {
    Long id;
    String name;
    String  type;
    String location;
    Long userId;
    Double energyConsumed;
}
