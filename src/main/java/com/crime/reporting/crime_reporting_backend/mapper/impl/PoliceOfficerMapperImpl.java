package com.crime.reporting.crime_reporting_backend.mapper.impl;

import com.crime.reporting.crime_reporting_backend.dto.PoliceOfficerResponse;
import com.crime.reporting.crime_reporting_backend.entity.PoliceOfficer;
import com.crime.reporting.crime_reporting_backend.mapper.PoliceOfficerMapper;
import org.springframework.stereotype.Component;

/**
 * Implementation of PoliceOfficerMapper for mapping between PoliceOfficer entities and DTOs
 */
@Component
public class PoliceOfficerMapperImpl implements PoliceOfficerMapper {

    @Override
    public PoliceOfficerResponse mapToDto(PoliceOfficer policeOfficer) {
        if (policeOfficer == null) {
            return null;
        }
        
        PoliceOfficerResponse response = new PoliceOfficerResponse();
        
        // Set base properties
        response.setId(policeOfficer.getId());
        response.setBadgeNumber(policeOfficer.getBadgeNumber());
        response.setRank(policeOfficer.getRank());
        response.setSpecialization(policeOfficer.getSpecialization());
        response.setJurisdiction(policeOfficer.getJurisdiction());
        response.setContactInfo(policeOfficer.getContactInfo());
        response.setCreatedAt(policeOfficer.getCreatedAt());
        response.setUpdatedAt(policeOfficer.getUpdatedAt());
        
        // Set user properties if user exists
        if (policeOfficer.getUser() != null) {
            response.setUserId(policeOfficer.getUser().getId());
            response.setFirstName(policeOfficer.getUser().getFirstName());
            response.setLastName(policeOfficer.getUser().getLastName());
            response.setEmail(policeOfficer.getUser().getEmail());
            response.setPhoneNumber(policeOfficer.getUser().getPhoneNumber());
        }
        
        // Set department properties if department exists
        if (policeOfficer.getDepartment() != null) {
            response.setDepartmentId(policeOfficer.getDepartment().getId());
            response.setDepartmentName(policeOfficer.getDepartment().getName());
        }
        
        return response;
    }
} 