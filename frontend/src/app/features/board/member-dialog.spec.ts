import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { Member } from '../../core/models';
import { ProjectService } from '../../core/project-service';
import { MemberDialog } from './member-dialog';

describe('MemberDialog', () => {
  let fixture: ComponentFixture<MemberDialog>;
  let projectService: jasmine.SpyObj<ProjectService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<MemberDialog, boolean>>;

  const owner: Member = { id: 1, userId: 1, name: 'Ana', email: 'ana@elotech.com', role: 'ADMIN' };
  const member: Member = { id: 2, userId: 2, name: 'Bruno', email: 'bruno@elotech.com', role: 'MEMBER' };

  beforeEach(async () => {
    projectService = jasmine.createSpyObj<ProjectService>('ProjectService', ['removeMember', 'addMember']);
    dialogRef = jasmine.createSpyObj<MatDialogRef<MemberDialog, boolean>>('MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      imports: [MemberDialog, NoopAnimationsModule],
      providers: [
        { provide: ProjectService, useValue: projectService },
        { provide: MatDialogRef, useValue: dialogRef },
        {
          provide: MAT_DIALOG_DATA,
          useValue: { projectId: 7, members: [owner, member], ownerId: owner.userId },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MemberDialog);
    fixture.detectChanges();
  });

  it('nao permite remover o dono do projeto', () => {
    const buttons: HTMLButtonElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('button[aria-label="Remover membro"]'),
    );

    expect(buttons[0].disabled).toBeTrue();
    expect(buttons[1].disabled).toBeFalse();
  });

  it('remove o membro e sinaliza a mudanca ao fechar', () => {
    projectService.removeMember.and.returnValue(of(void 0));

    fixture.componentInstance['remove'](member);
    fixture.componentInstance['close']();

    expect(projectService.removeMember).toHaveBeenCalledWith(7, member.userId);
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });
});
