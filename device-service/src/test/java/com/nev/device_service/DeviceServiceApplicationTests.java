package com.nev.device_service;

import com.nev.device_service.entity.Device;
import com.nev.device_service.model.DeviceType;
import com.nev.device_service.repository.DeviceRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
class DeviceServiceApplicationTests {
	public static final int NUM_OF_DEVICES = 200;
	public static final int USERS = 9;
	@Autowired
	private DeviceRepository deviceRepository;

	@Test
	void contextLoads() {
	}

	@Test
	@Disabled
	void createDevices(){
		for(int i = 1; i < NUM_OF_DEVICES; i++){
			var device = Device.builder()
					.name("Device" + i)
					.type(DeviceType.values()[i % DeviceType.values().length])
					.location("Location"+((i%3)+1))
					.user_id((long) ((i% USERS)+1))
					.build();
			deviceRepository.save(device);
		}
		log.info("Device repo has been populated");
	}
}
