package com.careermind.service;

import com.careermind.dto.ReportResponse;

public interface ReportService {
    ReportResponse build(Long taskId, boolean refreshExtras);
}
