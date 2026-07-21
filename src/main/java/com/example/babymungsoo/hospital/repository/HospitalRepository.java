package com.example.babymungsoo.hospital.repository;

import com.example.babymungsoo.hospital.entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, Long> {


    List<Hospital> findByIs24hourTrue();

    @Query("SELECT h FROM Hospital h WHERE " +
            "h.latitude BETWEEN :minLat AND :maxLat AND " +
            "h.longitude BETWEEN :minLng AND :maxLng")
    List<Hospital> findHospitalsByLocation(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng
    );

    @Query("SELECT h FROM Hospital h WHERE " +
            "h.is24hour = true AND " +
            "h.latitude BETWEEN :minLat AND :maxLat AND " +
            "h.longitude BETWEEN :minLng AND :maxLng")
    List<Hospital> findAvailableHospitalsByLocation(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng
    );
}