package com.xi9d.compulynx.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import com.xi9d.compulynx.entity.Student;
import com.xi9d.compulynx.repository.StudentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
@Service
@RequiredArgsConstructor
@Slf4j
public class DataService {
    
    private final StudentRepository studentRepository;
    
    private static final String[] CLASS_OPTIONS = {"Class1", "Class2", "Class3", "Class4", "Class5"};
    private static final String WINDOWS_BASE_PATH = "C:\\var\\log\\applications\\API\\dataprocessing\\";
    private static final String LINUX_BASE_PATH = "/var/log/applications/API/dataprocessing/";
    private static final String LOCAL_BASE_PATH   = "logs/applications/API/dataprocessing/";



    public String generateExcelFile(int recordCount) throws IOException {
        String fileName = "students_" + System.currentTimeMillis() + ".xlsx";
        String filePath = getFilePath(fileName);
        
        // Create directories if they don't exist
        Path path = Paths.get(filePath).getParent();
        Files.createDirectories(path);
        
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fileOut = new FileOutputStream(filePath)) {
            
            Sheet sheet = workbook.createSheet("Students");
            
            // Create header style once
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"studentId", "firstName", "lastName", "DOB", "class", "score"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Create data rows
            for (int i = 1; i <= recordCount; i++) {
                Row row = sheet.createRow(i);
                
                row.createCell(0).setCellValue(i); // studentId
                row.createCell(1).setCellValue(generateRandomString(3, 8)); // firstName
                row.createCell(2).setCellValue(generateRandomString(3, 8)); // lastName
                row.createCell(3).setCellValue(generateRandomDate().toString()); // DOB
                row.createCell(4).setCellValue(CLASS_OPTIONS[ThreadLocalRandom.current().nextInt(CLASS_OPTIONS.length)]); // class
                row.createCell(5).setCellValue(ThreadLocalRandom.current().nextInt(55, 76)); // score
                
                if (i % 10000 == 0) {
                    log.info("Generated {} records", i);
                }
            }
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(fileOut);
        }
        
