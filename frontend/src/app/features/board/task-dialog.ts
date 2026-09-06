import { DatePipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, ValidatorFn, Validators } from '@angular/forms';
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

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;
const MAX_DEADLINE_YEARS = 1;

/** Converte para o formato do input date usando o fuso local (toISOString usaria UTC). */
function toIsoDate(date: Date): string {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return local.toISOString().slice(0, 10);
}

function addYears(date: Date, years: number): Date {
  const result = new Date(date);
  result.setFullYear(result.getFullYear() + years);
  return result;
}

/** O input date aceita ano com mais de 4 digitos; aqui a data fora da janela e recusada. */
function deadlineWindow(min: string, max: string): ValidatorFn {
  return (control) => {
    const value = control.value as string;
    if (!value) {
      return null;
    }
    if (!ISO_DATE.test(value)) {
      return { deadlineFormat: true };
    }
    if (value < min) {
      return { deadlineTooEarly: true };
    }
    if (value > max) {
      return { deadlineTooLate: true };
    }
    return null;
  };
}

@Component({
  selector: 'app-task-dialog',
  imports: [
    DatePipe,
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

  protected readonly minDeadline = toIsoDate(new Date());
  protected readonly maxDeadline = toIsoDate(addYears(new Date(), MAX_DEADLINE_YEARS));

  protected readonly form = this.formBuilder.nonNullable.group({
    title: [this.data.task?.title ?? '', [Validators.required, Validators.maxLength(255)]],
    description: [this.data.task?.description ?? '', Validators.maxLength(4000)],
    priority: [this.data.task?.priority ?? ('MEDIUM' as TaskPriority), Validators.required],
    deadline: [
      this.data.task?.deadline ?? '',
      [Validators.required, deadlineWindow(this.minDeadline, this.maxDeadline)],
    ],
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
        deadline: value.deadline,
        assigneeId: value.assigneeId,
      },
    });
  }

  protected remove(): void {
    this.dialogRef.close({ action: 'delete' });
  }
}
