import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';

import { describeError } from '../../core/http-error';
import { Member, ProjectRole } from '../../core/models';
import { ProjectService } from '../../core/project-service';

export interface MemberDialogData {
  projectId: number;
  members: Member[];
  ownerId: number;
}

/**
 * Gestao de membros do projeto. O dialog fala direto com a API e devolve `true` no fechamento
 * quando algo mudou, para o board recarregar o projeto.
 */
@Component({
  selector: 'app-member-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatListModule,
    MatSelectModule,
  ],
  templateUrl: './member-dialog.html',
  styleUrl: './member-dialog.scss',
})
export class MemberDialog {
  private readonly formBuilder = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<MemberDialog, boolean>);
  private readonly projectService = inject(ProjectService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly data = inject<MemberDialogData>(MAT_DIALOG_DATA);
  protected readonly roles: ProjectRole[] = ['MEMBER', 'ADMIN'];
  protected readonly members = signal<Member[]>(this.data.members);

  private changed = false;

  protected readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    role: ['MEMBER' as ProjectRole, Validators.required],
  });

  protected isOwner(member: Member): boolean {
    return member.userId === this.data.ownerId;
  }

  protected add(): void {
    if (this.form.invalid) {
      return;
    }
    const { email, role } = this.form.getRawValue();
    this.projectService.addMember(this.data.projectId, email, role).subscribe({
      next: (member) => {
        this.members.update((current) => [...current, member]);
        this.form.reset({ email: '', role: 'MEMBER' });
        this.changed = true;
      },
      error: (error) => this.notifyError(error, 'Nao foi possivel adicionar o membro.'),
    });
  }

  protected remove(member: Member): void {
    this.projectService.removeMember(this.data.projectId, member.userId).subscribe({
      next: () => {
        this.members.update((current) => current.filter((item) => item.userId !== member.userId));
        this.changed = true;
      },
      error: (error) => this.notifyError(error, 'Nao foi possivel remover o membro.'),
    });
  }

  protected close(): void {
    this.dialogRef.close(this.changed);
  }

  private notifyError(error: unknown, fallback: string): void {
    this.snackBar.open(describeError(error, fallback), 'Fechar', { duration: 5000 });
  }
}
