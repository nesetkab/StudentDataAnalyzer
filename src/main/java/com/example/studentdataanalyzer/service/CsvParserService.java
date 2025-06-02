// --- CSV Parsing Service ---
// File: src/main/java/com/example/studentdataanalyzer/service/CsvParserService.java
package com.example.studentdataanalyzer.service;

import com.example.studentdataanalyzer.model.StudentData;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class CsvParserService {

    private static final Logger LOGGER = Logger.getLogger(CsvParserService.class.getName());
    private static final double DEFAULT_PASSING_THRESHOLD = 1500.0; // Example: Adjust if scale score has a defined passing mark

    // Define CSV headers
    private static final String HEADER_STUDENT_ID = "Student ID";
    private static final String HEADER_STUDENT_NAME_RAW = "Student Name";
    private static final String HEADER_GRADE = "Grade";
    private static final String HEADER_SPECIAL_ED = "Special Ed";
    private static final String HEADER_SCALE_SCORE = "Scale Score";
    private static final String HEADER_OVERALL_PERFORMANCE = "Performance"; // Overall performance
    private static final String HEADER_ETHNICITY = "Ethnicity";
    private static final String HEADER_GENDER = "Gender";

    // These columns will be unpivoted into "subject" and "performanceLevel"
    private static final String SUBJECT_LANGUAGE_PERFORMANCE = "Language Performance";
    private static final String SUBJECT_LISTENING_PERFORMANCE = "Listening Comprehension Performance";
    private static final String SUBJECT_READING_INFO_PERFORMANCE = "Reading Informational Text Performance";
    private static final String SUBJECT_READING_LIT_PERFORMANCE = "Reading Literature Performance";

    private static final List<String> SUBJECT_PERFORMANCE_COLUMNS = Arrays.asList(
            SUBJECT_LANGUAGE_PERFORMANCE,
            SUBJECT_LISTENING_PERFORMANCE,
            SUBJECT_READING_INFO_PERFORMANCE,
            SUBJECT_READING_LIT_PERFORMANCE
    );

    public List<StudentData> parseCsv(MultipartFile file, int yearFromUser) throws IOException, IllegalArgumentException {
        List<StudentData> studentDataList = new ArrayList<>();

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty.");
        }

        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(fileReader,
                     CSVFormat.DEFAULT.builder()
                             .setHeader()
                             .setSkipHeaderRecord(true)
                             .setTrim(true)
                             .setIgnoreEmptyLines(true)
                             .build())) {

            Map<String, Integer> headerMap = csvParser.getHeaderMap();
            if (headerMap == null || headerMap.isEmpty()) {
                throw new IllegalArgumentException("CSV file is missing a header row or headers could not be parsed.");
            }
            LOGGER.info("CSV Headers found: " + headerMap.keySet());

            // Define all expected headers for validation
            List<String> requiredHeaders = new ArrayList<>(Arrays.asList(
                    HEADER_STUDENT_ID, HEADER_STUDENT_NAME_RAW, HEADER_GRADE, HEADER_SPECIAL_ED,
                    HEADER_SCALE_SCORE, HEADER_OVERALL_PERFORMANCE, HEADER_ETHNICITY, HEADER_GENDER
            ));
            requiredHeaders.addAll(SUBJECT_PERFORMANCE_COLUMNS); // Add subject performance columns to required headers

            for (String requiredHeader : requiredHeaders) {
                if (!headerMap.containsKey(requiredHeader)) {
                    throw new IllegalArgumentException("CSV file is missing the required header: '" + requiredHeader + "'. Please ensure the CSV format is correct.");
                }
            }

            for (CSVRecord csvRecord : csvParser) {
                try {
                    // Common data from the CSV row
                    String studentId = csvRecord.get(HEADER_STUDENT_ID);
                    String rawStudentName = csvRecord.get(HEADER_STUDENT_NAME_RAW);
                    String formattedStudentName = rawStudentName;
                    if (rawStudentName != null && rawStudentName.contains(",")) {
                        String[] nameParts = rawStudentName.split(",", 2);
                        String lastName = nameParts[0].trim();
                        String firstName = (nameParts.length > 1) ? nameParts[1].trim() : "";
                        formattedStudentName = (!firstName.isEmpty() ? firstName + " " : "") + lastName;
                    } else if (rawStudentName != null) {
                        formattedStudentName = rawStudentName.trim();
                    }

                    String gradeLevel = csvRecord.get(HEADER_GRADE);
                    double scaleScore = Double.parseDouble(csvRecord.get(HEADER_SCALE_SCORE));
                    String specialEdStr = csvRecord.get(HEADER_SPECIAL_ED).toLowerCase();
                    boolean specialEd = "yes".equals(specialEdStr) || "true".equals(specialEdStr) || "1".equals(specialEdStr);
                    String overallPerformance = csvRecord.get(HEADER_OVERALL_PERFORMANCE);
                    String ethnicity = csvRecord.get(HEADER_ETHNICITY);
                    String gender = csvRecord.get(HEADER_GENDER);

                    // Unpivot: Create a StudentData object for each subject performance column
                    for (String subjectColumnName : SUBJECT_PERFORMANCE_COLUMNS) {
                        String performanceLevelForSubject = csvRecord.get(subjectColumnName);

                        studentDataList.add(new StudentData(
                                studentId, formattedStudentName, gradeLevel, specialEd, scaleScore,
                                overallPerformance, ethnicity, gender, yearFromUser, // user-entered year
                                subjectColumnName, // The "subject" is the name of the performance column
                                performanceLevelForSubject, // The "score" for this subject is its performance level
                                DEFAULT_PASSING_THRESHOLD
                        ));
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Error parsing numeric value (e.g., 'Scale Score') at record " + csvRecord.getRecordNumber() + ": " + e.getMessage(), e);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Error accessing data at record " + csvRecord.getRecordNumber() + ": " + e.getMessage(), e);
                }
            }
        }
        LOGGER.info("Successfully parsed " + studentDataList.size() + " unpivoted student-subject records from CSV.");
        return studentDataList;
    }
}