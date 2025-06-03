// --- Controller to Handle Web Requests ---
// File: src/main/java/com/example/studentdataanalyzer/controller/DataUploadController.java
package com.example.studentdataanalyzer.controller;

import com.example.studentdataanalyzer.model.StudentData;
import com.example.studentdataanalyzer.service.CsvParserService;
import com.example.studentdataanalyzer.service.DataAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


@RestController
@RequestMapping("/api/data")
@CrossOrigin(origins = "*")
public class DataUploadController {

    private static final Logger LOGGER = Logger.getLogger(DataUploadController.class.getName());

    private final CsvParserService csvParserService;
    private final DataAnalysisService dataAnalysisService;

    @Autowired
    public DataUploadController(CsvParserService csvParserService, DataAnalysisService dataAnalysisService) {
        this.csvParserService = csvParserService;
        this.dataAnalysisService = dataAnalysisService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadAndAnalyzeData(
            @RequestParam("file") MultipartFile file,
            @RequestParam("year") int year) {
        LOGGER.info("Received file upload request: " + file.getOriginalFilename() + " for year: " + year);
        Map<String, Object> responseBody = new HashMap<>();

        if (file.isEmpty()) {
            LOGGER.warning("Upload attempt with an empty file.");
            responseBody.put("error", "Please select a CSV file to upload.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody);
        }
        if (year <= 1900 || year > 2100) {
            LOGGER.warning("Invalid year provided: " + year);
            responseBody.put("error", "Please provide a valid year (e.g., 2023).");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody);
        }


        try {
            List<StudentData> studentDataList = csvParserService.parseCsv(file, year);
            LOGGER.info("Successfully parsed CSV. Number of unpivoted records: " + studentDataList.size());

            if (studentDataList.isEmpty() && !(file.getSize() > 0)) {
                LOGGER.info("CSV file was parsed but contained no data records.");
                responseBody.put("message", "CSV file is empty or contains no data records after header.");
                return ResponseEntity.ok(responseBody);
            }

            Map<String, Object> analysisResults = new HashMap<>();

            analysisResults.put("subjectPerformanceLevelDistributionByYear", dataAnalysisService.calculateSubjectPerformanceLevelDistributionByYear(studentDataList));
            analysisResults.put("riseElaProficiencyDistributionByGradeByYear", dataAnalysisService.calculateRiseElaProficiencyDistributionByGradeByYear(studentDataList));
            analysisResults.put("mathProficiencyDistributionByGradeByYear", dataAnalysisService.calculateMathProficiencyDistributionByGradeByYear(studentDataList));

            analysisResults.put("averageOverallScaleScoreByYear", dataAnalysisService.calculateAverageOverallScaleScoreByYear(studentDataList));
            analysisResults.put("averageOverallScaleScoreOfStudentsInSubjectAreaGroupsByYear", dataAnalysisService.calculateAverageOverallScaleScoreOfStudentsInSubjectAreaGroupsByYear(studentDataList));
            analysisResults.put("averageOverallScaleScoreBySpecialEdAndSubjectAreaByYear", dataAnalysisService.calculateAverageOverallScaleScoreBySpecialEdAndSubjectAreaByYear(studentDataList));

            analysisResults.put("overallElaPassRateByYear", dataAnalysisService.calculateOverallElaPassRateByYear(studentDataList));
            // Add overall Math pass rate if Math proficiency implies passing

            analysisResults.put("averageOverallScaleScoreByEthnicityByYear", dataAnalysisService.calculateAverageOverallScaleScoreByDemographicByYear(studentDataList, "ethnicity"));
            analysisResults.put("averageOverallScaleScoreByGenderByYear", dataAnalysisService.calculateAverageOverallScaleScoreByDemographicByYear(studentDataList, "gender"));
            analysisResults.put("averageOverallScaleScoreByGradeLevelByYear", dataAnalysisService.calculateAverageOverallScaleScoreByDemographicByYear(studentDataList, "gradelevel"));
            analysisResults.put("averageOverallScaleScoreByOverallPerformanceCsvByYear", dataAnalysisService.calculateAverageOverallScaleScoreByDemographicByYear(studentDataList, "overallperformance_csv"));
            analysisResults.put("averageOverallScaleScoreByRiseElaProficiencyByYear", dataAnalysisService.calculateAverageOverallScaleScoreByDemographicByYear(studentDataList, "riseproficiency_ela")); // Corrected key
            analysisResults.put("averageOverallScaleScoreByMathProficiencyByYear", dataAnalysisService.calculateAverageOverallScaleScoreByDemographicByYear(studentDataList, "mathproficiency"));
            analysisResults.put("averageOverallScaleScoreByEllByYear", dataAnalysisService.calculateAverageOverallScaleScoreByDemographicByYear(studentDataList, "ell"));
            analysisResults.put("averageOverallScaleScoreBySpecialEdByYear", dataAnalysisService.calculateAverageOverallScaleScoreByDemographicByYear(studentDataList, "specialed"));


            analysisResults.put("totalUnpivotedRecordsProcessed", studentDataList.size());
            analysisResults.put("fileName", file.getOriginalFilename());
            analysisResults.put("datasetYear", year);


            LOGGER.info("Analysis complete. Sending results for year " + year);
            return ResponseEntity.ok(analysisResults);

        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Error processing CSV file (IllegalArgumentException): " + e.getMessage(), e);
            responseBody.put("error", "Error in CSV data or format: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "IOException during CSV processing: " + e.getMessage(), e);
            responseBody.put("error", "Could not read or process the CSV file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseBody);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An unexpected error occurred during file upload and analysis: " + e.getMessage(), e);
            responseBody.put("error", "An unexpected server error occurred. Please try again later.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseBody);
        }
    }
}
