package com.kimwanyi.service.interfaces;

import com.kimwanyi.model.dto.LoggedInUserDto;
import com.kimwanyi.model.dto.forms.LoginForm;

public interface AuthenticationService {

    LoggedInUserDto authenticate(LoginForm loginForm);
}
