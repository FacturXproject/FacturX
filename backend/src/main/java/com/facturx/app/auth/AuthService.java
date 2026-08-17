package com.facturx.app.auth;

import com.facturx.app.user.User;
import com.facturx.app.user.UserRepository;
import org.springframework.stereotype.Service;
@Service
public class AuthService {

    private final UserRepository userRepository;
    
    public AuthService(UserRepository userRepository)  { /// to init the object userRepository defined under user/UserRepository.java
        this.userRepository = userRepository;
    }

    public String register(User user) {

        userRepository.save(user);
        return "User registered";

    }

}


// the mental Appraoch to follow 
//Request    == external
//  |
//Controller == receives HTTP request
//  |
//Service    ==  contains your  logic
//  |
//Repository == asks for data / saves data
//  |
//JPA        == maps Java objects to database tables
//  |
//PostgreSQL == stores the actual data                


                            