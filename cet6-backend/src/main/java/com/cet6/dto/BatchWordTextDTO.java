package com.cet6.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BatchWordTextDTO {
    @NotNull(message = "词书id不能为空")
    private Long bookId;
    @NotBlank(message = "单词文本不能为空")
    private String text;
}
