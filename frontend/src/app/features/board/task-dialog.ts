import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { Member, Task, TaskPayload, TaskPriority } from '../../core/models';

export interface TaskDialogData {
  task: Task | null;
  members: Member[];
}

export type TaskDialogResult = { action: 'save'; payload: TaskPayload } | { action: 'delete' };

@Component({
  selector: 'app-task-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './task-dialog.html',
})
export class TaskDialog {
  private readonly formBuilder = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<TaskDialog, TaskDialogResult>);

  protected readonly data = inject<TaskDialogData>(MAT_DIALOG_DATA);
  protected readonly priorities: TaskPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
  protected readonly isEditing = this.data.task !== null;

  protected readonly form = this.formBuilder.nonNullable.group({
    title: [this.data.task?.title ?? '', [Validators.required, Validators.maxLength(255)]],
    description: [this.data.task?.description ?? '', Validators.maxLength(4000)],
    priority: [this.data.task?.priority ?? ('MEDIUM' as TaskPriority), Validators.required],
    deadline: [this.data.task?.deadline ?? ''],
    assigneeId: [this.data.task?.assignee?.id ?? (null as number | null)],
  });

  protected submit(): void {
    if (this.form.invalid) {
      return;
    }
    const value = this.form.getRawValue();
    this.dialogRef.close({
      action: 'save',
      payload: {
        title: value.title,
        description: value.description || null,
        priority: value.priority,
        deadline: value.deadline || null,
        assigneeId: value.assigneeId,
      },
    });
  }

  protected remove(): void {
    this.dialogRef.close({ action: 'delete' });
  }
}
