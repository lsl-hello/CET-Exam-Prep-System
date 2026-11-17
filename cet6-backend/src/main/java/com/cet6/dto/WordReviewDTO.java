package com.cet6.dto;

import lombok.Data;

@Data
public class WordReviewDTO {

    private Long wordId;
    /** 1认识 2模糊 3遗忘 */
    private Integer memoryResult;
    /** 1新词 2复习 */
    private Integer reviewType;
}
