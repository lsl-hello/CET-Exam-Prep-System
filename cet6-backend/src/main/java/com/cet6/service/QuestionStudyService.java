package com.cet6.service;

import com.cet6.dto.AnswerSubmitDTO;

import java.util.Map;

public interface QuestionStudyService {

    Map<String, Object> submitAnswer(AnswerSubmitDTO dto);

    Map<String, Object> getWeakQuestionReport();
}
