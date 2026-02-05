package com.greencoin.repository;

import com.greencoin.model.WasteReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface WasteReportRepository extends JpaRepository<WasteReport, Long> {
    List<WasteReport> findByStatus(WasteReport.ReportStatus status);

    List<WasteReport> findByReporterId(Long reporterId);

    List<WasteReport> findByCollectorId(Long collectorId);

    @Query(value = "SELECT * FROM waste_reports WHERE status = 'OPEN' AND " +
            "(6371 * acos(cos(radians(?1)) * cos(radians(latitude)) * cos(radians(longitude) - radians(?2)) + sin(radians(?1)) * sin(radians(latitude)))) < ?3", nativeQuery = true)
    List<WasteReport> findNearby(Double lat, Double lon, Double radiusInKm);
}
