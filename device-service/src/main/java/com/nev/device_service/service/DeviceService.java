package com.nev.device_service.service;

import com.nev.device_service.dto.DeviceDto;
import com.nev.device_service.entity.Device;
import com.nev.device_service.exception.DeviceNotFound;
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
                .orElseThrow(() -> new DeviceNotFound("Device not found!"));
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
        device.setUser_id(input.getUserId());
        final Device savedDevice = deviceRepository.save(device);
        return maptoDto(savedDevice);
    }

    public DeviceDto updateDevice(Long id, DeviceDto input) {
        Device existing = deviceRepository.findById(id)
                .orElseThrow(() -> new DeviceNotFound("Device not Found!"));
        existing.setName(input.getName());
        existing.setType(input.getType());
        existing.setLocation(input.getLocation());
        existing.setUser_id(input.getUserId());

        final Device updatedDevice = deviceRepository.save(existing);
        return maptoDto(updatedDevice);
    }

    public void deleteDevice(Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new DeviceNotFound("Device not Found!"));
        deviceRepository.delete(device);
    }
}
