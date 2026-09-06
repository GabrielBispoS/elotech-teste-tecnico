# Task Manager — Desafio Técnico Elotech

Sistema simplificado de gerenciamento de tarefas para equipes: projetos, membros com papel por projeto,
tarefas com máquina de estados, WIP limit e relatório agregado.

Monorepo com `backend/` (Spring Boot 3) e `frontend/` (Angular).

---

## Como rodar

### Tudo com Docker (caminho mais curto)

Pré-requisito: Docker. Não precisa de Java nem Node instalados.

```bash
docker compose up -d --build
```

Sobe PostgreSQL, API e frontend. As migrations do Flyway rodam na subida da API.

| Serviço | URL |
|---|---|
| Frontend | http://localhost:4200 |
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |

O container do frontend é um nginx que serve o build de produção e encaminha `/api` para a API,
o mesmo contrato do `proxy.conf.json` usado no `ng serve` — sem URL de backend no código e sem CORS.

```bash
docker compose logs -f backend   # acompanhar a subida
docker compose down              # parar (use -v para apagar o banco)
```

### Backend em desenvolvimento

Pré-requisitos: Java 21 e Docker.

```bash
# 1. sobe apenas o PostgreSQL
docker compose up -d postgres

# 2. sobe a API em http://localhost:8080
cd backend
./mvnw spring-boot:run
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- Testes: `./mvnw test` (o teste de integração é pulado automaticamente se não houver Docker)

**Usuários de demonstração** (criados pela migration `V2`, senha `password123`):

| E-mail | Nome |
|---|---|
| `ana@elotech.com` | Ana Souza |
| `bruno@elotech.com` | Bruno Lima |
| `carla@elotech.com` | Carla Dias |

Não há endpoint de cadastro: o escopo do desafio lista apenas `POST /api/auth/login`, então os usuários
vêm de uma migration de seed.

**Configuração por variável de ambiente** (todas têm default de desenvolvimento):
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`.
Em produção, `JWT_SECRET` deve ser sobrescrito.

### Frontend em desenvolvimento

Pré-requisito: Node 20+ (desenvolvido com Node 24). O backend precisa estar no ar.

```bash
cd frontend
npm install
npm start   # http://localhost:4200
```

O `ng serve` usa `proxy.conf.json` para encaminhar `/api` para `http://localhost:8080`, então não há
URL de backend hard-coded no código nem CORS em desenvolvimento.

- Testes de componente: `npm test -- --watch=false --browsers=ChromeHeadless`
- Testes E2E (Playwright, exigem a aplicação no ar): `npx playwright install chromium` uma vez e então
  `npm run e2e` (aponte para outra URL com `E2E_BASE_URL=http://localhost:4200`)
- Build de produção: `npm run build`

**Telas**

| Rota | Tela |
|---|---|
| `/login` | autenticação; guarda `authGuard` protege as demais |
| `/projects` | projetos em que o usuário é membro, com criação, edição e exclusão de projeto |
| `/projects/:projectId` | board kanban com drag and drop, resumo do projeto, filtros, busca textual, criação/edição de tarefa com histórico de alterações e gestão de membros |

---

## Stack e versões

**Backend**
- Java 21, Spring Boot 3.3.13, Maven (via wrapper `./mvnw`)
- Spring Web, Spring Security, Spring Data JPA, Bean Validation
- JWT com jjwt 0.12.7
- PostgreSQL 16 + Flyway
- Spring Cache (`ConcurrentMapCacheManager`) no relatório
- springdoc-openapi 2.6.0 (Swagger UI)
- Lombok (apenas nas entidades)
- JUnit 5 + Mockito + AssertJ; Testcontainers PostgreSQL

**Frontend**
- Angular 20.3 (standalone components, signals para estado, rotas com lazy loading)
- Angular Material / CDK 20.2 (drag and drop no board)
- HTTP interceptor funcional para o JWT
- Karma + Jasmine (componentes) e Playwright (E2E)

---