        log.info("Excel file generated successfully: {}", filePath);
        return fileName;
    }
    public List<Student> getAllStudentsFromCsv() {
    List<Student> students = new ArrayList<>();
    
    // Look for the most recent CSV file in the logs directory
    String csvFilePath = findMostRecentCsvFile();
    
    if (csvFilePath == null) {
        log.warn("No CSV file found in logs directory");
        return students; // Return empty list
    }
    
    try (FileReader fileReader = new FileReader(csvFilePath);
         CSVReader csvReader = new CSVReader(fileReader)) {
        
        String[] record;
        boolean isFirstRow = true;
        int recordCount = 0;
        
        try {
            while ((record = csvReader.readNext()) != null) {
                // Skip header row
                if (isFirstRow) {
                    isFirstRow = false;
                    log.info("Reading from CSV file: {}, Header: {}", csvFilePath, Arrays.toString(record));
                    continue;
                }
                
                recordCount++;
                
                if (record.length >= 6) {
                    try {
                        Long studentId = Long.parseLong(record[0].trim());
                        String firstName = record[1].trim();
                        String lastName = record[2].trim();
                        LocalDate dob = LocalDate.parse(record[3].trim());
                        String className = record[4].trim();
                        Integer score = Integer.parseInt(record[5].trim());
                        
                        Student student = new Student(studentId, firstName, lastName, dob, className, score);
                        students.add(student);
                        
                    } catch (Exception e) {
                        log.warn("Error parsing record {}: {} - Record: {}", recordCount, e.getMessage(), Arrays.toString(record));
                    }
                }
            }
        } catch (CsvValidationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        log.info("Successfully read {} students from CSV file: {}", students.size(), csvFilePath);
        
    } catch (IOException e) {
        log.error("Error reading CSV file: {}", e.getMessage(), e);
    }
    
    // Sort by studentId to maintain consistent ordering
    students.sort((s1, s2) -> Long.compare(s1.getStudentId(), s2.getStudentId()));
    
    return students;
}

private String findMostRecentCsvFile() {
    try {
        Path logsDir = Paths.get(LOCAL_BASE_PATH);
        
        if (!Files.exists(logsDir)) {
            log.warn("Logs directory does not exist: {}", logsDir);
            return null;
        }
        
        // Find all CSV files and get the most recent one
        Optional<Path> mostRecentFile = Files.list(logsDir)
                .filter(path -> path.toString().toLowerCase().endsWith(".csv"))
                .max((path1, path2) -> {
                    try {
                        return Files.getLastModifiedTime(path1).compareTo(Files.getLastModifiedTime(path2));
                    } catch (IOException e) {
                        log.error("Error comparing file times", e);
                        return 0;
                    }
                });
        
        if (mostRecentFile.isPresent()) {
            String filePath = mostRecentFile.get().toString();
            log.info("Found most recent CSV file: {}", filePath);
            return filePath;
        } else {
            log.warn("No CSV files found in directory: {}", logsDir);
            return null;
        }
        
    } catch (IOException e) {
        log.error("Error accessing logs directory: {}", e.getMessage(), e);
        return null;
    }
}
    public String processExcelToCsv(MultipartFile file) throws IOException {
        String csvFileName = "processed_" + System.currentTimeMillis() + ".csv";
        String csvFilePath = getFilePath(csvFileName);
        
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream);
             FileWriter writer = new FileWriter(csvFilePath);
             CSVWriter csvWriter = new CSVWriter(writer)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            
            // Write header
            Row headerRow = sheet.getRow(0);
            String[] headers = new String[headerRow.getLastCellNum()];
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                headers[i] = getCellValueAsString(headerRow.getCell(i));
            }
            csvWriter.writeNext(headers);
            
            // Process data rows
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;
                
                String[] rowData = new String[headers.length];
                for (int cellIndex = 0; cellIndex < headers.length; cellIndex++) {
                    Cell cell = row.getCell(cellIndex);
                    String cellValue = getCellValueAsString(cell);
                    
                    // Add 10 to score (assuming score is in column 5)
                    if (cellIndex == 5 && cellValue != null && !cellValue.isEmpty()) {
                        try {
                            int score = Integer.parseInt(cellValue);
                            rowData[cellIndex] = String.valueOf(score + 10);
                        } catch (NumberFormatException e) {
                            rowData[cellIndex] = cellValue;
                        }
                    } else {
                        rowData[cellIndex] = cellValue;
                    }
                }
                csvWriter.writeNext(rowData);
                
                if (rowIndex % 10000 == 0) {
                    log.info("Processed {} rows to CSV", rowIndex);
                }
            }
        }
        
        log.info("CSV file processed successfully: {}", csvFilePath);
        return csvFileName;
    }
  
