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
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;


enum DataType { ELA, MATH, UNKNOWN, BOTH }

@Service
public class CsvParserService {

    private static final Logger LOGGER = Logger.getLogger(CsvParserService.class.getName());

    // Common CSV Headers
    private static final String HEADER_STUDENT_ID = "Student ID";
    private static final String HEADER_STUDENT_NAME_RAW = "Student Name";
    private static final String HEADER_GRADE = "Grade";
    private static final String HEADER_ELL = "ELL";
    private static final String HEADER_SPECIAL_ED = "Special Ed";
    private static final String HEADER_SCALE_SCORE = "Scale Score";
    private static final String HEADER_OVERALL_PERFORMANCE_CSV = "Performance";
    private static final String HEADER_ETHNICITY = "Ethnicity";
    private static final String HEADER_GENDER = "Gender";

    // ELA Performance Area Columns
    private static final String ELA_SUBJECT_LANGUAGE_PERFORMANCE = "Language Performance";
    private static final String ELA_SUBJECT_LISTENING_PERFORMANCE = "Listening Comprehension Performance";
    private static final String ELA_SUBJECT_READING_INFO_PERFORMANCE = "Reading Informational Text Performance";
    private static final String ELA_SUBJECT_READING_LIT_PERFORMANCE = "Reading Literature Performance";
    private static final List<String> ELA_PERFORMANCE_COLUMNS = Arrays.asList(
            ELA_SUBJECT_LANGUAGE_PERFORMANCE, ELA_SUBJECT_LISTENING_PERFORMANCE,
            ELA_SUBJECT_READING_INFO_PERFORMANCE, ELA_SUBJECT_READING_LIT_PERFORMANCE
    );

    // Math Performance Area Columns
    private static final String MATH_SUBJECT_EXPRESSIONS_EQUATIONS = "Expressions and Equations Performance";
    private static final String MATH_SUBJECT_FUNCTIONS = "Functions Performance";
    private static final String MATH_SUBJECT_GEOMETRY_NUM_SYSTEM = "Geometry / The Number System Performance";
    private static final String MATH_SUBJECT_STATS_PROBABILITY = "Statistics and Probability Performance";
    private static final List<String> MATH_PERFORMANCE_COLUMNS = Arrays.asList(
            MATH_SUBJECT_EXPRESSIONS_EQUATIONS, MATH_SUBJECT_FUNCTIONS,
            MATH_SUBJECT_GEOMETRY_NUM_SYSTEM, MATH_SUBJECT_STATS_PROBABILITY
    );

    private static final List<String> ALL_POSSIBLE_PERFORMANCE_COLUMNS = new ArrayList<>();
    static {
        ALL_POSSIBLE_PERFORMANCE_COLUMNS.addAll(ELA_PERFORMANCE_COLUMNS);
        ALL_POSSIBLE_PERFORMANCE_COLUMNS.addAll(MATH_PERFORMANCE_COLUMNS);
    }


