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
import { Project } from '../../core/models';
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

  protected openCreateDialog(): void {
    this.dialog
      .open(ProjectDialog, { width: '420px' })
      .afterClosed()
      .subscribe((result?: { name: string; description: string | null }) => {
        if (!result) {
          return;
        }
        this.projectService.create(result.name, result.description).subscribe({
          next: (project) => this.projects.update((current) => [...current, project]),
          error: () => this.snackBar.open('Nao foi possivel criar o projeto.', 'Fechar', { duration: 4000 }),
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
