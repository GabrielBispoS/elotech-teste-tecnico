export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type ProjectRole = 'ADMIN' | 'MEMBER';

export interface UserSummary {
  id: number;
  name: string;
  email: string;
}

export interface LoginResponse {
  token: string;
  expiresInSeconds: number;
  userId: number;
  name: string;
  email: string;
}

export interface Project {
  id: number;
  name: string;
  description: string | null;
  ownerId: number;
  myRole: ProjectRole;
}

export interface Member extends UserSummary {
  userId: number;
  role: ProjectRole;
}

export interface ProjectDetail extends Project {
  members: Member[];
}

export interface Task {
  id: number;
  projectId: number;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  deadline: string | null;
  assignee: UserSummary | null;
  createdAt: string;
  updatedAt: string;
}

export interface TaskPayload {
  title: string;
  description: string | null;
  priority: TaskPriority;
  deadline: string | null;
  assigneeId: number | null;
}

export interface TaskAuditLog {
  id: number;
  field: string;
  oldValue: string | null;
  newValue: string | null;
  changedBy: UserSummary | null;
  changedAt: string;
}

export interface ProjectReport {
  byStatus: Record<TaskStatus, number>;
  byPriority: Record<TaskPriority, number>;
}

/** Criterios aceitos pela API em ?sort=; vazio mantem a ordem padrao (mais recentes). */
export type TaskSort = '' | 'priority,desc' | 'createdAt,desc' | 'deadline,asc';

export interface TaskQuery {
  search: string;
  status: TaskStatus | null;
  priority: TaskPriority | null;
  assigneeId: number | null;
  from: string | null;
  to: string | null;
  sort: TaskSort;
}

export const EMPTY_TASK_QUERY: TaskQuery = {
  search: '',
  status: null,
  priority: null,
  assigneeId: null,
  from: null,
  to: null,
  sort: '',
};

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
