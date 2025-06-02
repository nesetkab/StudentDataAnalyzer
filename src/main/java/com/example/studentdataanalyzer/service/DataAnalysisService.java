// --- Data Analysis Service ---
// File: src/main/java/com/example/studentdataanalyzer/service/DataAnalysisService.java
package com.example.studentdataanalyzer.service;

import com.example.studentdataanalyzer.model.StudentData;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.LinkedHashMap; // To maintain insertion order for charts

@Service
public class DataAnalysisService {

    /**
     * Calculates the distribution of performance levels for each subject within each year.
     * Example: {2023: {"Language Performance": {"Proficient": 50, "Basic": 20}}}
     */
    public Map<Integer, Map<String, Map<String, Long>>> calculatePerformanceLevelDistributionBySubjectByYear(List<StudentData> data) {
        if (data == null || data.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return data.stream()
                .collect(Collectors.groupingBy(StudentData::getYear, LinkedHashMap::new, // Group by year
                        Collectors.groupingBy(StudentData::getSubject, LinkedHashMap::new, // Then by subject
                                Collectors.groupingBy(StudentData::getPerformanceLevel, LinkedHashMap::new, // Then by performance level
                                        Collectors.counting())))); // Count occurrences
    }

    /**
     * Calculates the average overall Scale Score for each subject within each year.
     * Note: ScaleScore is the overall score, repeated for each unpivoted subject entry.
     */
    public Map<Integer, Map<String, Double>> calculateAverageScaleScoreBySubjectByYear(List<StudentData> data) {
        if (data == null || data.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return data.stream()
                .collect(Collectors.groupingBy(StudentData::getYear, LinkedHashMap::new,
                        Collectors.groupingBy(StudentData::getSubject, LinkedHashMap::new,
                                Collectors.averagingDouble(StudentData::getScaleScore))));
    }

    /**
     * Calculates the average overall Scale Score grouped by Special Ed status, then by subject, then by year.
     */
    public Map<Boolean, Map<Integer, Map<String, Double>>> calculateAverageScaleScoreBySpecialEdAndSubjectByYear(List<StudentData> data) {
        if (data == null || data.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return data.stream()
                .collect(Collectors.groupingBy(StudentData::isSpecialEd, LinkedHashMap::new, // Primary group: Special Ed status
                        Collectors.groupingBy(StudentData::getYear, LinkedHashMap::new,      // Secondary group: Year
                                Collectors.groupingBy(StudentData::getSubject, LinkedHashMap::new, // Tertiary group: Subject
                                        Collectors.averagingDouble(StudentData::getScaleScore))))); // Aggregate: Average Scale Score
    }


    /**
     * Calculates the number of students passing (based on overall Scale Score) for each subject within each year.
     */
    public Map<Integer, Map<String, Long>> countPassingStudentsBySubjectByYear(List<StudentData> data) {
        if (data == null || data.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return data.stream()
                .filter(StudentData::isPassing) // Filter for students who are passing based on overall scale score
                .collect(Collectors.groupingBy(StudentData::getYear, LinkedHashMap::new,
                        Collectors.groupingBy(StudentData::getSubject, LinkedHashMap::new,
                                Collectors.counting())));
    }

    /**
     * Calculates the pass rate (percentage, based on overall Scale Score) for each subject within each year.
     */
    public Map<Integer, Map<String, Double>> calculatePassRateBySubjectByYear(List<StudentData> data) {
        if (data == null || data.isEmpty()) {
            return new LinkedHashMap<>();
        }
        // Total students per subject per year
        Map<Integer, Map<String, Long>> totalStudentsPerSubjectYear = data.stream()
                .collect(Collectors.groupingBy(StudentData::getYear, LinkedHashMap::new,
                        Collectors.groupingBy(StudentData::getSubject, LinkedHashMap::new,
                                Collectors.counting())));

        // Passing students per subject per year
        Map<Integer, Map<String, Long>> passingStudentsPerSubjectYear = countPassingStudentsBySubjectByYear(data);

        Map<Integer, Map<String, Double>> passRate = new LinkedHashMap<>();
        totalStudentsPerSubjectYear.forEach((year, subjectMap) -> {
            Map<String, Double> subjectPassRate = new LinkedHashMap<>();
            subjectMap.forEach((subject, totalCount) -> {
                long passingCount = passingStudentsPerSubjectYear.getOrDefault(year, new LinkedHashMap<>()).getOrDefault(subject, 0L);
                double rate = (totalCount > 0) ? (double) passingCount / totalCount * 100.0 : 0.0;
                subjectPassRate.put(subject, Math.round(rate * 100.0) / 100.0); // Round to 2 decimal places
            });
            if (!subjectPassRate.isEmpty()) {
                passRate.put(year, subjectPassRate);
            }
        });
        return passRate;
    }

    /**
     * Calculates average scale score by a given demographic field (e.g., Ethnicity, Gender) for each year.
     * This requires careful handling of unpivoted data to avoid double-counting student scale scores.
     * We group by student ID first within each year and demographic to get unique scale scores.
     */
    public Map<Integer, Map<String, Double>> calculateAverageScaleScoreByDemographicByYear(List<StudentData> data, String demographicType) {
        if (data == null || data.isEmpty()) {
            return new LinkedHashMap<>();
        }
        // Temporary structure to hold unique student scale scores for demographic grouping
        // StudentId -> {DemographicValue, ScaleScore}
        // This is to ensure each student's scale score is counted once per year for a given demographic.

        // Step 1: Get unique (Year, StudentID) -> (DemographicValue, ScaleScore)
        // This ensures that if a student has multiple subject entries, their scale score and demographic are considered once.
        Map<Integer, Map<String, StudentData>> uniqueStudentEntriesByYear = data.stream()
                .collect(Collectors.groupingBy(StudentData::getYear, LinkedHashMap::new,
                        Collectors.toMap(StudentData::getStudentId, sd -> sd, (sd1, sd2) -> sd1) // Keep first entry if duplicate studentId in same year
                ));

        // Step 2: Group these unique entries by demographic and calculate average scale score
        Map<Integer, Map<String, Double>> result = new LinkedHashMap<>();
        uniqueStudentEntriesByYear.forEach((year, studentMap) -> {
            Map<String, Double> demographicAverage = studentMap.values().stream()
                    .collect(Collectors.groupingBy(sd -> {
                        switch (demographicType.toLowerCase()) {
                            case "ethnicity": return sd.getEthnicity();
                            case "gender": return sd.getGender();
                            case "gradelevel": return sd.getGradeLevel();
                            case "overallperformance": return sd.getOverallPerformance();
                            default: return "Unknown";
                        }
                    }, LinkedHashMap::new, Collectors.averagingDouble(StudentData::getScaleScore)));
            if (!demographicAverage.isEmpty()) {
                result.put(year, demographicAverage);
            }
        });
        return result;
    }
}