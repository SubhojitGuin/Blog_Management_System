package com.project.Blog_Management_System.Service;

import com.project.Blog_Management_System.Dto.ProfileUpdateDTO;
import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Enums.Role;
import com.project.Blog_Management_System.Exceptions.ResourceNotFoundException;
import com.project.Blog_Management_System.Repositories.UserRepository;
import com.project.Blog_Management_System.Service.Interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.project.Blog_Management_System.Utils.AppUtils.getCurrentUser;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Override
    public UserEntity getUserById(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Override
    @Transactional
    public UserEntity addUser(UserEntity user) {
        return userRepository.save(user);
    }

    @Override
    public UserEntity getUserByUsernameOrEmail(String username, String email) {
        return userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(username, email).orElse(null);
    }

    @Override
    @Transactional
    public ProfileUpdateDTO updateProfile(ProfileUpdateDTO profileUpdateDTO) {
        UserEntity user = getCurrentUser();
        System.out.println("user = " + user);
        modelMapper.map(profileUpdateDTO, user);
        userRepository.save(user);
        return modelMapper.map(user, ProfileUpdateDTO.class);
    }

    public boolean hasRole(Role role) {
        UserEntity user = getCurrentUser();
        return user.getRoles().contains(role);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsernameIgnoreCase(username).orElse(null);
    }
}
