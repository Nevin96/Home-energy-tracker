package com.nev.user_service;

import com.nev.user_service.entity.User;
import com.nev.user_service.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
class UserServiceApplicationTests {
	public static final int NUM_OF_USERS = 10;
	@Autowired
	private UserRepository userRepository;

	@Test
	void contextLoads() {
	}
	@Disabled
	@Test
	void createUsers(){
		for(int i = 1; i < NUM_OF_USERS; i++){
			User user = User.builder()
					.name("User"+i)
					.surname("Surname"+i)
					.email("user"+i+"@example.com")
					.address(i+"th street")
					.alerting(i%2 == 0)
					.energyAlertingThreshold(1000 + i)
					.build();
			userRepository.save(user);
		}
		log.info("users have been populated");
	}
}
