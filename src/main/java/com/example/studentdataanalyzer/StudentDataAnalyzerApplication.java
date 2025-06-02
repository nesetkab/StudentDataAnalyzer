package com.example.studentdataanalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StudentDataAnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudentDataAnalyzerApplication.class, args);
        System.out.println("the backend is running!");
        System.out.println("access at http://localhost:8080");
        // Default port is 8080. You can change this in application.properties if needed:
        // server.port=8081
    }
}
