import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { Page, Task, TaskAuditLog, TaskPayload, TaskQuery, TaskStatus } from './models';

const BOARD_PAGE_SIZE = 200;

@Injectable({ providedIn: 'root' })
export class TaskService {
  private readonly http = inject(HttpClient);

  /**
   * Busca textual e filtros sao endpoints distintos na API: com termo informado o board consulta
   * /tasks/search, sem termo usa a listagem com filtros, ordenacao e paginacao.
   */
  listByProject(projectId: number, query: TaskQuery): Observable<Task[]> {
    const term = query.search.trim();
    return term
      ? this.fetchPage(`/api/projects/${projectId}/tasks/search`, new HttpParams().set('q', term))
      : this.fetchPage(`/api/projects/${projectId}/tasks`, toHttpParams(query));
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

  auditLog(projectId: number, taskId: number): Observable<TaskAuditLog[]> {
    return this.http.get<TaskAuditLog[]>(`/api/projects/${projectId}/tasks/${taskId}/audit`);
  }

  changeStatus(taskId: number, status: TaskStatus): Observable<Task> {
    return this.http.patch<Task>(`/api/tasks/${taskId}/status`, { status });
  }

  private fetchPage(url: string, params: HttpParams): Observable<Task[]> {
    return this.http
      .get<Page<Task>>(url, { params: params.set('size', BOARD_PAGE_SIZE) })
      .pipe(map((page) => page.content));
  }
}

function toHttpParams(query: TaskQuery): HttpParams {
  const values: Record<string, string | number | null> = {
    status: query.status,
    priority: query.priority,
    assignee: query.assigneeId,
    from: query.from,
    to: query.to,
    sort: query.sort || null,
  };

  return Object.entries(values).reduce(
    (params, [key, value]) => (value === null || value === '' ? params : params.set(key, value)),
    new HttpParams(),
  );
}
