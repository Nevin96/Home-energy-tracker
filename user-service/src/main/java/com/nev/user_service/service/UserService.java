package com.nev.user_service.service;

import com.nev.user_service.dto.UserDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserService {


    public UserDto createUser(UserDto userDto) {
        return userDto;
    }
}