## Arquitetura

Camadas pragmáticas (`controller → service → repository`), **organizadas por feature**:

```
com.elotech.taskmanager
├── auth      login, JWT, filtro, SecurityConfig
├── user      entidade e repository
├── project   controller, service, repository, ProjectAccessService, domain, dto
├── task      controller, service, repository, specifications, audit, relatório + cache, domain, dto
└── common    exceptions + ProblemDetail, paginação, security, validação, cache, OpenAPI
```

Entidades JPA no domínio de cada feature; DTOs são `record` com mapeamento manual via método `from(...)`
estático no próprio record.

No frontend, a mesma ideia: `core/` para o que atravessa a aplicação (serviços HTTP, modelos, guard,
interceptor), `features/` por tela e `shared/` para os componentes reutilizados entre elas.

```
src/app
├── core       auth, interceptor JWT, guard, services de projeto e tarefa, models
├── features
│   ├── login      formulário de autenticação
│   ├── projects   lista de projetos + dialog de criação/edição
│   └── board      board kanban, filtros, resumo, dialogs de tarefa e membros
└── shared     toolbar e dialog de confirmação
```

---

## Decisões e tradeoffs

**Camadas por feature, não arquitetura hexagonal.** O domínio aqui é pequeno e a fonte de dados é uma só.
Portas e adaptadores custariam interfaces e mapeamentos extras sem nenhum ganho de testabilidade real —
os testes de regra de negócio já rodam sem Spring, mockando repositórios. Organizar por feature (e não
por camada técnica) mantém junto o que muda junto.

**Autorização de recurso explícita, não SpEL em `@PreAuthorize`.** Todo service começa chamando
`ProjectAccessService.requireMember(...)` ou `requireAdmin(...)`. É código Java comum: aparece no stack
trace, é testável isoladamente, refatorável pela IDE e legível para quem revisa. Expressões em anotação
resolvem o mesmo problema com uma linguagem paralela, sem tipagem e sem cobertura de teste.

**Papel por projeto, não papel global.** `ProjectMembership` guarda o `ProjectRole`; o mesmo usuário pode
ser ADMIN em um projeto e MEMBER em outro. Por isso o JWT não carrega authorities de negócio — carrega
apenas a identidade, e o papel é resolvido por projeto a cada operação.

**Máquina de estados no domínio.** `Task.changeStatus(novoStatus, papelDoAtor)` usa um `switch` exaustivo
sobre o enum. A regra vive junto do dado que ela protege, e o service não consegue burlá-la. A trava de
CRITICAL entra no mesmo método porque depende só do estado da tarefa e do papel do ator.

**WIP limit contado no banco.** A contagem sai de `count(*)` com índice em `(assignee_id, status)`, nunca
carregando tarefas em memória. Fica no service (e não no domínio) porque depende de uma consulta.
Tradeoff assumido: sob concorrência alta, duas requisições simultâneas podem passar juntas pela contagem.
Resolver isso exigiria lock pessimista ou constraint no banco — desnecessário para o volume deste cenário,
e está listado em "o que eu faria com mais tempo".

**Ordenação por prioridade com coluna derivada.** `priority` é gravada como texto, então `sort=priority`
ordenaria em ordem alfabética (CRITICAL, HIGH, LOW, MEDIUM) — que não é a ordem do negócio. A tabela ganhou
`priority_rank smallint generated always as (...) stored`: o banco deriva o peso do próprio enum, sem
chance de divergir, e o índice `(project_id, priority_rank)` mantém a ordenação barata. O contrato da API
não muda — `TaskSortMapper` traduz `sort=priority` para a coluna derivada. A alternativa (um `CASE` na
query) evitaria a migration, mas não seria indexável.

**JPA Specifications para os filtros.** Os predicados são compostos apenas para os filtros efetivamente
informados (`TaskSpecifications.build`), evitando o clássico `where 1=1` com `if` em SQL string. Ordenação
e paginação usam `Pageable`/`Sort` nativos do Spring Data; a resposta vem em `PageResponse` com `page`,
`size`, `totalElements` e `totalPages`.

