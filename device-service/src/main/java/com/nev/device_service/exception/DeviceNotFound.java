package com.nev.device_service.exception;

public class DeviceNotFound extends RuntimeException {
    public DeviceNotFound(String message) {
        super(message);
    }
}
