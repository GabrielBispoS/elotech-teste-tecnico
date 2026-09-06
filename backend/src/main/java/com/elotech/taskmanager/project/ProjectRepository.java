package com.elotech.taskmanager.project;

import com.elotech.taskmanager.project.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}
