package com.xi9d.compulynx.repository;

import com.xi9d.compulynx.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    
    Optional<Student> findByStudentId(Long studentId);
    
    Page<Student> findByStudentId(Long studentId, Pageable pageable);
    
    Page<Student> findByClassName(String className, Pageable pageable);
    
    @Query("SELECT s FROM Student s WHERE " +
           "(:studentId IS NULL OR s.studentId = :studentId) AND " +
           "(:className IS NULL OR s.className = :className)")
    Page<Student> findStudentsWithFilters(@Param("studentId") Long studentId,
                                        @Param("className") String className,
                                        Pageable pageable);
    
    List<Student> findByClassNameOrderByStudentIdAsc(String className);
    
    List<Student> findAllByOrderByStudentIdAsc();
}