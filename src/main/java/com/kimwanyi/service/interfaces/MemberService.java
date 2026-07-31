package com.kimwanyi.service.interfaces;

import com.kimwanyi.model.Member;
import com.kimwanyi.model.dto.MemberDto;
import com.kimwanyi.model.dto.forms.MemberRegistrationForm;
import com.kimwanyi.model.dto.forms.MemberUpdateForm;

import java.util.List;

public interface MemberService {

    Member registerMember(MemberRegistrationForm form);
    MemberDto getByUserAccountId(Long userAccountId);
    List<MemberDto> search(String keyword);
    MemberDto updateProfile(Long userAccountId, MemberUpdateForm form);
}
