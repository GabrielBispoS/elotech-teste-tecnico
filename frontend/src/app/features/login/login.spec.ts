import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../core/auth-service';
import { Login } from './login';

describe('Login', () => {
  let fixture: ComponentFixture<Login>;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['login']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Login);
    fixture.detectChanges();
  });

  function fillForm(email: string, password: string): void {
    setInput('input[type="email"]', email);
    setInput('input[type="password"]', password);
  }

  function setInput(selector: string, value: string): void {
    const input: HTMLInputElement = fixture.nativeElement.querySelector(selector);
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  function submit(): void {
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
  }

  it('nao chama o servico quando o formulario esta invalido', () => {
    submit();

    expect(authService.login).not.toHaveBeenCalled();
  });

  it('navega para os projetos apos autenticar com sucesso', () => {
    authService.login.and.returnValue(
      of({ token: 'jwt', expiresInSeconds: 3600, userId: 1, name: 'Ana', email: 'ana@elotech.com' }),
    );
    fillForm('ana@elotech.com', 'password123');

    submit();

    expect(authService.login).toHaveBeenCalledWith('ana@elotech.com', 'password123');
    expect(router.navigate).toHaveBeenCalledWith(['/projects']);
  });

  it('exibe mensagem generica quando as credenciais sao recusadas', () => {
    authService.login.and.returnValue(throwError(() => new Error('401')));
    fillForm('ana@elotech.com', 'senha-errada');

    submit();
    fixture.detectChanges();

    expect(router.navigate).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('.login-error').textContent).toContain('invalidos');
  });
});
