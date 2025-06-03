// --- Model Class ---
// File: src/main/java/com/example/studentdataanalyzer/model/StudentData.java
package com.example.studentdataanalyzer.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;

@Data
@NoArgsConstructor
@EqualsAndHashCode(of = {"studentId", "year"})
public class StudentData {
    // Common fields
    private String studentId;
    private String studentName;
    private String gradeLevel;
    private boolean specialEd;
    private boolean ell; // New field for ELL status
    private double scaleScore;  // Overall score
    private String overallPerformanceCsv; // From "Performance" column in CSV (overall for the scale score)
    private String ethnicity;
    private String gender;
    private int year;

    // ELA specific derived fields
    private String riseElaProficiencyLevel;
    private boolean elaPassing; // True if riseElaProficiencyLevel is "Proficient" or "Highly Proficient"

    // Math specific derived fields
    private String mathProficiencyLevel;
    private boolean mathPassing; // True if mathProficiencyLevel is "Proficient" or "Highly Proficient"

    // Fields for unpivoted subject performance areas (can be ELA or Math)
    private String subjectArea; // e.g., "Language Performance", "Expressions and Equations Performance"
    private String subjectPerformanceLevel; // Descriptive level, e.g., "At/Near Standard"

    // Constructor
    public StudentData(String studentId, String studentName, String gradeLevel, boolean specialEd, boolean ell,
                       double scaleScore, String overallPerformanceCsv, String ethnicity, String gender, int year,
                       String subjectArea, String subjectPerformanceLevel,
                       String riseElaProficiencyLevel, String mathProficiencyLevel) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.gradeLevel = gradeLevel;
        this.specialEd = specialEd;
        this.ell = ell;
        this.scaleScore = scaleScore;
        this.overallPerformanceCsv = overallPerformanceCsv;
        this.ethnicity = ethnicity;
        this.gender = gender;
        this.year = year;
        this.subjectArea = subjectArea;
        this.subjectPerformanceLevel = subjectPerformanceLevel;

        this.riseElaProficiencyLevel = riseElaProficiencyLevel;
        if (this.riseElaProficiencyLevel != null) {
            this.elaPassing = "Proficient".equalsIgnoreCase(this.riseElaProficiencyLevel) ||
                    "Highly Proficient".equalsIgnoreCase(this.riseElaProficiencyLevel);
        } else {
            this.elaPassing = false;
        }

        this.mathProficiencyLevel = mathProficiencyLevel;
        if (this.mathProficiencyLevel != null) {
            this.mathPassing = "Proficient".equalsIgnoreCase(this.mathProficiencyLevel) ||
                    "Highly Proficient".equalsIgnoreCase(this.mathProficiencyLevel);
        } else {
            this.mathPassing = false;
        }
    }
}