public void uploadCsvToDatabase(MultipartFile file) throws IOException {
    log.info("Starting CSV upload process. File: {}, Size: {} bytes", file.getOriginalFilename(), file.getSize());
    
    try (InputStream inputStream = file.getInputStream();
         InputStreamReader reader = new InputStreamReader(inputStream);
         CSVReader csvReader = new CSVReader(reader)) {
        
        List<Student> students = new ArrayList<>();
        String[] record;
        int recordCount = 0;
        int successfulRecords = 0;
        boolean isFirstRow = true;
        
        // Read records one by one instead of loading all into memory
        while ((record = csvReader.readNext()) != null) {
            // Skip header row
            if (isFirstRow) {
                isFirstRow = false;
                log.info("Header row: {}", Arrays.toString(record));
                continue;
            }
            
            recordCount++;
            log.debug("Processing record {}: {}", recordCount, Arrays.toString(record));
            
            if (record.length >= 6) {
                try {
                    Long studentId = Long.parseLong(record[0].trim());
                    String firstName = record[1].trim();
                    String lastName = record[2].trim();
                    LocalDate dob = LocalDate.parse(record[3].trim());
                    String className = record[4].trim();
                    Integer score = Integer.parseInt(record[5].trim()) + 5; // Add 5 to score
                    
                    // Debug log for first few records
                    if (recordCount <= 5) {
                        log.info("Parsed record {}: ID={}, Name={} {}, DOB={}, Class={}, Score={}", 
                               recordCount, studentId, firstName, lastName, dob, className, score);
                    }
                    
                    // Create student object - check if constructor exists
                    Student student = new Student();
                    student.setStudentId(studentId);
                    student.setFirstName(firstName);
                    student.setLastName(lastName);
                    student.setDob(dob);
                    student.setClassName(className);
                    student.setScore(score);
                    
                    students.add(student);
                    successfulRecords++;
                    
                    // Save in batches to avoid memory issues
                    if (students.size() >= 1000) {
                        try {
                            studentRepository.saveAll(students);
                            log.info("Saved batch of 1000 students to database. Total processed: {}", recordCount);
                            students.clear();
                        } catch (Exception e) {
                            log.error("Error saving batch to database: {}", e.getMessage(), e);
                            throw e; // Re-throw to stop processing
                        }
                    }
                } catch (NumberFormatException e) {
                    log.warn("Error parsing numbers in record {}: {} - Record: {}", recordCount, e.getMessage(), Arrays.toString(record));
                } catch (java.time.format.DateTimeParseException e) {
                    log.warn("Error parsing date in record {}: {} - Record: {}", recordCount, e.getMessage(), Arrays.toString(record));
                } catch (Exception e) {
                    log.error("Unexpected error processing record {}: {} - Record: {}", recordCount, e.getMessage(), Arrays.toString(record), e);
                }
            } else {
                log.warn("Record {} has insufficient columns ({}), expected 6 - Record: {}", 
                        recordCount, record.length, Arrays.toString(record));
            }
        }
        
        // Save remaining students
        if (!students.isEmpty()) {
            try {
                studentRepository.saveAll(students);
                log.info("Saved final batch of {} students to database", students.size());
            } catch (Exception e) {
                log.error("Error saving final batch to database: {}", e.getMessage(), e);
                throw e;
            }
        }
        
        log.info("CSV data uploaded to database successfully. Total records processed: {}, Successful: {}", 
                recordCount, successfulRecords);
                
    } catch (Exception e) {
        log.error("Error during CSV upload process: {}", e.getMessage(), e);
    
    }
}
    public Page<Student> getStudentsWithFilters(Long studentId, String className, Pageable pageable) {
        return studentRepository.findStudentsWithFilters(studentId, className, pageable);
    }
    
    public List<Student> getAllStudents() {
    // First try to read from CSV file
    List<Student> studentsFromCsv = getAllStudentsFromCsv();
    
    // If CSV file has data, return it
    if (!studentsFromCsv.isEmpty()) {
        log.info("Returning {} students from CSV file", studentsFromCsv.size());
        return studentsFromCsv;
    }
    
    // Fallback to database if no CSV data found
    log.info("No CSV data found, falling back to database");
    return studentRepository.findAllByOrderByStudentIdAsc();
}
    
    public List<Student> getStudentsByClassFromCsv(String className) {
    List<Student> allStudents = getAllStudentsFromCsv();
    
    if (className == null || className.isEmpty()) {
        return allStudents;
    }
    
    return allStudents.stream()
            .filter(student -> className.equals(student.getClassName()))
            .sorted((s1, s2) -> Long.compare(s1.getStudentId(), s2.getStudentId()))
            .collect(java.util.stream.Collectors.toList());
}

