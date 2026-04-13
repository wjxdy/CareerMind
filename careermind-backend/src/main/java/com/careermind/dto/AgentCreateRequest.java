package com.careermind.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AgentCreateRequest {
    @NotBlank(message = "角色名称不能为空")
    private String name;

    private String type;  // AgentType enum name

    private String systemPrompt;

    private String modelType;

    private String description;
}
