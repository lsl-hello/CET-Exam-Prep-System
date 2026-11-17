package com.cet6.dto;

import lombok.Data;

@Data
public class AnswerSubmitDTO {

    private Long questionId;
    private String userAnswer;
}
