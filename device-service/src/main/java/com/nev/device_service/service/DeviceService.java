package com.nev.device_service.service;

import com.nev.device_service.dto.DeviceDto;
import com.nev.device_service.entity.Device;
import com.nev.device_service.repository.DeviceRepository;
import org.springframework.stereotype.Service;

@Service
public class DeviceService {
    private DeviceRepository deviceRepository;
    public DeviceService(DeviceRepository deviceRepository){
        this.deviceRepository = deviceRepository;
    }
    public DeviceDto getDeviceById(Long id){
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Device not found!"));
        return maptoDto(device);
    }
    private DeviceDto maptoDto(Device device){
        DeviceDto dto = new DeviceDto();
        dto.setId(device.getId());
        dto.setName(device.getName());
        dto.setType(device.getType());
        dto.setLocation(device.getLocation());
        dto.setUserId(device.getUser_id());
        return dto;
    }

    public DeviceDto createDevice(DeviceDto input) {
        Device device = new Device();
        device.setName(input.getName());
        device.setLocation(input.getLocation());
        device.setType(input.getType());
        Device savedDevice = deviceRepository.save(device);
        return maptoDto(savedDevice);
    }
}
