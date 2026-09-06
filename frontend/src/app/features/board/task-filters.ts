import { Component, output } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { inject, input } from '@angular/core';
import { EMPTY_TASK_QUERY, Member, TaskPriority, TaskQuery, TaskSort, TaskStatus } from '../../core/models';

const TYPING_DEBOUNCE_MS = 350;

@Component({
  selector: 'app-task-filters',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './task-filters.html',
  styleUrl: './task-filters.scss',
})
export class TaskFilters {
  private readonly formBuilder = inject(FormBuilder);

  readonly members = input<Member[]>([]);
  readonly queryChange = output<TaskQuery>();

  protected readonly statuses: { value: TaskStatus; label: string }[] = [
    { value: 'TODO', label: 'A fazer' },
    { value: 'IN_PROGRESS', label: 'Em andamento' },
    { value: 'DONE', label: 'Concluido' },
  ];
  protected readonly priorities: TaskPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
  protected readonly sorts: { value: TaskSort; label: string }[] = [
    { value: '', label: 'Mais recentes' },
    { value: 'priority,desc', label: 'Prioridade' },
    { value: 'createdAt,desc', label: 'Data de criacao' },
    { value: 'deadline,asc', label: 'Prazo' },
  ];

  protected readonly form = this.formBuilder.nonNullable.group({
    search: [EMPTY_TASK_QUERY.search],
    status: [EMPTY_TASK_QUERY.status],
    priority: [EMPTY_TASK_QUERY.priority],
    assigneeId: [EMPTY_TASK_QUERY.assigneeId],
    from: [EMPTY_TASK_QUERY.from],
    to: [EMPTY_TASK_QUERY.to],
    sort: [EMPTY_TASK_QUERY.sort],
  });

  constructor() {
    this.form.valueChanges
      .pipe(
        debounceTime(TYPING_DEBOUNCE_MS),
        distinctUntilChanged((previous, current) => JSON.stringify(previous) === JSON.stringify(current)),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.queryChange.emit(this.form.getRawValue()));
  }

  /** A API de busca textual nao combina com os demais filtros; a UI avisa quando ha termo. */
  protected get searching(): boolean {
    return this.form.controls.search.value.trim().length > 0;
  }

  protected clear(): void {
    this.form.reset(EMPTY_TASK_QUERY);
  }
}