    // RISE ELA Cut Scores
    private static final Map<String, Map<String, int[]>> RISE_ELA_CUT_SCORES = new HashMap<>();
    static {
        Map<String, int[]> ela3 = new LinkedHashMap<>();
        ela3.put("Below Proficient", new int[]{Integer.MIN_VALUE, 290});
        ela3.put("Approaching Proficient", new int[]{291, 333});
        ela3.put("Proficient", new int[]{334, 405});
        ela3.put("Highly Proficient", new int[]{406, Integer.MAX_VALUE});
        RISE_ELA_CUT_SCORES.put("3", ela3);
        Map<String, int[]> ela4 = new LinkedHashMap<>();
        ela4.put("Below Proficient", new int[]{Integer.MIN_VALUE, 322});
        ela4.put("Approaching Proficient", new int[]{323, 377});
        ela4.put("Proficient", new int[]{378, 441});
        ela4.put("Highly Proficient", new int[]{442, Integer.MAX_VALUE});
        RISE_ELA_CUT_SCORES.put("4", ela4);
        Map<String, int[]> ela5 = new LinkedHashMap<>();
        ela5.put("Below Proficient", new int[]{Integer.MIN_VALUE, 360});
        ela5.put("Approaching Proficient", new int[]{361, 409});
        ela5.put("Proficient", new int[]{410, 464});
        ela5.put("Highly Proficient", new int[]{465, Integer.MAX_VALUE});
        RISE_ELA_CUT_SCORES.put("5", ela5);
        Map<String, int[]> ela6 = new LinkedHashMap<>();
        ela6.put("Below Proficient", new int[]{Integer.MIN_VALUE, 393});
        ela6.put("Approaching Proficient", new int[]{394, 433});
        ela6.put("Proficient", new int[]{434, 492});
        ela6.put("Highly Proficient", new int[]{493, Integer.MAX_VALUE});
        RISE_ELA_CUT_SCORES.put("6", ela6);
        Map<String, int[]> ela7 = new LinkedHashMap<>();
        ela7.put("Below Proficient", new int[]{Integer.MIN_VALUE, 403});
        ela7.put("Approaching Proficient", new int[]{404, 449});
        ela7.put("Proficient", new int[]{450, 513});
        ela7.put("Highly Proficient", new int[]{514, Integer.MAX_VALUE});
        RISE_ELA_CUT_SCORES.put("7", ela7);
        Map<String, int[]> ela8 = new LinkedHashMap<>();
        ela8.put("Below Proficient", new int[]{Integer.MIN_VALUE, 415});
        ela8.put("Approaching Proficient", new int[]{416, 470});
        ela8.put("Proficient", new int[]{471, 532});
        ela8.put("Highly Proficient", new int[]{533, Integer.MAX_VALUE});
        RISE_ELA_CUT_SCORES.put("8", ela8);
    }

    // RISE Math Cut Scores (Updated from image)
    private static final Map<String, Map<String, int[]>> MATH_CUT_SCORES = new HashMap<>();
    static {
        // Math 3: <297 (BP), 297-316 (AP), 317-336 (P), 337+ (HP)
        Map<String, int[]> math3 = new LinkedHashMap<>();
        math3.put("Below Proficient", new int[]{Integer.MIN_VALUE, 296});
        math3.put("Approaching Proficient", new int[]{297, 316});
        math3.put("Proficient", new int[]{317, 336});
        math3.put("Highly Proficient", new int[]{337, Integer.MAX_VALUE});
        MATH_CUT_SCORES.put("3", math3);

        // Math 4: <326 (BP), 326-348 (AP), 349-375 (P), 376+ (HP)
        Map<String, int[]> math4 = new LinkedHashMap<>();
        math4.put("Below Proficient", new int[]{Integer.MIN_VALUE, 325});
        math4.put("Approaching Proficient", new int[]{326, 348});
        math4.put("Proficient", new int[]{349, 375});
        math4.put("Highly Proficient", new int[]{376, Integer.MAX_VALUE});
        MATH_CUT_SCORES.put("4", math4);

        // Math 5: <360 (BP), 360-383 (AP), 384-415 (P), 416+ (HP)
        Map<String, int[]> math5 = new LinkedHashMap<>();
        math5.put("Below Proficient", new int[]{Integer.MIN_VALUE, 359});
        math5.put("Approaching Proficient", new int[]{360, 383});
        math5.put("Proficient", new int[]{384, 415});
        math5.put("Highly Proficient", new int[]{416, Integer.MAX_VALUE});
        MATH_CUT_SCORES.put("5", math5);

        // Math 6: <397 (BP), 397-431 (AP), 432-463 (P), 464+ (HP)
        Map<String, int[]> math6 = new LinkedHashMap<>();
        math6.put("Below Proficient", new int[]{Integer.MIN_VALUE, 396});
        math6.put("Approaching Proficient", new int[]{397, 431});
        math6.put("Proficient", new int[]{432, 463});
        math6.put("Highly Proficient", new int[]{464, Integer.MAX_VALUE});
        MATH_CUT_SCORES.put("6", math6);

        // Math 7: <415 (BP), 415-449 (AP), 450-498 (P), 499+ (HP)
        Map<String, int[]> math7 = new LinkedHashMap<>();
        math7.put("Below Proficient", new int[]{Integer.MIN_VALUE, 414});
        math7.put("Approaching Proficient", new int[]{415, 449});
        math7.put("Proficient", new int[]{450, 498});
        math7.put("Highly Proficient", new int[]{499, Integer.MAX_VALUE});
        MATH_CUT_SCORES.put("7", math7);

        // Math 8: <447 (BP), 447-498 (AP), 499-553 (P), 554+ (HP)
        Map<String, int[]> math8 = new LinkedHashMap<>();
        math8.put("Below Proficient", new int[]{Integer.MIN_VALUE, 446});
        math8.put("Approaching Proficient", new int[]{447, 498});
        math8.put("Proficient", new int[]{499, 553});
        math8.put("Highly Proficient", new int[]{554, Integer.MAX_VALUE});
        MATH_CUT_SCORES.put("8", math8);
        // Note: "Sec Math I" is not included as CSV grade format is expected as 3-8.
    }

