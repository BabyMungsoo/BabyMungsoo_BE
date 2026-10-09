package com.example.babymungsoo.triage.repository;

import com.example.babymungsoo.triage.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findBySessionId(Long sessionId);
}
