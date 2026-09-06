package com.elotech.taskmanager.task;

import com.elotech.taskmanager.project.ProjectAccessService;
import com.elotech.taskmanager.task.dto.ProjectReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskReportService {

    private final ProjectAccessService accessService;
    private final ProjectReportCache reportCache;

    public ProjectReportResponse report(Long projectId, Long actorId) {
        accessService.requireMember(projectId, actorId);
        return reportCache.byProject(projectId);
    }
}
