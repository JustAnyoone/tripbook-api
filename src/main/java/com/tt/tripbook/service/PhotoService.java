package com.tt.tripbook.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.tt.tripbook.exception.BusinessRuleException;
import com.tt.tripbook.exception.ResourceNotFoundException;
import com.tt.tripbook.model.AppUser;
import com.tt.tripbook.model.Passenger;
import com.tt.tripbook.repository.PassengerRepository;
import com.tt.tripbook.repository.UserRepository;
import com.tt.tripbook.security.CurrentUser;
import java.io.IOException;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PhotoService {

    private static final long MAX_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final String FOLDER = "tripbook/passengers";
    private static final String USER_FOLDER = "tripbook/users";

    private final Cloudinary cloudinary;
    private final PassengerRepository passengerRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    public PhotoService(Cloudinary cloudinary,
                        PassengerRepository passengerRepository,
                        UserRepository userRepository,
                        CurrentUser currentUser) {
        this.cloudinary = cloudinary;
        this.passengerRepository = passengerRepository;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
    }

    @Transactional
    public String uploadPhoto(Long passengerId, MultipartFile file) {
        validate(file);

        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger not found: " + passengerId));

        if (!passenger.getOwner().getId().equals(currentUser.id())) {
            throw new BusinessRuleException("You can only upload photos for your own passengers");
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", FOLDER,
                            "public_id", "passenger_" + passengerId,
                            "overwrite", true,
                            // Resize to a square avatar automatically
                            "transformation", "c_fill,w_200,h_200,g_face"
                    ));
            String url = (String) result.get("secure_url");
            passenger.setPhotoUrl(url);
            passengerRepository.save(passenger);
            return url;
        } catch (IOException e) {
            throw new BusinessRuleException("Failed to upload photo: " + e.getMessage());
        }
    }

    @Transactional
    public void deletePhoto(Long passengerId) {
        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger not found: " + passengerId));

        if (!passenger.getOwner().getId().equals(currentUser.id())) {
            throw new BusinessRuleException("You can only manage photos for your own passengers");
        }

        if (passenger.getPhotoUrl() == null) {
            throw new BusinessRuleException("Passenger has no photo");
        }

        try {
            cloudinary.uploader().destroy(FOLDER + "/passenger_" + passengerId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            // Log but do not fail: the DB record should be cleared regardless
        }

        passenger.setPhotoUrl(null);
        passengerRepository.save(passenger);
    }

    // Sets the signed-in account's own profile picture, replacing the previous one
    @Transactional
    public String uploadUserPhoto(MultipartFile file) {
        validate(file);
        AppUser user = currentUser.get();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", USER_FOLDER,
                            "public_id", "user_" + user.getId(),
                            "overwrite", true,
                            "transformation", "c_fill,w_200,h_200,g_face"
                    ));
            String url = (String) result.get("secure_url");
            user.setPhotoUrl(url);
            userRepository.save(user);
            return url;
        } catch (IOException e) {
            throw new BusinessRuleException("Failed to upload photo: " + e.getMessage());
        }
    }

    @Transactional
    public void deleteUserPhoto() {
        AppUser user = currentUser.get();
        if (user.getPhotoUrl() == null) {
            throw new BusinessRuleException("Account has no photo");
        }
        try {
            cloudinary.uploader().destroy(USER_FOLDER + "/user_" + user.getId(), ObjectUtils.emptyMap());
        } catch (IOException e) {
            // Do not fail: the DB record should be cleared regardless
        }
        user.setPhotoUrl(null);
        userRepository.save(user);
    }

    private void validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessRuleException("File is empty");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessRuleException("File exceeds the 5 MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessRuleException("Only image files are accepted");
        }
    }
}
