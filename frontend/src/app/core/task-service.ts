import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { Page, Task, TaskPayload, TaskStatus } from './models';

const BOARD_PAGE_SIZE = 200;

@Injectable({ providedIn: 'root' })
export class TaskService {
  private readonly http = inject(HttpClient);

  listByProject(projectId: number): Observable<Task[]> {
    return this.http
      .get<Page<Task>>(`/api/projects/${projectId}/tasks`, { params: { size: BOARD_PAGE_SIZE } })
      .pipe(map((page) => page.content));
  }

  create(projectId: number, payload: TaskPayload): Observable<Task> {
    return this.http.post<Task>(`/api/projects/${projectId}/tasks`, payload);
  }

  update(projectId: number, taskId: number, payload: TaskPayload): Observable<Task> {
    return this.http.put<Task>(`/api/projects/${projectId}/tasks/${taskId}`, payload);
  }

  remove(projectId: number, taskId: number): Observable<void> {
    return this.http.delete<void>(`/api/projects/${projectId}/tasks/${taskId}`);
  }

  changeStatus(taskId: number, status: TaskStatus): Observable<Task> {
    return this.http.patch<Task>(`/api/tasks/${taskId}/status`, { status });
  }
}
