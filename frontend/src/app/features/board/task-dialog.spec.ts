import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { TaskDialog, TaskDialogResult } from './task-dialog';

describe('TaskDialog', () => {
  let fixture: ComponentFixture<TaskDialog>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<TaskDialog, TaskDialogResult>>;

  beforeEach(async () => {
    dialogRef = jasmine.createSpyObj<MatDialogRef<TaskDialog, TaskDialogResult>>('MatDialogRef', [
      'close',
    ]);

    await TestBed.configureTestingModule({
      imports: [TaskDialog, NoopAnimationsModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { projectId: 1, task: null, members: [] } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TaskDialog);
    fixture.detectChanges();
  });

  function deadlineControl() {
    return fixture.componentInstance['form'].controls.deadline;
  }

  function isoDate(offsetDays: number): string {
    const date = new Date();
    date.setDate(date.getDate() + offsetDays);
    return date.toISOString().slice(0, 10);
  }

  it('exige o prazo', () => {
    deadlineControl().setValue('');

    expect(deadlineControl().hasError('required')).toBeTrue();
    expect(fixture.componentInstance['form'].invalid).toBeTrue();
  });

  it('recusa prazo anterior a hoje', () => {
    deadlineControl().setValue(isoDate(-1));

    expect(deadlineControl().hasError('deadlineTooEarly')).toBeTrue();
  });

  it('recusa prazo alem de um ano', () => {
    deadlineControl().setValue(isoDate(366));

    expect(deadlineControl().hasError('deadlineTooLate')).toBeTrue();
  });

  it('recusa ano com mais de quatro digitos', () => {
    deadlineControl().setValue('123456-01-01');

    expect(deadlineControl().hasError('deadlineFormat')).toBeTrue();
  });

  it('fecha com o payload quando o prazo esta dentro da janela', () => {
    const form = fixture.componentInstance['form'];
    form.patchValue({ title: 'Nova tarefa', priority: 'HIGH', deadline: isoDate(10) });

    fixture.componentInstance['submit']();

    expect(dialogRef.close).toHaveBeenCalledWith({
      action: 'save',
      payload: jasmine.objectContaining({ title: 'Nova tarefa', deadline: isoDate(10) }),
    });
  });
});
