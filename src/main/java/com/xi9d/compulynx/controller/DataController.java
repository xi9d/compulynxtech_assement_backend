package com.xi9d.compulynx.controller;

import com.xi9d.compulynx.entity.Student;
import com.xi9d.compulynx.service.DataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/data")
@CrossOrigin(
    origins = "http://localhost:4200",
    allowCredentials = "true"
)
@RequiredArgsConstructor
@Slf4j
public class DataController {
    
    private final DataService dataService;
    
    @PostMapping("/generate-excel")
    public ResponseEntity<Map<String, Object>> generateExcelFile(@RequestParam int recordCount) {
        Map<String, Object> response = new HashMap<>();
        try {
            String fileName = dataService.generateExcelFile(recordCount);
            response.put("success", true);
            response.put("message", "Excel file generated successfully");
            response.put("fileName", fileName);
            response.put("recordCount", recordCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating Excel file", e);
            response.put("success", false);
            response.put("message", "Error generating Excel file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping("/process-excel")
    public ResponseEntity<Map<String, Object>> processExcelToCsv(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "Please select a file");
                return ResponseEntity.badRequest().body(response);
            }
            
            String csvFileName = dataService.processExcelToCsv(file);
            response.put("success", true);
            response.put("message", "Excel file processed to CSV successfully");
            response.put("csvFileName", csvFileName);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing Excel file", e);
            response.put("success", false);
            response.put("message", "Error processing Excel file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping("/upload-csv")
    public ResponseEntity<Map<String, Object>> uploadCsvToDatabase(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "Please select a CSV file");
                return ResponseEntity.badRequest().body(response);
            }
            
            dataService.uploadCsvToDatabase(file);
            response.put("success", true);
            response.put("message", "CSV data uploaded to database successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error uploading CSV to database", e);
            response.put("success", false);
            response.put("message", "Error uploading CSV to database: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/students")
    public ResponseEntity<Map<String, Object>> getStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String className) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Student> studentsPage = dataService.getStudentsWithFilters(studentId, className, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", studentsPage.getContent());
            response.put("totalElements", studentsPage.getTotalElements());
            response.put("totalPages", studentsPage.getTotalPages());
            response.put("currentPage", studentsPage.getNumber());
            response.put("size", studentsPage.getSize());
            response.put("first", studentsPage.isFirst());
            response.put("last", studentsPage.isLast());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching students", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching students: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/students/export")
    public ResponseEntity<byte[]> exportStudents(
            @RequestParam String format,
            @RequestParam(required = false) String className) {
        
        try {
            List<Student> students = dataService.getStudentsByClass(className);
            
            HttpHeaders headers = new HttpHeaders();
            byte[] data;
            
            switch (format.toLowerCase()) {
                case "excel":
                    data = dataService.exportToExcel(students);
                    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                    headers.setContentDispositionFormData("attachment", "students_report.xlsx");
                    break;
                case "csv":
                    data = dataService.exportToCsv(students);
                    headers.setContentType(MediaType.TEXT_PLAIN);
                    headers.setContentDispositionFormData("attachment", "students_report.csv");
                    break;
                case "pdf":
                    data = dataService.exportToPdf(students);
                    headers.setContentType(MediaType.APPLICATION_PDF);
                    headers.setContentDispositionFormData("attachment", "students_report.pdf");
                    break;
                default:
                    return ResponseEntity.badRequest().build();
            }
            TODO : //remember to add data as a parameter
            return new ResponseEntity<>( data, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error exporting students", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/classes")
    public ResponseEntity<String[]> getClasses() {
        String[] classes = {"Class1", "Class2", "Class3", "Class4", "Class5"};
        return ResponseEntity.ok(classes);
    }
}