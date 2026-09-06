import { HttpErrorResponse } from '@angular/common/http';

/** O backend responde erros em ProblemDetail (RFC 7807); o campo detail e a mensagem para o usuario. */
export function describeError(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse && typeof error.error?.detail === 'string') {
    return error.error.detail;
  }
  return fallback;
}
