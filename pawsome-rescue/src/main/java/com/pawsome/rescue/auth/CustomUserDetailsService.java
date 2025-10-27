package com.pawsome.rescue.auth;

import com.pawsome.rescue.auth.entity.Credentials;
import com.pawsome.rescue.auth.entity.UserRole;
import com.pawsome.rescue.auth.repository.CredentialsRepository;
import com.pawsome.rescue.auth.repository.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private CredentialsRepository credentialsRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Find by username in the main credentials table. If not found, they aren't authorized.
        Credentials credentials = credentialsRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found or not authorized: " + username));

        // Fetch user roles (this remains the same)
        Long userId = credentials.getUser().getId(); // Corrected to get ID from User
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);

        List<GrantedAuthority> authorities = userRoles.stream()
                .map(userRole -> new SimpleGrantedAuthority("ROLE_" + userRole.getRole().getName().toString()))
                .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(
                credentials.getUsername(),
                credentials.getPasswordHash(),
                authorities
        );
    }
}