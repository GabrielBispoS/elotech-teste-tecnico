import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth-service';
import { describeError } from '../../core/http-error';
import { ProjectDetail, Task, TaskPayload, TaskStatus } from '../../core/models';
import { ProjectService } from '../../core/project-service';
import { TaskService } from '../../core/task-service';
import { ConfirmDialog } from '../../shared/confirm-dialog/confirm-dialog';
import { Toolbar } from '../../shared/toolbar/toolbar';
import { MemberDialog } from './member-dialog';
import { TaskDialog, TaskDialogResult } from './task-dialog';

const COLUMNS: { status: TaskStatus; label: string }[] = [
  { status: 'TODO', label: 'A fazer' },
  { status: 'IN_PROGRESS', label: 'Em andamento' },
  { status: 'DONE', label: 'Concluido' },
];

@Component({
  selector: 'app-board',
  imports: [
    RouterLink,
    DragDropModule,
    Toolbar,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatDialogModule,
    MatIconModule,
    MatProgressBarModule,
  ],
  templateUrl: './board.html',
  styleUrl: './board.scss',
})
export class Board implements OnInit {
  private readonly projectService = inject(ProjectService);
  private readonly taskService = inject(TaskService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly auth = inject(AuthService);

  readonly projectId = input.required({ transform: Number });

  protected readonly columns = COLUMNS;
  protected readonly project = signal<ProjectDetail | null>(null);
  protected readonly tasks = signal<Task[]>([]);
  protected readonly loading = signal(false);

  protected readonly isAdmin = computed(() => this.project()?.myRole === 'ADMIN');

  ngOnInit(): void {
    this.loadProject();
    this.loadTasks();
  }

  protected tasksOf(status: TaskStatus): Task[] {
    return this.tasks().filter((task) => task.status === status);
  }

  protected drop(event: CdkDragDrop<TaskStatus>, target: TaskStatus): void {
    const task = event.item.data as Task;
    if (task.status === target) {
      return;
    }

    const previousStatus = task.status;
    this.applyStatus(task.id, target);

    this.taskService.changeStatus(task.id, target).subscribe({
      next: (updated) => this.replaceTask(updated),
      error: (error) => {
        // rollback: o backend recusou a transicao (regra de estado, WIP limit ou trava de CRITICAL)
        this.applyStatus(task.id, previousStatus);
        this.snackBar.open(describeError(error, 'Nao foi possivel mover a tarefa.'), 'Fechar', {
          duration: 5000,
        });
      },
    });
  }

  protected openTaskDialog(task: Task | null): void {
    this.dialog
      .open(TaskDialog, { width: '480px', data: { task, members: this.project()?.members ?? [] } })
      .afterClosed()
      .subscribe((result?: TaskDialogResult) => {
        if (!result) {
          return;
        }
        if (result.action === 'delete' && task) {
          this.deleteTask(task);
          return;
        }
        if (result.action === 'save') {
          this.saveTask(task, result.payload);
        }
      });
  }

  protected openMemberDialog(): void {
    const current = this.project();
    if (!current) {
      return;
    }
    this.dialog
      .open(MemberDialog, {
        width: '520px',
        data: { projectId: current.id, members: current.members, ownerId: current.ownerId },
      })
      .afterClosed()
      .subscribe((changed?: boolean) => {
        if (changed) {
          this.loadProject();
        }
      });
  }

  private saveTask(task: Task | null, payload: TaskPayload): void {
    const request = task
      ? this.taskService.update(this.projectId(), task.id, payload)
      : this.taskService.create(this.projectId(), payload);

    request.subscribe({
      next: (saved) => {
        if (task) {
          this.replaceTask(saved);
        } else {
          this.tasks.update((current) => [saved, ...current]);
        }
        this.notifyIfAssignedToMe(saved, task?.assignee?.id ?? null);
      },
      error: (error) =>
        this.snackBar.open(describeError(error, 'Nao foi possivel salvar a tarefa.'), 'Fechar', {
          duration: 5000,
        }),
    });
  }

  private deleteTask(task: Task): void {
    this.dialog
      .open(ConfirmDialog, {
        width: '420px',
        data: { title: 'Excluir tarefa', message: `A tarefa "${task.title}" sera excluida.` },
      })
      .afterClosed()
      .subscribe((confirmed?: boolean) => {
        if (!confirmed) {
          return;
        }
        this.taskService.remove(this.projectId(), task.id).subscribe({
          next: () => this.tasks.update((current) => current.filter((item) => item.id !== task.id)),
          error: (error) =>
            this.snackBar.open(describeError(error, 'Nao foi possivel excluir a tarefa.'), 'Fechar', {
              duration: 5000,
            }),
        });
      });
  }

  /** Avisa o usuario logado quando a tarefa passa a ser dele. */
  private notifyIfAssignedToMe(task: Task, previousAssigneeId: number | null): void {
    const myId = this.auth.currentUser()?.id;
    if (task.assignee?.id === myId && previousAssigneeId !== myId) {
      this.snackBar.open(`"${task.title}" foi atribuida a voce.`, 'Fechar', { duration: 5000 });
    }
  }

  protected replaceTask(updated: Task): void {
    this.tasks.update((current) => current.map((task) => (task.id === updated.id ? updated : task)));
  }

  private applyStatus(taskId: number, status: TaskStatus): void {
    this.tasks.update((current) =>
      current.map((task) => (task.id === taskId ? { ...task, status } : task)),
    );
  }

  private loadProject(): void {
    this.projectService.findById(this.projectId()).subscribe({
      next: (project) => this.project.set(project),
      error: (error) =>
        this.snackBar.open(describeError(error, 'Nao foi possivel carregar o projeto.'), 'Fechar', {
          duration: 5000,
        }),
    });
  }

  private loadTasks(): void {
    this.loading.set(true);
    this.taskService.listByProject(this.projectId()).subscribe({
      next: (tasks) => {
        this.tasks.set(tasks);
        this.loading.set(false);
      },
      error: (error) => {
        this.snackBar.open(describeError(error, 'Nao foi possivel carregar as tarefas.'), 'Fechar', {
          duration: 5000,
        });
        this.loading.set(false);
      },
    });
  }
}
