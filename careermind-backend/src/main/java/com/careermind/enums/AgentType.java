package com.careermind.enums;

public enum AgentType {
    INDUSTRY_ANALYST("行业分析师", "Industry Analyst"),
    SKILL_ASSESSOR("能力评估师", "Skill Assessor"),
    RISK_WATCHER("风险警示者", "Risk Watcher"),
    OPPORTUNITY_HUNTER("机会挖掘者", "Opportunity Hunter"),
    VALUE_EXAMINER("价值观拷问者", "Value Examiner"),
    MERGE_AGENT("整合专家", "Merge Agent"),
    CUSTOM("自定义", "Custom");

    private final String chineseName;
    private final String englishName;

    AgentType(String chineseName, String englishName) {
        this.chineseName = chineseName;
        this.englishName = englishName;
    }

    public String getChineseName() {
        return chineseName;
    }

    public String getEnglishName() {
        return englishName;
    }
}
