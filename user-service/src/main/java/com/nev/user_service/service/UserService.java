package com.nev.user_service.service;

import com.nev.user_service.dto.UserDto;
import com.nev.user_service.entity.User;
import com.nev.user_service.repository.UserRepository;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    public UserService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public void updateUser(Long id, UserDto dto) {
        log.info("Updating user info with id : {}",id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User Not Found!"));
        user.setName(dto.getName());
        user.setSurname(dto.getSurname());
        user.setEmail(dto.getEmail());
        user.setAddress(dto.getAddress());
        user.setAlerting(dto.isAlerting());
        user.setEnergyAlertingThreshold(dto.getEnergyAlertingThreshold());

        userRepository.save(user);
    }

    public UserDto createUser(UserDto input) {
        log.info("creating user: {}",input);
        final User createdUser = User.builder()
                .name(input.getName())
                .surname(input.getSurname())
                .email(input.getEmail())
                .address(input.getAddress())
                .alerting(input.isAlerting())
                .energyAlertingThreshold(input.getEnergyAlertingThreshold())
                .build();
        final User saved = userRepository.save(createdUser);
        return toDto(saved);
    }

    private UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .surname(user.getSurname())
                .email(user.getEmail())
                .address(user.getAddress())
                .alerting(user.isAlerting())
                .energyAlertingThreshold(user.getEnergyAlertingThreshold())
                .build();
    }

    public UserDto getUserById(Long id) {
        log.info("getting user by id: {}",id);
        return userRepository.findById(id)
                .map(this::toDto)
                .orElse(null);
    }

    public void deleteUser(Long id) {
        log.info("deleting user: {}",id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User Not Found"));
        userRepository.delete(user);
    }
}
