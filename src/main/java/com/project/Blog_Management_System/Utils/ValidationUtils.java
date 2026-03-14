package com.project.Blog_Management_System.Utils;

import com.project.Blog_Management_System.Entities.CategoryEntity;
import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Exceptions.ResourceNotFoundException;

public class ValidationUtils {

    public static void isInvalidCategory(CategoryEntity category, String slug) {
        if (category == null || !category.getSlug().equals(slug)) {
            throw new ResourceNotFoundException("Category does not exist");
        }
    }

    public static void isInvalidUser(UserEntity user, String username) {
        if (user == null || user.getIsDeleted() || !user.getUsername().equalsIgnoreCase(username)) {
            throw new ResourceNotFoundException("User account does not exist");
        }
    }
}