**Busca textual com ILIKE — e o seu limite.** `GET /tasks/search?q=` usa uma query nativa com `ILIKE` e o
termo passado como parâmetro (nunca concatenado). O índice `idx_tasks_title_lower` acelera busca por
prefixo, mas um padrão `%termo%` não usa índice B-tree — esse é o tradeoff consciente. Em escala, a
migração natural é `pg_trgm` com índice GIN, ou full-text (`tsvector` + `to_tsquery`) se a busca virar
requisito de produto.

**Cache só no relatório, com invalidação explícita.** O relatório é a leitura mais cara (dois `GROUP BY`)
e a mais repetida — o board recarrega o resumo a cada visita, enquanto as tarefas do projeto mudam pouco.
Ele vive em `ProjectReportCache`, com chave igual ao `projectId`, e **toda escrita de tarefa daquele
projeto** (criar, editar, mudar status, excluir) chama `invalidate(projectId)`: invalidação por evento, não
por TTL, porque o conjunto de operações que sujam o relatório é pequeno e conhecido — TTL entregaria número
errado por um tempo sem economizar nada. A verificação de acesso fica fora do trecho cacheado
(`TaskReportService` chama `requireMember` antes), senão o cache serviria dados a um não-membro.
A listagem de tarefas ficou de fora de propósito: com filtros, ordenação e paginação, a chave viraria a
combinação de todos os parâmetros e a taxa de acerto seria baixa demais para pagar o custo. Tradeoff: o
cache é do processo (`ConcurrentMapCacheManager`); com mais de uma instância, cada uma teria a sua cópia e
a invalidação não cruzaria — aí o passo natural é Redis.

**Relatório agregado no banco.** `GROUP BY` com projeção direta para `record` via construtor JPQL. Nada de
carregar tarefas e contar em memória. Os enums sem ocorrência voltam com zero, para o cliente não precisar
tratar chave ausente.

**Virtual threads (`spring.threads.virtual.enabled=true`).** Uma linha de configuração do Java 21 que troca
o pool de plataforma por virtual threads no Tomcat. Para uma API dominada por espera de I/O (banco), é o
ganho de throughput mais barato que existe: mantém o modelo de programação bloqueante — simples de ler e
depurar — sem o custo de reescrever tudo em WebFlux.

**Erros com `ProblemDetail` (RFC 7807).** Nativo no Boot 3, sem DTO de erro próprio. Um
`@RestControllerAdvice` mapeia as exceptions de domínio para status HTTP, e um `AuthenticationEntryPoint`
próprio garante que o 401 saia no mesmo formato. O handler genérico loga a stack trace e devolve uma
mensagem neutra — nada de detalhe interno vazando para o cliente.

**Testcontainers no teste de integração.** Banco real, migrations reais, dialeto real. H2 daria um teste
mais rápido que valida um banco que não é o de produção — e a busca usa `ILIKE`, que é específico do
PostgreSQL. A classe é anotada com `@Testcontainers(disabledWithoutDocker = true)`: sem Docker na máquina,
o teste é pulado em vez de quebrar o build. Nota para Windows: com Docker Desktop em versões recentes do
Engine, o Testcontainers 1.19 (versão gerenciada pelo Spring Boot 3.3) pode não encontrar o daemon pelo
named pipe e o teste aparece como *skipped*; em Linux/macOS, ou com o daemon exposto em
`tcp://localhost:2375`, ele roda normalmente.

**Signals no frontend, não NgRx.** Estado nativo do Angular, sem actions/reducers/effects para três telas.
O estado do board é um `signal<Task[]>` e as colunas derivam dele — mover um card é uma atualização
imutável desse array. NgRx só se paga quando há muitos consumidores do mesmo estado e necessidade de
time-travel/devtools; aqui seria mais boilerplate do que benefício.

