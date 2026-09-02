package com.elotech.taskmanager.project;

import com.elotech.taskmanager.common.security.AuthenticatedUser;
import com.elotech.taskmanager.project.dto.AddMemberRequest;
import com.elotech.taskmanager.project.dto.MemberResponse;
import com.elotech.taskmanager.project.dto.ProjectDetailResponse;
import com.elotech.taskmanager.project.dto.ProjectRequest;
import com.elotech.taskmanager.project.dto.ProjectResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public List<ProjectResponse> list(@AuthenticationPrincipal AuthenticatedUser actor) {
        return projectService.listMyProjects(actor.id());
    }

    @GetMapping("/{projectId}")
    public ProjectDetailResponse findById(@PathVariable Long projectId,
                                          @AuthenticationPrincipal AuthenticatedUser actor) {
        return projectService.findById(projectId, actor.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@Valid @RequestBody ProjectRequest request,
                                  @AuthenticationPrincipal AuthenticatedUser actor) {
        return projectService.create(request, actor.id());
    }

    @PutMapping("/{projectId}")
    public ProjectResponse update(@PathVariable Long projectId,
                                  @Valid @RequestBody ProjectRequest request,
                                  @AuthenticationPrincipal AuthenticatedUser actor) {
        return projectService.update(projectId, request, actor.id());
    }

    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long projectId, @AuthenticationPrincipal AuthenticatedUser actor) {
        projectService.delete(projectId, actor.id());
    }

    @PostMapping("/{projectId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse addMember(@PathVariable Long projectId,
                                    @Valid @RequestBody AddMemberRequest request,
                                    @AuthenticationPrincipal AuthenticatedUser actor) {
        return projectService.addMember(projectId, request, actor.id());
    }

    @DeleteMapping("/{projectId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long projectId, @PathVariable Long userId,
                             @AuthenticationPrincipal AuthenticatedUser actor) {
        projectService.removeMember(projectId, userId, actor.id());
    }
}
