package com.facturx.app.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

//this class gives you already operation such as :
    //save(user);
    //findById(id);
    //findAll();
    //delete(user);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

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