**Filtros e busca no board, resolvidos no servidor.** A barra de filtros (`TaskFilters`) é um componente
próprio que emite a consulta e o board recarrega — nada é filtrado em memória, então o resultado é o mesmo
da API e a paginação continua fazendo sentido. Status, prioridade, responsável, intervalo de prazo e
ordenação (prioridade, criação ou deadline) viram query params de `GET /tasks`; digitar texto passa a
consulta para `GET /tasks/search`, que é um endpoint separado na API — por isso a UI avisa que a busca
textual ignora os demais filtros, em vez de fingir que combina. A digitação tem debounce de 350 ms para
não disparar uma requisição por tecla.

**Resumo e histórico consumidos das telas onde importam.** O relatório (`GET /projects/{id}/report`) fica
no topo do board e é recarregado a cada escrita de tarefa — o mesmo evento que invalida o cache no
servidor, então o número na tela nunca fica atrás do banco. O histórico de auditoria é carregado ao abrir
uma tarefa para edição, e não na listagem: é informação de detalhe, e buscá-la por card multiplicaria
requisições sem ninguém olhar.

**Prazo obrigatório e limitado a um ano, validado nos dois lados.** No formulário, `min`/`max` no input
mais um validador próprio que recusa data passada, data além de um ano e ano fora do padrão de 4 dígitos
(o `input type="date"` aceita anos de até 6 dígitos). No backend, a mesma regra vive em `@DeadlineWindow`,
uma constraint de Bean Validation — a validação do cliente é conveniência de UX, a do servidor é a que
vale, e ambas respondem com a mesma janela. Tarefas antigas sem prazo continuam no banco (a coluna segue
nullable); só não é mais possível criar ou editar uma tarefa sem informá-lo.

**Drag and drop otimista com rollback.** Soltar um card aplica a mudança na UI imediatamente e dispara o
`PATCH /status`. Se o backend recusar (transição inválida, WIP limit ou trava de CRITICAL), o estado
anterior é restaurado e a mensagem do `ProblemDetail` aparece no snackbar — o usuário lê a regra que
barrou a ação, não um erro genérico. As regras vivem só no backend; o frontend não as duplica.

**Componentes standalone e rotas com lazy loading.** Sem `NgModule`; cada rota carrega seu componente sob
demanda. Os parâmetros de rota chegam como `input()` graças a `withComponentInputBinding()`.

**JWT no `localStorage`.** É o caminho pragmático para uma SPA que fala com uma API stateless, ao custo de
ficar exposto a XSS. A alternativa mais segura seria cookie `HttpOnly` + `SameSite`, que exigiria CSRF
token e mudaria o desenho de autenticação do backend — fora do escopo deste desafio.

**Input `type="date"` nativo em vez de `MatDatepicker`.** O backend recebe `LocalDate` no formato ISO, que
é exatamente o que o input nativo produz. Usar o datepicker do Material significaria adicionar um date
adapter e converter `Date` ↔ string nos dois sentidos, sem ganho funcional.

---

## Premissas assumidas

### Matriz de transições de status

O enunciado especifica quatro transições. As demais foram decididas aqui e implementadas de forma
consistente no `switch` de `Task.changeStatus`:

| De \ Para | TODO | IN_PROGRESS | DONE |
|---|---|---|---|
| **TODO** | ❌ | ✅ | ❌ |
| **IN_PROGRESS** | ✅ | ❌ | ✅ * |
| **DONE** | ❌ | ✅ | ❌ |

\* concluir uma tarefa `CRITICAL` exige que o ator seja ADMIN do projeto (senão 403).

Decisões tomadas além do enunciado:

- **TODO → DONE bloqueado.** Concluir sem passar por IN_PROGRESS destruiria a rastreabilidade do fluxo e
  também escaparia do WIP limit. Se a tarefa foi feita, ela passou por "em andamento".
- **IN_PROGRESS → TODO permitido.** É o desfazer de um "iniciar" — devolve a tarefa para a fila e libera
  uma vaga no WIP do responsável.
