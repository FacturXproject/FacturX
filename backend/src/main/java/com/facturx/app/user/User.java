package com.facturx.app.user;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long userId; //primary Key for my SQL 

    private String name; //column 

    private String lastName; //column 

    private String email; 

    private String homeAdress;

    private String sex;

    public User() {}
    //// setters
    public void setName(String name) {
        this.name = name;
    }
    public void setSex (String sex) {
        this.sex = sex ;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public void setHomeAdress(String homeAdress) {
        this.homeAdress = homeAdress;
    }
    public String getName() {
    return name;
    }

    //geters
    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getHomeAdress() {
        return homeAdress;
    }

    public String getSex() {
        return sex;
    }

    public Long getUserId() {
        return userId;
    }

}

//Java side                     Database side

//User object                   users table
//---------                     -----------
//userId      ───────────────>  user_id
//name        ───────────────>  name
//lastName    ───────────────>  last_name
//email       ───────────────>  email