package com.kimwanyi.util.converter;

import com.kimwanyi.model.UserAccount;
import com.kimwanyi.model.dto.UserAccountDto;

import org.springframework.stereotype.Component;

@Component
public class UserAccountConverter {

    public UserAccountDto toDto(UserAccount account) {
        UserAccountDto dto = new UserAccountDto();
        dto.setId(account.getId());
        dto.setUsername(account.getUsername());
        dto.setFirstName(account.getFirstName());
        dto.setLastName(account.getLastName());
        dto.setEmail(account.getEmail());
        dto.setRole(account.getRole() != null ? account.getRole().name() : null);
        dto.setEnabled(account.isEnabled());
        dto.setCreatedAt(account.getCreatedAt());
        return dto;
    }
}