public List<Student> getStudentsByClass(String className) {
    // Try CSV first
    List<Student> studentsFromCsv = getStudentsByClassFromCsv(className);
    
    if (!studentsFromCsv.isEmpty() || getAllStudentsFromCsv().isEmpty() == false) {
        return studentsFromCsv;
    }
    
    // Fallback to database
    if (className == null || className.isEmpty()) {
        return getAllStudents();
    }
    return studentRepository.findByClassNameOrderByStudentIdAsc(className);
}
    
    public byte[] exportToExcel(List<Student> students) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("Students Report");
            
            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            // Create header
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Student ID", "First Name", "Last Name", "DOB", "Class", "Score"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Create data rows
            for (int i = 0; i < students.size(); i++) {
                Student student = students.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(student.getStudentId());
                row.createCell(1).setCellValue(student.getFirstName());
                row.createCell(2).setCellValue(student.getLastName());
                row.createCell(3).setCellValue(student.getDob().toString());
                row.createCell(4).setCellValue(student.getClassName());
                row.createCell(5).setCellValue(student.getScore());
            }
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
    
    public byte[] exportToCsv(List<Student> students) throws IOException {
        try (StringWriter writer = new StringWriter();
             CSVWriter csvWriter = new CSVWriter(writer)) {
            
            // Write header
            String[] headers = {"Student ID", "First Name", "Last Name", "DOB", "Class", "Score"};
            csvWriter.writeNext(headers);
            
            // Write data
            for (Student student : students) {
                String[] data = {
                    student.getStudentId().toString(),
                    student.getFirstName(),
                    student.getLastName(),
                    student.getDob().toString(),
                    student.getClassName(),
                    student.getScore().toString()
                };
                csvWriter.writeNext(data);
            }
            
            return writer.toString().getBytes();
        }
    }
    
    public byte[] exportToPdf(List<Student> students) throws DocumentException, IOException {
    Document document = new Document();
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
        PdfWriter.getInstance(document, outputStream);
        document.open();
        
        // Add title
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Paragraph title = new Paragraph("Student Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph("\n"));
        
        // Create table
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        
        // Add headers
        String[] headers = {"Student ID", "First Name", "Last Name", "DOB", "Class", "Score"};
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
        
        // Add data
        for (Student student : students) {
            table.addCell(student.getStudentId().toString());
            table.addCell(student.getFirstName());
            table.addCell(student.getLastName());
            table.addCell(student.getDob().toString());
            table.addCell(student.getClassName());
            table.addCell(student.getScore().toString());
        }
        
        document.add(table);
        document.close();
        
        return outputStream.toByteArray();
    }
}
    
    private String generateRandomString(int minLength, int maxLength) {
        int length = ThreadLocalRandom.current().nextInt(minLength, maxLength + 1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            char c = (char) (ThreadLocalRandom.current().nextInt(26) + 'a');
            sb.append(c);
        }
        return sb.toString();
    }
    
    private LocalDate generateRandomDate() {
        LocalDate start = LocalDate.of(2000, 1, 1);
        LocalDate end = LocalDate.of(2010, 12, 31);
        long startEpochDay = start.toEpochDay();
        long endEpochDay = end.toEpochDay();
        long randomDay = ThreadLocalRandom.current().nextLong(startEpochDay, endEpochDay + 1);
        return LocalDate.ofEpochDay(randomDay);
    }
    
   private String getFilePath(String fileName) {
    String os = System.getProperty("os.name").toLowerCase();

    Path basePath;
    if (os.contains("win")) {
        basePath = Paths.get(WINDOWS_BASE_PATH);
    } else {
        basePath = Paths.get(LINUX_BASE_PATH);
    }

    try {
        // Try creating system directory
        Files.createDirectories(basePath);
    } catch (IOException e) {
        // Fallback to local logs directory inside project
        basePath = Paths.get(LOCAL_BASE_PATH);
        try {
            Files.createDirectories(basePath);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to create local fallback directory: " + basePath, ex);
        }
    }

    return basePath.resolve(fileName).toString();
}
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}