- **Transição para o mesmo status é rejeitada (409).** `PATCH /status` expressa uma transição, não um
  estado desejado; repetir o estado atual indica erro do cliente, não uma operação idempotente.

### Outras premissas

- **WIP limit é global por responsável**, não por projeto: o limite existe para proteger a pessoa, e ela
  não trabalha em um projeto de cada vez.
- **O WIP limit também vale ao trocar o responsável** de uma tarefa que já está IN_PROGRESS (via `PUT`):
  a tarefa passa a ocupar uma vaga do novo responsável, então o limite dele é verificado. Sem isso o
  limite seria contornável atribuindo tarefas já em andamento.
- **Tarefa sem responsável não sofre WIP limit** — não há de quem contar as tarefas.
- **`from`/`to` filtram o `deadline`** da tarefa (o parâmetro não foi qualificado no enunciado).
- **Projeto inexistente retorna 404; projeto existente sem vínculo retorna 403.** É a resposta mais útil
  para o cliente da API; o custo é revelar a existência de um id, aceitável neste domínio.
- **Quem cria o projeto vira ADMIN dele** e não pode ser removido dos membros — do contrário o projeto
  poderia ficar sem nenhum administrador.
- **Membros (não só ADMINs) criam, editam e excluem tarefas.** ADMIN é exigido para gerir o projeto e seus
  membros, e para concluir tarefas CRITICAL.
- **`assignee` precisa ser membro do projeto** da tarefa (409 caso contrário).
- **Prazo é obrigatório e no máximo um ano à frente** (400 caso contrário). Um prazo aberto ou distante
  demais não ajuda a acompanhar trabalho; a janela de um ano é a premissa assumida aqui.

---

## Testes: o que foi priorizado

A cobertura foi direcionada para o que quebra em produção e é caro de descobrir tarde — as regras de
negócio — e não para percentual de linhas.

**Unitários (sem Spring, com Mockito):**
- `TaskStateMachineTest` — cada transição válida e cada inválida da matriz acima, mais a trava de CRITICAL
  (bloqueia MEMBER, libera ADMIN, não se aplica na reabertura).
- `TaskServiceTest` — WIP limit exatamente no limite e acima dele, WIP na troca de responsável de uma
  tarefa em andamento (e a ausência de revalidação quando o responsável não muda), tarefa sem responsável,
  propagação da falha de autorização, responsável fora do projeto, registro no audit log e invalidação do
  relatório.
- `TaskSortMapperTest` — tradução de `sort=priority` para a coluna de ordem semântica, preservando direção,
  demais critérios e paginação.
- `ProjectAccessServiceTest` — membro, não-membro (403), ADMIN vs MEMBER, projeto inexistente (404).

**Integração (1, com Testcontainers):** `TaskFlowIntegrationTest` percorre o fluxo crítico ponta a ponta
contra um PostgreSQL real — login → cria projeto → adiciona membro → cria tarefa → viola transição inválida
(409) → satura o WIP limit (409) → tenta concluir CRITICAL como MEMBER (403) e conclui como ADMIN (200) →
confere o relatório e a busca textual → verifica a ordenação semântica por prioridade, o WIP limit na troca
de responsável via `PUT` (409), a recusa de prazo ausente ou além de um ano (400) e o relatório atualizado
depois das escritas. Um segundo teste cobre 403 para não-membro e 401 sem token.

Não foram escritos testes de controller isolados (`@WebMvcTest`): o teste de integração já exercita
serialização, validação, filtros de segurança e status codes com muito mais fidelidade.

**Componentes do frontend (Karma + Jasmine), cinco specs:**
- `login.spec.ts` — formulário inválido não chama o serviço, sucesso navega para `/projects`, credenciais
  recusadas exibem mensagem genérica (sem revelar qual campo falhou).
- `task-dialog.spec.ts` — as regras do prazo: obrigatório, não anterior a hoje, não além de um ano, ano
  com mais de quatro dígitos recusado, e o payload emitido quando a data é válida.
