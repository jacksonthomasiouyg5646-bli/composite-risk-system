package com.example.usermanagement.system.service;

import com.example.usermanagement.common.api.PageResult;

import java.util.Map;

public interface CreditDomainQueryService {
    PageResult<Map<String, Object>> query(CreditDomainQueryCriteria criteria, int page, int size);
}
