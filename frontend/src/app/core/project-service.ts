import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Member, Project, ProjectDetail, ProjectRole } from './models';

@Injectable({ providedIn: 'root' })
export class ProjectService {
  private readonly http = inject(HttpClient);

  list(): Observable<Project[]> {
    return this.http.get<Project[]>('/api/projects');
  }

  findById(projectId: number): Observable<ProjectDetail> {
    return this.http.get<ProjectDetail>(`/api/projects/${projectId}`);
  }

  create(name: string, description: string | null): Observable<Project> {
    return this.http.post<Project>('/api/projects', { name, description });
  }

  addMember(projectId: number, email: string, role: ProjectRole): Observable<Member> {
    return this.http.post<Member>(`/api/projects/${projectId}/members`, { email, role });
  }
}
