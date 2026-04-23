package com.careermind.enums;

public enum AgentType {
    // 职业团
    INDUSTRY_ANALYST("行业分析师", "Industry Analyst", "CAREER"),
    SKILL_ASSESSOR("能力评估师", "Skill Assessor", "CAREER"),
    RISK_WATCHER("风险警示者", "Risk Watcher", "CAREER"),
    OPPORTUNITY_HUNTER("机会挖掘者", "Opportunity Hunter", "CAREER"),
    VALUE_EXAMINER("价值观拷问者", "Value Examiner", "CAREER"),

    // 法律团
    CONTRACT_REVIEWER("合同审查师", "Contract Reviewer", "LEGAL"),
    LITIGATION_ANALYST("诉讼风险师", "Litigation Analyst", "LEGAL"),
    RIGHTS_DEFENDER("权益维护者", "Rights Defender", "LEGAL"),
    PRACTICAL_COUNSEL("实务执行官", "Practical Counsel", "LEGAL"),
    MEDIATION_ADVISOR("调解智者", "Mediation Advisor", "LEGAL"),

    // 特殊
    MERGE_AGENT("整合专家", "Merge Agent", "SYSTEM"),
    CUSTOM("自定义", "Custom", "CUSTOM");

    private final String chineseName;
    private final String englishName;
    private final String category;

    AgentType(String chineseName, String englishName, String category) {
        this.chineseName = chineseName;
        this.englishName = englishName;
        this.category = category;
    }

    public String getChineseName() { return chineseName; }
    public String getEnglishName() { return englishName; }
    public String getCategory()    { return category; }
}
