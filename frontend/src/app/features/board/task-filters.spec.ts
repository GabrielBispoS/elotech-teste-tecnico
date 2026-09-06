import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { TaskQuery } from '../../core/models';
import { TaskFilters } from './task-filters';

describe('TaskFilters', () => {
  let fixture: ComponentFixture<TaskFilters>;
  let emitted: TaskQuery[];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TaskFilters, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(TaskFilters);
    emitted = [];
    fixture.componentInstance.queryChange.subscribe((query) => emitted.push(query));
    fixture.detectChanges();
  });

  function form() {
    return fixture.componentInstance['form'];
  }

  it('emite a consulta com os filtros preenchidos apos o debounce', fakeAsync(() => {
    form().patchValue({ status: 'IN_PROGRESS', priority: 'HIGH', assigneeId: 7 });
    tick(350);

    expect(emitted.at(-1)).toEqual(
      jasmine.objectContaining({ status: 'IN_PROGRESS', priority: 'HIGH', assigneeId: 7 }),
    );
  }));

  it('nao emite antes do debounce', fakeAsync(() => {
    form().patchValue({ search: 'relatorio' });
    tick(100);

    expect(emitted).toEqual([]);
    tick(250);
    expect(emitted.at(-1)?.search).toBe('relatorio');
  }));

  it('limpa os filtros e emite a consulta vazia', fakeAsync(() => {
    form().patchValue({ status: 'DONE', sort: 'deadline,asc' });
    tick(350);

    fixture.componentInstance['clear']();
    tick(350);

    expect(emitted.at(-1)).toEqual(
      jasmine.objectContaining({ status: null, sort: '', search: '' }),
    );
  }));
});
