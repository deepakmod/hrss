package com.mod.hrss.security.oauth2;

import com.mod.hrss.entity.Role;
import com.mod.hrss.entity.RoleName;
import com.mod.hrss.entity.User;
import com.mod.hrss.entity.UserStatus;
import com.mod.hrss.repository.RoleRepository;
import com.mod.hrss.repository.UserRepository;
import com.mod.hrss.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);

        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (Exception ex) {
            log.error("Error processing OAuth2 user", ex);
            throw new OAuth2AuthenticationException(ex.getMessage());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
        
        String email = getEmailByProvider(registrationId, attributes);
        if (email == null) {
            throw new IllegalArgumentException("Email not found from OAuth2 provider");
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new IllegalArgumentException("User account is inactive");
            }
            user = updateExistingUser(user, registrationId, attributes);
        } else {
            user = registerNewUser(registrationId, email, attributes);
        }

        return UserPrincipal.create(user, attributes);
    }

    private String getEmailByProvider(String registrationId, Map<String, Object> attributes) {
        if ("google".equalsIgnoreCase(registrationId)) {
            return (String) attributes.get("email");
        } else if ("azure".equalsIgnoreCase(registrationId) || "microsoft".equalsIgnoreCase(registrationId)) {
            // Azure AD emails are usually in 'mail', 'preferred_username', or 'upn'
            String email = (String) attributes.get("mail");
            if (email == null) {
                email = (String) attributes.get("preferred_username");
            }
            return email;
        }
        return (String) attributes.get("email");
    }

    private User registerNewUser(String registrationId, String email, Map<String, Object> attributes) {
        User user = new User();
        user.setEmail(email);
        user.setStatus(UserStatus.ACTIVE);
        
        // Extract names
        String firstName = "";
        String lastName = "";
        if ("google".equalsIgnoreCase(registrationId)) {
            firstName = (String) attributes.get("given_name");
            lastName = (String) attributes.get("family_name");
        } else {
            String name = (String) attributes.get("name");
            if (name != null) {
                String[] parts = name.split(" ", 2);
                firstName = parts[0];
                lastName = parts.length > 1 ? parts[1] : "";
            }
        }
        
        user.setFirstName(firstName != null ? firstName : "OAuth2");
        user.setLastName(lastName != null ? lastName : "User");
        user.setPassword(""); // OAuth2 users don't have password logins initially
        user.setEmployeeCode("EMP-OAUTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        Role employeeRole = roleRepository.findByName(RoleName.ROLE_EMPLOYEE)
                .orElseThrow(() -> new IllegalStateException("Default role ROLE_EMPLOYEE not found"));
        user.setRoles(Collections.singleton(employeeRole));

        return userRepository.save(user);
    }

    private User updateExistingUser(User existingUser, String registrationId, Map<String, Object> attributes) {
        // Optionally update profile details from social provider
        String firstName = "";
        String lastName = "";
        if ("google".equalsIgnoreCase(registrationId)) {
            firstName = (String) attributes.get("given_name");
            lastName = (String) attributes.get("family_name");
        }
        if (firstName != null && !firstName.isEmpty()) {
            existingUser.setFirstName(firstName);
        }
        if (lastName != null && !lastName.isEmpty()) {
            existingUser.setLastName(lastName);
        }
        return userRepository.save(existingUser);
    }
}
