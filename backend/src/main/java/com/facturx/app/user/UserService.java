package com.facturx.app.user;
import java.util.List;
import org.springframework.stereotype.Service;
@Service
public class UserService {

    private final UserRepository userRepository;
    
    public UserService (UserRepository userRepository)  { /// to init the object userRepository defined under user/UserRepository.java
        this.userRepository = userRepository;
    }

    public List<User> getAllUsers() {

        return userRepository.findAll();
        

    }

}


//1. What does the client ask?
//   -> GET /api/users

//2. Who receives that HTTP request?
//   -> UserController

//3. Who contains the logic?
//   -> UserService

//4. Who talks to the database?
//   -> UserRepository

//5. What object represents each row?
//   -> User