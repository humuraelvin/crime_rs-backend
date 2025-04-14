package com.crime.reporting.crime_reporting_backend.mapper;

import com.crime.reporting.crime_reporting_backend.dto.PoliceOfficerResponse;
import com.crime.reporting.crime_reporting_backend.entity.PoliceOfficer;

/**
 * Interface for mapping between PoliceOfficer entities and DTOs
 */
public interface PoliceOfficerMapper {
    
    /**
     * Maps a PoliceOfficer entity to a PoliceOfficerResponse DTO
     * 
     * @param policeOfficer the entity to map
     * @return the mapped DTO
     */
    PoliceOfficerResponse mapToDto(PoliceOfficer policeOfficer);
} 