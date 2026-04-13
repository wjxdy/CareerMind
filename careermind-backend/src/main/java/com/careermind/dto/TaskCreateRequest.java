package com.careermind.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class TaskCreateRequest {
    @NotBlank(message = "标题不能为空")
    private String title;

    private String background;

    private String goal;

    private String constraints;

    @NotEmpty(message = "至少需要选择一个Agent")
    private List<Long> agentIds;
}
