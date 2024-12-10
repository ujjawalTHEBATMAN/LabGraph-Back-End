package leonardo.labutilities.qualitylabpro.controller.analytics;

import java.time.LocalDateTime;
import java.util.List;

import leonardo.labutilities.qualitylabpro.constants.AvailableCoagulationAnalytics;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import leonardo.labutilities.qualitylabpro.dto.analytics.GenericValuesRecord;
import leonardo.labutilities.qualitylabpro.dto.analytics.MeanAndStandardDeviationRecord;
import leonardo.labutilities.qualitylabpro.service.analytics.CoagulationAnalyticsService;

@RestController("coagulation-analytics")
@SecurityRequirement(name = "bearer-key")
@RequestMapping("coagulation-analytics")
public class CoagulationAnalyticsController extends GenericAnalyticsController {

        private final CoagulationAnalyticsService coagulationAnalyticsService;

        public CoagulationAnalyticsController(CoagulationAnalyticsService coagulationAnalyticsService) {
                super(coagulationAnalyticsService);
                this.coagulationAnalyticsService = coagulationAnalyticsService;
        }

        @Override
        @GetMapping("/results/search/name/level")
        public ResponseEntity<List<GenericValuesRecord>> getAnalyticsByLevel(Pageable pageable, String name,
                        String level) {
                return ResponseEntity.ok(coagulationAnalyticsService
                                .findAllAnalyticsByNameAndLevel(pageable, name, level));
        }

        @GetMapping("/results/names/date-range")
        public ResponseEntity<List<GenericValuesRecord>> getAllAnalyticsDateBetween(
                        @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
                        @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {

                AvailableCoagulationAnalytics names = new AvailableCoagulationAnalytics();
                List<GenericValuesRecord> resultsList = coagulationAnalyticsService
                                .getAllByNameInAndDateBetween(names.availableCoagulationAnalytics(), startDate,
                                                endDate);
                return ResponseEntity.ok(resultsList);
        }

        @Override
        @GetMapping("/results/search/date-range")
        public ResponseEntity<List<GenericValuesRecord>> getAllAnalyticsByDateRange(
                        @RequestParam String name,
                        @RequestParam String level,
                        @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
                        @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {
                return ResponseEntity.ok(coagulationAnalyticsService
                                .findAllAnalyticsByNameAndLevelAndDate(name, level, startDate, endDate));
        }

        @Override
        @GetMapping("/results/mean-standard-deviation")
        public ResponseEntity<MeanAndStandardDeviationRecord> getMeanAndStandardDeviation(
                        @RequestParam String name,
                        @RequestParam String level,
                        @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
                        @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {
                return ResponseEntity
                                .ok(coagulationAnalyticsService.generateMeanAndStandardDeviation(name, level, startDate,
                                                endDate));
        }
}
