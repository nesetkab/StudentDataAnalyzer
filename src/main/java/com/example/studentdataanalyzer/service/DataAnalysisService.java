// --- Data Analysis Service ---
// File: src/main/java/com/example/studentdataanalyzer/service/DataAnalysisService.java
package com.example.studentdataanalyzer.service;

import com.example.studentdataanalyzer.model.StudentData;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;

@Service
public class DataAnalysisService {

    private List<StudentData> getUniqueStudentYearEntries(List<StudentData> allData) {
        if (allData == null || allData.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(allData.stream()
                .collect(Collectors.toMap(
                        sd -> sd.getStudentId() + "_" + sd.getYear(),
                        sd -> sd,
                        (sd1, sd2) -> sd1
                )).values());
    }

    public Map<Integer, Map<String, Map<String, Long>>> calculateSubjectPerformanceLevelDistributionByYear(List<StudentData> allUnpivotedData) {
        if (allUnpivotedData == null || allUnpivotedData.isEmpty()) return new LinkedHashMap<>();
        return allUnpivotedData.stream()
                .filter(sd -> sd.getSubjectArea() != null && sd.getSubjectPerformanceLevel() != null)
                .collect(Collectors.groupingBy(StudentData::getYear, LinkedHashMap::new,
                        Collectors.groupingBy(StudentData::getSubjectArea, LinkedHashMap::new,
                                Collectors.groupingBy(StudentData::getSubjectPerformanceLevel, LinkedHashMap::new,
                                        Collectors.counting()))));
    }

    public Map<Integer, Map<String, Map<String, Long>>> calculateRiseElaProficiencyDistributionByGradeByYear(List<StudentData> allUnpivotedData) {
        if (allUnpivotedData == null || allUnpivotedData.isEmpty()) return new LinkedHashMap<>();
        List<StudentData> uniqueStudentEntries = getUniqueStudentYearEntries(allUnpivotedData);
        return uniqueStudentEntries.stream()
                .filter(sd -> sd.getRiseElaProficiencyLevel() != null)
                .collect(Collectors.groupingBy(StudentData::getYear, LinkedHashMap::new,
                        Collectors.groupingBy(StudentData::getGradeLevel, LinkedHashMap::new,
                                Collectors.groupingBy(StudentData::getRiseElaProficiencyLevel, LinkedHashMap::new,
                                        Collectors.counting()))));
    }

    public Map<Integer, Map<String, Map<String, Long>>> calculateMathProficiencyDistributionByGradeByYear(List<StudentData> allUnpivotedData) {
        if (allUnpivotedData == null || allUnpivotedData.isEmpty()) return new LinkedHashMap<>();
        List<StudentData> uniqueStudentEntries = getUniqueStudentYearEntries(allUnpivotedData);
        return uniqueStudentEntries.stream()
                .filter(sd -> sd.getMathProficiencyLevel() != null)
                .collect(Collectors.groupingBy(StudentData::getYear, LinkedHashMap::new,
                        Collectors.groupingBy(StudentData::getGradeLevel, LinkedHashMap::new,
                                Collectors.groupingBy(StudentData::getMathProficiencyLevel, LinkedHashMap::new,
                                        Collectors.counting()))));
    }

    public Map<Integer, Double> calculateAverageOverallScaleScoreByYear(List<StudentData> allUnpivotedData) {
        if (allUnpivotedData == null || allUnpivotedData.isEmpty()) return new LinkedHashMap<>();
        List<StudentData> uniqueStudentEntries = getUniqueStudentYearEntries(allUnpivotedData);
        return uniqueStudentEntries.stream()
                .collect(Collectors.groupingBy(StudentData::getYear, LinkedHashMap::new,
                        Collectors.collectingAndThen(
                                Collectors.averagingDouble(StudentData::getScaleScore),
                                avg -> Math.round(avg * 100.0) / 100.0)
                ));
    }

    public Map<Integer, Map<String, Double>> calculateAverageOverallScaleScoreOfStudentsInSubjectAreaGroupsByYear(List<StudentData> allUnpivotedData) {
        if (allUnpivotedData == null || allUnpivotedData.isEmpty()) return new LinkedHashMap<>();
        Map<Integer, Map<String, Set<StudentData>>> uniqueStudentsByYearSubjectArea = allUnpivotedData.stream()
                .filter(sd -> sd.getSubjectArea() != null)
                .collect(Collectors.groupingBy(StudentData::getYear, LinkedHashMap::new,
                        Collectors.groupingBy(StudentData::getSubjectArea, LinkedHashMap::new,
                                Collectors.toSet())));

        Map<Integer, Map<String, Double>> result = new LinkedHashMap<>();
        uniqueStudentsByYearSubjectArea.forEach((year, subjectAreaMap) -> {
            Map<String, Double> subjectAreaAverages = new LinkedHashMap<>();
            subjectAreaMap.forEach((subjectArea, studentSet) -> {
                if (!studentSet.isEmpty()) {
                    double average = studentSet.stream().mapToDouble(StudentData::getScaleScore).average().orElse(0.0);
                    subjectAreaAverages.put(subjectArea, Math.round(average * 100.0) / 100.0);
                }
            });
            if (!subjectAreaAverages.isEmpty()) result.put(year, subjectAreaAverages);
        });
        return result;
    }

    public Map<Boolean, Map<Integer, Map<String, Double>>> calculateAverageOverallScaleScoreBySpecialEdAndSubjectAreaByYear(List<StudentData> allUnpivotedData) {
        if (allUnpivotedData == null || allUnpivotedData.isEmpty()) return new LinkedHashMap<>();
        Map<Boolean, Map<Integer, Map<String, Set<StudentData>>>> uniqueStudentsBySpEdYearSubjectArea = allUnpivotedData.stream()
                .filter(sd -> sd.getSubjectArea() != null)
                .collect(Collectors.groupingBy(StudentData::isSpecialEd, LinkedHashMap::new,
                        Collectors.groupingBy(StudentData::getYear, LinkedHashMap::new,
                                Collectors.groupingBy(StudentData::getSubjectArea, LinkedHashMap::new,
                                        Collectors.toSet()))));

        Map<Boolean, Map<Integer, Map<String, Double>>> result = new LinkedHashMap<>();
        uniqueStudentsBySpEdYearSubjectArea.forEach((isSpEd, yearMap) -> {
            Map<Integer, Map<String, Double>> yearAverages = new LinkedHashMap<>();
            yearMap.forEach((year, subjectAreaMap) -> {
                Map<String, Double> subjectAreaAverages = new LinkedHashMap<>();
                subjectAreaMap.forEach((subjectArea, studentSet) -> {
                    if (!studentSet.isEmpty()) {
                        double average = studentSet.stream().mapToDouble(StudentData::getScaleScore).average().orElse(0.0);
                        subjectAreaAverages.put(subjectArea, Math.round(average * 100.0) / 100.0);
                    }
                });
                if (!subjectAreaAverages.isEmpty()) yearAverages.put(year, subjectAreaAverages);
            });
            if (!yearAverages.isEmpty()) result.put(isSpEd, yearAverages);
        });
        return result;
    }

    public Map<Integer, Long> countElaPassingStudentsByYear(List<StudentData> allUnpivotedData) {
        if (allUnpivotedData == null || allUnpivotedData.isEmpty()) return new LinkedHashMap<>();
        List<StudentData> uniqueStudentEntries = getUniqueStudentYearEntries(allUnpivotedData);
        return uniqueStudentEntries.stream()
                .filter(StudentData::isElaPassing)
                .collect(Collectors.groupingBy(StudentData::getYear, LinkedHashMap::new,
                        Collectors.counting()));
    }

    public Map<Integer, Double> calculateOverallElaPassRateByYear(List<StudentData> allUnpivotedData) {
        if (allUnpivotedData == null || allUnpivotedData.isEmpty()) return new LinkedHashMap<>();
        List<StudentData> uniqueStudentEntries = getUniqueStudentYearEntries(allUnpivotedData);

        List<StudentData> elaAssessedUniqueStudents = uniqueStudentEntries.stream()
                .filter(sd -> sd.getRiseElaProficiencyLevel() != null && !sd.getRiseElaProficiencyLevel().startsWith("N/A"))
                .collect(Collectors.toList());

        Map<Integer, Long> totalElaAssessedUniqueStudentsByYear = elaAssessedUniqueStudents.stream()
                .collect(Collectors.groupingBy(StudentData::getYear, LinkedHashMap::new,
                        Collectors.counting()));

        Map<Integer, Long> passingElaUniqueStudentsByYear = elaAssessedUniqueStudents.stream()
                .filter(StudentData::isElaPassing)
                .collect(Collectors.groupingBy(StudentData::getYear, LinkedHashMap::new,
                        Collectors.counting()));

        Map<Integer, Double> passRateByYear = new LinkedHashMap<>();
        totalElaAssessedUniqueStudentsByYear.forEach((year, totalCount) -> {
            if (totalCount > 0) {
                long passingCount = passingElaUniqueStudentsByYear.getOrDefault(year, 0L);
                double rate = (double) passingCount / totalCount * 100.0;
                passRateByYear.put(year, Math.round(rate * 100.0) / 100.0);
            } else {
                passRateByYear.put(year, 0.0);
            }
        });
        return passRateByYear;
    }

    public Map<Integer, Map<String, Double>> calculateAverageOverallScaleScoreByDemographicByYear(List<StudentData> allUnpivotedData, String demographicType) {
        if (allUnpivotedData == null || allUnpivotedData.isEmpty()) return new LinkedHashMap<>();
        List<StudentData> uniqueStudentEntries = getUniqueStudentYearEntries(allUnpivotedData);

        Map<Integer, Map<String, Double>> result = new LinkedHashMap<>();
        uniqueStudentEntries.stream()
                .collect(Collectors.groupingBy(StudentData::getYear, LinkedHashMap::new,
                        Collectors.groupingBy(sd -> {
                            String demographicValue = "Unknown";
                            switch (demographicType.toLowerCase()) {
                                case "ethnicity": demographicValue = sd.getEthnicity(); break;
                                case "gender": demographicValue = sd.getGender(); break;
                                case "gradelevel": demographicValue = sd.getGradeLevel(); break;
                                case "overallperformance_csv": demographicValue = sd.getOverallPerformanceCsv(); break;
                                case "riseproficiency_ela": demographicValue = sd.getRiseElaProficiencyLevel(); break;
                                case "mathproficiency": demographicValue = sd.getMathProficiencyLevel(); break;
                                case "ell": demographicValue = sd.isEll() ? "Yes" : "No"; break;
                                case "specialed": demographicValue = sd.isSpecialEd() ? "Yes" : "No"; break;
                            }
                            return demographicValue != null ? demographicValue : "Unknown";
                        }, LinkedHashMap::new, Collectors.averagingDouble(StudentData::getScaleScore))
                ))
                .forEach((year, demographicMap) -> {
                    Map<String, Double> roundedMap = new LinkedHashMap<>();
                    demographicMap.forEach((key, value) -> roundedMap.put(key, Math.round(value * 100.0)/100.0));
                    if(!roundedMap.isEmpty()) result.put(year, roundedMap);
                });
        return result;
    }
}