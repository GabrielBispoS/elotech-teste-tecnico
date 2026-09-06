import { Page, expect, test } from '@playwright/test';

const ADMIN = { email: 'ana@elotech.com', password: 'password123' };
const MEMBER_EMAIL = 'bruno@elotech.com';

/**
 * Fluxo critico ponta a ponta: login, projeto, membro, tarefa com prazo, drag and drop entre
 * colunas e o resumo do projeto refletindo a mudanca.
 */
test('do login ao board: cria projeto, tarefa e move o card entre colunas', async ({ page }) => {
  const projectName = `E2E ${Date.now()}`;

  await login(page, ADMIN.email, ADMIN.password);
  await createProject(page, projectName);
  await openBoard(page, projectName);
  await addMember(page, MEMBER_EMAIL);
  await createTask(page, 'Tarefa do fluxo E2E');

  await expect(column(page, 'A fazer').getByText('Tarefa do fluxo E2E')).toBeVisible();
  await expect(statusCounter(page, 'A fazer')).toHaveText('1');

  await dragCard(page, 'Tarefa do fluxo E2E', 'Em andamento');

  await expect(column(page, 'Em andamento').getByText('Tarefa do fluxo E2E')).toBeVisible();
  await expect(statusCounter(page, 'Em andamento')).toHaveText('1');

  await deleteProject(page, projectName);
});

test('busca textual filtra os cards do board', async ({ page }) => {
  const projectName = `E2E busca ${Date.now()}`;

  await login(page, ADMIN.email, ADMIN.password);
  await createProject(page, projectName);
  await openBoard(page, projectName);
  await createTask(page, 'Ajustar relatorio mensal');
  await createTask(page, 'Corrigir login');

  await page.getByLabel('Buscar').fill('relatorio');

  await expect(page.locator('.task-card', { hasText: 'Ajustar relatorio mensal' })).toBeVisible();
  await expect(page.locator('.task-card', { hasText: 'Corrigir login' })).toBeHidden();

  await deleteProject(page, projectName);
});

async function login(page: Page, email: string, password: string): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('E-mail').fill(email);
  await page.getByLabel('Senha').fill(password);
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page.getByRole('heading', { name: 'Meus projetos' })).toBeVisible();
}

async function createProject(page: Page, name: string): Promise<void> {
  await page.getByRole('button', { name: 'Novo projeto' }).click();
  const dialog = page.getByRole('dialog');
  await dialog.getByLabel('Nome').fill(name);
  await dialog.getByRole('button', { name: 'Criar' }).click();
  await expect(page.getByText(name)).toBeVisible();
}

async function openBoard(page: Page, projectName: string): Promise<void> {
  await page
    .locator('mat-card', { hasText: projectName })
    .getByRole('link', { name: 'Abrir board' })
    .click();
  await expect(page.getByRole('button', { name: 'Nova tarefa' })).toBeVisible();
}

async function addMember(page: Page, email: string): Promise<void> {
  await page.getByRole('button', { name: 'Membros' }).click();
  const dialog = page.getByRole('dialog');
  await dialog.getByLabel('E-mail').fill(email);
  await dialog.getByRole('button', { name: 'Adicionar' }).click();
  await expect(dialog.getByText(email)).toBeVisible();
  await dialog.getByRole('button', { name: 'Fechar' }).click();
}

async function createTask(page: Page, title: string): Promise<void> {
  await page.getByRole('button', { name: 'Nova tarefa' }).click();
  const dialog = page.getByRole('dialog');
  await dialog.getByLabel('Titulo').fill(title);
  await dialog.getByLabel('Prazo', { exact: true }).fill(inDays(30));
  await dialog.getByRole('button', { name: 'Salvar' }).click();
  await expect(page.locator('.task-card', { hasText: title })).toBeVisible();
}

async function deleteProject(page: Page, projectName: string): Promise<void> {
  await page.getByRole('link', { name: 'Projetos' }).click();
  await page.locator('mat-card', { hasText: projectName }).getByRole('button', { name: 'Excluir' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Excluir' }).click();
  await expect(page.locator('mat-card', { hasText: projectName })).toBeHidden();
}

function column(page: Page, label: string) {
  return page.locator('.board-column', { hasText: label });
}

function statusCounter(page: Page, label: string) {
  return column(page, label).locator('.board-column-count');
}

/** O CDK escuta eventos de mouse; um drag em passos e o que dispara o drop de verdade. */
async function dragCard(page: Page, title: string, targetColumn: string): Promise<void> {
  const card = page.locator('.task-card', { hasText: title });
  const target = column(page, targetColumn).locator('.board-column-list');

  const from = (await card.boundingBox())!;
  const to = (await target.boundingBox())!;

  await page.mouse.move(from.x + from.width / 2, from.y + from.height / 2);
  await page.mouse.down();
  await page.mouse.move(to.x + to.width / 2, to.y + 60, { steps: 20 });
  await page.mouse.move(to.x + to.width / 2, to.y + 80, { steps: 10 });
  await page.mouse.up();
}

function inDays(days: number): string {
  const date = new Date();
  date.setDate(date.getDate() + days);
  return date.toISOString().slice(0, 10);
}
