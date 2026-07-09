package com.nev.device_service.controller;

import com.nev.device_service.service.DeviceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/device")
public class DeviceController {
    private DeviceService deviceService;
    public DeviceController(DeviceService deviceService){
        this.deviceService = deviceService;
    }
    @GetMapping("/{id}")
    public ResponseEntity<DeviceDto> getDeviceById(@PathVariable Long id){
        DeviceDto device = deviceService.getDeviceByid(id);
        return ResponseEntity.ok(device);
    }
}
