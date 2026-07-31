package com.kimwanyi.service.interfaces;

import java.time.LocalDate;

import com.kimwanyi.model.dto.AccountStatementDto;

public interface StatementService {
    AccountStatementDto generateForMember(Long userAccountId, LocalDate fromDate, LocalDate toDate);
}
