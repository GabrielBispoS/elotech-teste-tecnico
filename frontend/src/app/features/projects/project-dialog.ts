import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { Project } from '../../core/models';

export interface ProjectDialogData {
  project: Project | null;
}

@Component({
  selector: 'app-project-dialog',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  templateUrl: './project-dialog.html',
})
export class ProjectDialog {
  private readonly formBuilder = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<ProjectDialog>);
  private readonly data = inject<ProjectDialogData>(MAT_DIALOG_DATA, { optional: true });

  protected readonly isEditing = this.data?.project != null;

  protected readonly form = this.formBuilder.nonNullable.group({
    name: [this.data?.project?.name ?? '', [Validators.required, Validators.maxLength(255)]],
    description: [this.data?.project?.description ?? '', Validators.maxLength(2000)],
  });

  protected submit(): void {
    if (this.form.invalid) {
      return;
    }
    const { name, description } = this.form.getRawValue();
    this.dialogRef.close({ name, description: description || null });
  }
}
