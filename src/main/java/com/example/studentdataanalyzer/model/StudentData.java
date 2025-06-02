// --- Model Class ---
// File: src/main/java/com/example/studentdataanalyzer/model/StudentData.java
package com.example.studentdataanalyzer.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StudentData {
    // Fields from CSV that are common to each unpivoted record
    private String studentId;
    private String studentName; // Will be "FirstName LastName" after parsing
    private String gradeLevel;  // From "Grade" column
    private boolean specialEd;  // From "Special Ed" column
    private double scaleScore;  // Overall score for the test event
    private boolean passing;    // Derived from scaleScore
    private String overallPerformance; // From "Performance" column (overall for the scale score)
    private String ethnicity;
    private String gender;
    private int year;           // User-entered year for the dataset

    // Fields specific to the unpivoted subject
    private String subject;         // e.g., "Language Performance", "Reading Literature"
    private String performanceLevel; // The value from the subject-specific performance column (e.g., "Proficient")


    // Constructor for the unpivoted data structure
    public StudentData(String studentId, String studentName, String gradeLevel, boolean specialEd,
                       double scaleScore, String overallPerformance, String ethnicity, String gender, int year,
                       String subject, String performanceLevel,
                       double passingThreshold) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.gradeLevel = gradeLevel;
        this.specialEd = specialEd;
        this.scaleScore = scaleScore;
        this.passing = (scaleScore >= passingThreshold); // Passing based on overall scale score
        this.overallPerformance = overallPerformance;
        this.ethnicity = ethnicity;
        this.gender = gender;
        this.year = year;
        this.subject = subject; // Specific subject/assessment area
        this.performanceLevel = performanceLevel; // Performance in that specific subject
    }
}