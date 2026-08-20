package com.example.usermanagement.system.mapper;

import com.example.usermanagement.system.service.CreditDomainQueryCriteria;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface CreditDomainQueryMapper {
    List<Map<String, Object>> listCustomerView(@Param("criteria") CreditDomainQueryCriteria criteria, @Param("limit") int limit, @Param("offset") int offset);

    long countCustomerView(@Param("criteria") CreditDomainQueryCriteria criteria);

    List<Map<String, Object>> listLimitView(@Param("criteria") CreditDomainQueryCriteria criteria, @Param("limit") int limit, @Param("offset") int offset);

    long countLimitView(@Param("criteria") CreditDomainQueryCriteria criteria);

    List<Map<String, Object>> listApplicationContractView(@Param("criteria") CreditDomainQueryCriteria criteria, @Param("limit") int limit, @Param("offset") int offset);

    long countApplicationContractView(@Param("criteria") CreditDomainQueryCriteria criteria);

    List<Map<String, Object>> listDebtExposureView(@Param("criteria") CreditDomainQueryCriteria criteria, @Param("limit") int limit, @Param("offset") int offset);

    long countDebtExposureView(@Param("criteria") CreditDomainQueryCriteria criteria);

    List<Map<String, Object>> listDefaultOverdueView(@Param("criteria") CreditDomainQueryCriteria criteria, @Param("limit") int limit, @Param("offset") int offset);

    long countDefaultOverdueView(@Param("criteria") CreditDomainQueryCriteria criteria);
}
