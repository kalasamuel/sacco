package com.kimwanyi.service.interfaces;

import java.util.List;
import com.kimwanyi.model.UserAccount;

public interface EmailService {
    int sendToMembers(List<Long> memberIds, String subject, String message, UserAccount actor);
}
