package com.xiyu.bid.biddraftagent.application;

public interface TenderDocumentStorage {

    StoredTenderDocument store(Long projectId, String fileName, String contentType, byte[] content);
}
