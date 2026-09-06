import { Component, computed, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';

import { ProjectReport as Report, TaskPriority, TaskStatus } from '../../core/models';

const STATUS_LABELS: Record<TaskStatus, string> = {
  TODO: 'A fazer',
  IN_PROGRESS: 'Em andamento',
  DONE: 'Concluido',
};

const PRIORITY_ORDER: TaskPriority[] = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];

/** Resumo do projeto vindo de GET /projects/{id}/report (agregado no banco, nao no cliente). */
@Component({
  selector: 'app-project-report',
  imports: [MatCardModule],
  templateUrl: './project-report.html',
  styleUrl: './project-report.scss',
})
export class ProjectReport {
  readonly report = input.required<Report>();

  protected readonly statusCounters = computed(() =>
    (Object.keys(STATUS_LABELS) as TaskStatus[]).map((status) => ({
      key: status,
      label: STATUS_LABELS[status],
      total: this.report().byStatus[status] ?? 0,
    })),
  );

  protected readonly priorityCounters = computed(() =>
    PRIORITY_ORDER.map((priority) => ({
      key: priority,
      label: priority,
      total: this.report().byPriority[priority] ?? 0,
    })),
  );
}
