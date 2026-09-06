import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';

import { ProjectService } from '../../core/project-service';
import { describeError } from '../../core/http-error';
import { Project } from '../../core/models';
import { ConfirmDialog } from '../../shared/confirm-dialog/confirm-dialog';
import { Toolbar } from '../../shared/toolbar/toolbar';
import { ProjectDialog } from './project-dialog';

@Component({
  selector: 'app-projects',
  imports: [
    RouterLink,
    Toolbar,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatDialogModule,
    MatIconModule,
    MatProgressBarModule,
  ],
  templateUrl: './projects.html',
  styleUrl: './projects.scss',
})
export class Projects implements OnInit {
  private readonly projectService = inject(ProjectService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly projects = signal<Project[]>([]);
  protected readonly loading = signal(false);

  ngOnInit(): void {
    this.load();
  }

  protected openProjectDialog(project: Project | null): void {
    this.dialog
      .open(ProjectDialog, { width: '420px', data: { project } })
      .afterClosed()
      .subscribe((result?: { name: string; description: string | null }) => {
        if (!result) {
          return;
        }
        const request = project
          ? this.projectService.update(project.id, result.name, result.description)
          : this.projectService.create(result.name, result.description);

        request.subscribe({
          next: (saved) =>
            this.projects.update((current) =>
              project ? current.map((item) => (item.id === saved.id ? saved : item)) : [...current, saved],
            ),
          error: (error) =>
            this.snackBar.open(describeError(error, 'Nao foi possivel salvar o projeto.'), 'Fechar', {
              duration: 5000,
            }),
        });
      });
  }

  protected confirmDelete(project: Project): void {
    this.dialog
      .open(ConfirmDialog, {
        width: '420px',
        data: {
          title: 'Excluir projeto',
          message: `O projeto "${project.name}" e todas as suas tarefas serao excluidos.`,
        },
      })
      .afterClosed()
      .subscribe((confirmed?: boolean) => {
        if (!confirmed) {
          return;
        }
        this.projectService.remove(project.id).subscribe({
          next: () => this.projects.update((current) => current.filter((item) => item.id !== project.id)),
          error: (error) =>
            this.snackBar.open(describeError(error, 'Nao foi possivel excluir o projeto.'), 'Fechar', {
              duration: 5000,
            }),
        });
      });
  }

  private load(): void {
    this.loading.set(true);
    this.projectService.list().subscribe({
      next: (projects) => {
        this.projects.set(projects);
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Nao foi possivel carregar os projetos.', 'Fechar', { duration: 4000 });
        this.loading.set(false);
      },
    });
  }
}
