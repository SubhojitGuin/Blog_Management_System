package com.project.Blog_Management_System.Utils;

import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Enums.Role;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;

public class AppUtils {

    public static UserEntity getCurrentUser() {
        return (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public static boolean hasRole(Role role) {
        return Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getAuthorities().stream().anyMatch(
                authority -> Objects.equals(authority.getAuthority(), "ROLE_" + role.name())
        );
    }

    public static String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-");
    }

    public Sort convertToSort(List<String> sortFields) {
        List<Sort.Order> orders = new ArrayList<>();
        for (String field : sortFields) {
            String[] propertyAndDirection = field.split(",");
            String property = propertyAndDirection[0];
            Sort.Direction direction = Sort.DEFAULT_DIRECTION;
            if (propertyAndDirection.length > 1) {
                String directionString = propertyAndDirection[1];
                direction = Sort.Direction.fromOptionalString(directionString)
                        .orElse(Sort.DEFAULT_DIRECTION);
            }
            Sort.Order order = new Sort.Order(direction, property);
            orders.add(order);
        }
        return Sort.by(orders);
    }

}