    private String determineRiseElaProficiency(String csvGrade, double scaleScore) {
        String gradeKey = csvGrade.replaceFirst("^0+(?!$)", "");
        Map<String, int[]> cutoffsForGrade = RISE_ELA_CUT_SCORES.get(gradeKey);
        if (cutoffsForGrade == null) return "N/A (Grade not in ELA 3-8)";
        for (Map.Entry<String, int[]> entry : cutoffsForGrade.entrySet()) {
            if (scaleScore >= entry.getValue()[0] && scaleScore <= entry.getValue()[1]) return entry.getKey();
        }
        return "N/A (Score out of ELA range)";
    }

    private String determineMathProficiency(String csvGrade, double scaleScore) {
        String gradeKey = csvGrade.replaceFirst("^0+(?!$)", "");
        Map<String, int[]> cutoffsForGrade = MATH_CUT_SCORES.get(gradeKey);
        if (cutoffsForGrade == null) return "N/A (Math Grade not in 3-8)"; // Updated message
        for (Map.Entry<String, int[]> entry : cutoffsForGrade.entrySet()) {
            if (scaleScore >= entry.getValue()[0] && scaleScore <= entry.getValue()[1]) return entry.getKey();
        }
        return "N/A (Score out of Math range)";
    }

    public List<StudentData> parseCsv(MultipartFile file, int yearFromUser) throws IOException, IllegalArgumentException {
        List<StudentData> studentDataList = new ArrayList<>();
        if (file.isEmpty()) throw new IllegalArgumentException("Uploaded file is empty.");

        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(fileReader,
                     CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).setIgnoreEmptyLines(true).build())) {

            Set<String> csvHeaders = csvParser.getHeaderMap().keySet().stream().map(String::trim).collect(Collectors.toSet());
            LOGGER.info("CSV Headers found: " + csvHeaders);

            boolean hasElaPerformanceCols = ELA_PERFORMANCE_COLUMNS.stream().anyMatch(csvHeaders::contains);
            boolean hasMathPerformanceCols = MATH_PERFORMANCE_COLUMNS.stream().anyMatch(csvHeaders::contains);

            DataType detectedType;
            if (hasElaPerformanceCols && hasMathPerformanceCols) detectedType = DataType.BOTH;
            else if (hasElaPerformanceCols) detectedType = DataType.ELA;
            else if (hasMathPerformanceCols) detectedType = DataType.MATH;
            else detectedType = DataType.UNKNOWN;

            LOGGER.info("Detected CSV data type: " + detectedType);

            List<String> commonRequiredHeaders = Arrays.asList(
                    HEADER_STUDENT_ID, HEADER_STUDENT_NAME_RAW, HEADER_GRADE, HEADER_ELL, HEADER_SPECIAL_ED,
                    HEADER_SCALE_SCORE, HEADER_OVERALL_PERFORMANCE_CSV, HEADER_ETHNICITY, HEADER_GENDER
            );
            for (String requiredHeader : commonRequiredHeaders) {
                if (!csvHeaders.contains(requiredHeader)) {
                    throw new IllegalArgumentException("CSV file is missing the common required header: '" + requiredHeader + "'.");
                }
            }

            for (CSVRecord csvRecord : csvParser) {
                try {
                    String studentId = csvRecord.get(HEADER_STUDENT_ID);
                    String rawStudentName = csvRecord.get(HEADER_STUDENT_NAME_RAW);
                    String formattedStudentName = rawStudentName;
                    if (rawStudentName != null && rawStudentName.contains(",")) {
                        String[] nameParts = rawStudentName.split(",", 2);
                        formattedStudentName = (nameParts.length > 1 ? nameParts[1].trim() : "") + " " + nameParts[0].trim();
                    } else if (rawStudentName != null) {
                        formattedStudentName = rawStudentName.trim();
                    }

                    String gradeLevelFromCsv = csvRecord.get(HEADER_GRADE);
                    String ellStr = csvRecord.get(HEADER_ELL).toLowerCase();
                    boolean ell = "yes".equals(ellStr) || "true".equals(ellStr) || "1".equals(ellStr);
                    String specialEdStr = csvRecord.get(HEADER_SPECIAL_ED).toLowerCase();
                    boolean specialEd = "yes".equals(specialEdStr) || "true".equals(specialEdStr) || "1".equals(specialEdStr);
                    double scaleScore = Double.parseDouble(csvRecord.get(HEADER_SCALE_SCORE));
                    String overallPerformanceCsv = csvRecord.get(HEADER_OVERALL_PERFORMANCE_CSV);
                    String ethnicity = csvRecord.get(HEADER_ETHNICITY);
                    String gender = csvRecord.get(HEADER_GENDER);

                    String riseElaProficiency = determineRiseElaProficiency(gradeLevelFromCsv, scaleScore);
                    String mathProficiency = determineMathProficiency(gradeLevelFromCsv, scaleScore);

                    List<String> performanceColsToUnpivot = new ArrayList<>();
                    if (hasElaPerformanceCols) { // Use flag instead of detectedType for more direct check
                        ELA_PERFORMANCE_COLUMNS.stream().filter(csvHeaders::contains).forEach(performanceColsToUnpivot::add);
                    }
                    if (hasMathPerformanceCols) { // Use flag
                        MATH_PERFORMANCE_COLUMNS.stream().filter(csvHeaders::contains).forEach(performanceColsToUnpivot::add);
                    }

                    if (performanceColsToUnpivot.isEmpty()){
                        studentDataList.add(new StudentData(
                                studentId, formattedStudentName, gradeLevelFromCsv, specialEd, ell, scaleScore,
                                overallPerformanceCsv, ethnicity, gender, yearFromUser,
                                null,
                                null,
                                riseElaProficiency, mathProficiency
                        ));
                    } else {
                        for (String subjectAreaColumnName : performanceColsToUnpivot) {
                            String performanceLevelForSubjectArea = csvRecord.isMapped(subjectAreaColumnName) ? csvRecord.get(subjectAreaColumnName) : null;
                            studentDataList.add(new StudentData(
                                    studentId, formattedStudentName, gradeLevelFromCsv, specialEd, ell, scaleScore,
                                    overallPerformanceCsv, ethnicity, gender, yearFromUser,
                                    subjectAreaColumnName,
                                    performanceLevelForSubjectArea,
                                    riseElaProficiency, mathProficiency
                            ));
                        }
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Error parsing numeric value at record " + csvRecord.getRecordNumber() + ": " + e.getMessage(), e);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Error accessing data at record " + csvRecord.getRecordNumber() + ": " + e.getMessage(), e);
                }
            }
        }
        LOGGER.info("Successfully parsed " + studentDataList.size() + " unpivoted student-subject records from CSV.");
        return studentDataList;
    }
}