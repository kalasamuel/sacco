package com.kimwanyi.util.converter;

import com.kimwanyi.model.SavingsAccount;
import com.kimwanyi.model.dto.SavingsAccountDto;

import org.springframework.stereotype.Component;

@Component
public class SavingsAccountConverter {

    public SavingsAccountDto toDto(SavingsAccount account) {
        SavingsAccountDto dto = new SavingsAccountDto();
        dto.setId(account.getId());
        dto.setAccountNumber(account.getAccountNumber());
        dto.setMemberId(account.getMember() != null ? account.getMember().getId() : null);
        dto.setBalance(account.getBalance());
        return dto;
    }
}