- `task-filters.spec.ts` — a consulta emitida após o debounce, a ausência de emissão antes dele e o
  "Limpar" voltando aos filtros vazios.
- `project-report.spec.ts` — contadores por status na ordem do fluxo e por prioridade da maior para a menor.
- `member-dialog.spec.ts` — o dono do projeto não pode ser removido, e remover um membro sinaliza a
  mudança ao fechar o dialog.

**E2E (Playwright, 2 fluxos):** `frontend/e2e/board.spec.ts` roda contra a aplicação de verdade — login,
criação de projeto, adição de membro, criação de tarefa com prazo, **drag and drop** de card entre colunas
(com os eventos de mouse que o CDK realmente escuta) e o contador do resumo mudando junto; o segundo teste
cobre a busca textual filtrando o board. Cada teste cria e exclui o próprio projeto, então rodar duas
vezes não suja a base. Não usa `webServer`: a stack sobe por `docker compose`, e a URL é configurável.

---

## Endpoints

| Método | Rota | Observação |
|---|---|---|
| `POST` | `/api/auth/login` | público, devolve JWT |
| `GET` | `/api/projects` | projetos em que o usuário é membro |
| `POST` | `/api/projects` | 201, autor vira ADMIN |
| `GET` | `/api/projects/{id}` | detalhe com membros |
| `PUT` `DELETE` | `/api/projects/{id}` | somente ADMIN |
| `POST` | `/api/projects/{id}/members` | somente ADMIN, 201 |
| `DELETE` | `/api/projects/{id}/members/{userId}` | somente ADMIN, 204 |
| `GET` | `/api/projects/{id}/tasks` | `status`, `priority`, `assignee`, `from`, `to`, `sort`, `page`, `size` — `sort=priority` usa a ordem CRITICAL > HIGH > MEDIUM > LOW |
| `POST` | `/api/projects/{id}/tasks` | 201 |
| `PUT` `DELETE` | `/api/projects/{id}/tasks/{taskId}` | |
| `GET` | `/api/projects/{id}/tasks/{taskId}/audit` | histórico de alterações |
| `GET` | `/api/projects/{id}/tasks/search?q=` | busca em título e descrição |
| `GET` | `/api/projects/{id}/report` | `byStatus` / `byPriority` |
| `PATCH` | `/api/tasks/{id}/status` | transição isolada |

**Status codes:** 200/201/204 · 400 validação · 401 sem token · 403 sem permissão · 404 não encontrado ·
409 regra de negócio (WIP limit, transição inválida).

---

## O que eu faria com mais tempo

- **Busca full-text** com `pg_trgm`/`tsvector`, substituindo o `ILIKE '%termo%'` que hoje não usa índice.
- **Audit log por eventos de domínio** (`ApplicationEventPublisher` + listener transacional), tirando a
  escrita de auditoria do caminho síncrono do service.
- **Refresh token** com rotação e revogação, mais um tempo de expiração curto no access token.
- **Concorrência no WIP limit**: lock pessimista na contagem ou constraint no banco, se o volume justificar.
- **Mais testes E2E** cobrindo os caminhos secundários (WIP limit pela interface, trava de CRITICAL,
  remoção de membro com tarefas atribuídas) e testes de contrato da API.
- **Observabilidade**: Actuator, métricas no Micrometer/Prometheus e tracing distribuído, além de logs
  estruturados em JSON com correlation id por requisição.
- **Cache distribuído (Redis)** no lugar do cache em processo, se a API rodar em mais de uma instância —
  hoje cada réplica teria a própria cópia do relatório.
- **Paginação por cursor** na listagem de tarefas, se as listas crescerem a ponto de o `offset` doer.
- **No frontend**: paginação ou scroll infinito no board (hoje ele carrega uma página grande de uma vez),
  edição do papel de um membro já existente e testes de componente do board cobrindo o rollback do drag
  and drop.
