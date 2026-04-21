package com.xiyu.bid.contractborrow.controller;

import java.time.LocalDate;

public record CreateContractBorrowRequest(
    String contractNo,
    String contractName,
    String sourceName,
    String borrowerName,
    String borrowerDept,
    String customerName,
    String purpose,
    String borrowType,
    LocalDate expectedReturnDate
) {
}
