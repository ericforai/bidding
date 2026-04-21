package com.xiyu.bid.contractborrow.controller;

public record ContractBorrowActionRequest(
    String actorName,
    String comment,
    String reason
) {
}
