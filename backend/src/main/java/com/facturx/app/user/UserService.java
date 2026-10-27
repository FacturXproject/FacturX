package com.facturx.app.user;
import org.springframework.stereotype.Service;
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService (UserRepository userRepository)  { /// to init the object userRepository defined under user/UserRepository.java
        this.userRepository = userRepository;
    }

	// Recharge l'utilisateur depuis la base : l'objet stocke dans la session peut etre
	// perime (profil modifie, statut change...) et ne doit pas ecraser la ligne en base.
	public User updateProfile(Long userId, String firstName, String lastName) {

		User user = userRepository.findById(userId).orElseThrow();

		user.setFirstName(firstName);
		user.setLastName(lastName);

		return userRepository.save(user);
	}

}
