package com.kimwanyi.service.interfaces;
import java.util.List;

import com.kimwanyi.model.UserAccount;
import com.kimwanyi.model.dto.UserAccountDto;

public interface UserManagementService {

    List<UserAccountDto> search(String keyword);

    void setEnabled(Long userId, boolean enabled, UserAccount adminAccount);

    void resetPassword(Long userId, String newPassword, UserAccount adminAccount);

    void approveMember(Long userId, UserAccount adminAccount);
}
