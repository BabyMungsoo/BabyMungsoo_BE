package com.example.babymungsoo.triage.repository;

import com.example.babymungsoo.triage.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findAllByOrderByOrderNoAsc();

    List<Question> findBySymptomCategoryOrderByOrderNoAsc(String symptomCategory);
}
