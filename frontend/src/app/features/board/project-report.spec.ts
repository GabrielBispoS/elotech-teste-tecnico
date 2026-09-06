import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectReport as Report } from '../../core/models';
import { ProjectReport } from './project-report';

describe('ProjectReport', () => {
  let fixture: ComponentFixture<ProjectReport>;

  const report: Report = {
    byStatus: { TODO: 12, IN_PROGRESS: 3, DONE: 45 },
    byPriority: { LOW: 10, MEDIUM: 20, HIGH: 25, CRITICAL: 5 },
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ProjectReport] }).compileComponents();

    fixture = TestBed.createComponent(ProjectReport);
    fixture.componentRef.setInput('report', report);
    fixture.detectChanges();
  });

  function totals(): string[] {
    return Array.from(fixture.nativeElement.querySelectorAll('.report-total')).map((element) =>
      (element as HTMLElement).textContent!.trim(),
    );
  }

  it('mostra os contadores por status na ordem do fluxo', () => {
    expect(totals().slice(0, 3)).toEqual(['12', '3', '45']);
  });

  it('mostra os contadores por prioridade da maior para a menor', () => {
    expect(totals().slice(3)).toEqual(['5', '25', '20', '10']);
  });
});
