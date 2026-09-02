# Task Manager — Desafio Técnico Elotech

Sistema simplificado de gerenciamento de tarefas para equipes: projetos, membros com papel por projeto,
tarefas com máquina de estados, WIP limit e relatório agregado.

Monorepo com `backend/` (Spring Boot 3) e `frontend/` (Angular).

---

## Como rodar

### Backend

Pré-requisitos: Java 21 e Docker.

```bash
# 1. sobe o PostgreSQL (as migrations do Flyway rodam na subida da aplicação)
docker compose up -d

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

### Frontend

Pré-requisito: Node 20+ (desenvolvido com Node 24). O backend precisa estar no ar.

```bash
cd frontend
npm install
npm start   # http://localhost:4200
```

O `ng serve` usa `proxy.conf.json` para encaminhar `/api` para `http://localhost:8080`, então não há
URL de backend hard-coded no código nem CORS em desenvolvimento.

- Testes de componente: `npm test -- --watch=false --browsers=ChromeHeadless`
- Build de produção: `npm run build`

**Telas**

| Rota | Tela |
|---|---|
| `/login` | autenticação; guarda `authGuard` protege as demais |
| `/projects` | projetos em que o usuário é membro, com criação de projeto |
| `/projects/:projectId` | board kanban com drag and drop, criação/edição de tarefa e gestão de membros |

---

## Stack e versões

**Backend**
- Java 21, Spring Boot 3.3.13, Maven (via wrapper `./mvnw`)
- Spring Web, Spring Security, Spring Data JPA, Bean Validation
- JWT com jjwt 0.12.7
- PostgreSQL 16 + Flyway
- springdoc-openapi 2.6.0 (Swagger UI)
- Lombok (apenas nas entidades)
- JUnit 5 + Mockito + AssertJ; Testcontainers PostgreSQL

**Frontend**
- Angular 20.3 (standalone components, signals para estado, rotas com lazy loading)
- Angular Material / CDK 20.2 (drag and drop no board)
- HTTP interceptor funcional para o JWT
- Karma + Jasmine

---

## Arquitetura

Camadas pragmáticas (`controller → service → repository`), **organizadas por feature**:

```
com.elotech.taskmanager
├── auth      login, JWT, filtro, SecurityConfig
├── user      entidade e repository
├── project   controller, service, repository, ProjectAccessService, domain, dto
├── task      controller, service, repository, specifications, audit, domain, dto
└── common    exceptions + ProblemDetail, paginação, security, OpenAPI
```

Entidades JPA no domínio de cada feature; DTOs são `record` com mapeamento manual via método `from(...)`
estático no próprio record.

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

**JPA Specifications para os filtros.** Os predicados são compostos apenas para os filtros efetivamente
informados (`TaskSpecifications.build`), evitando o clássico `where 1=1` com `if` em SQL string. Ordenação
e paginação usam `Pageable`/`Sort` nativos do Spring Data; a resposta vem em `PageResponse` com `page`,
`size`, `totalElements` e `totalPages`.

**Busca textual com ILIKE — e o seu limite.** `GET /tasks/search?q=` usa uma query nativa com `ILIKE` e o
termo passado como parâmetro (nunca concatenado). O índice `idx_tasks_title_lower` acelera busca por
prefixo, mas um padrão `%termo%` não usa índice B-tree — esse é o tradeoff consciente. Em escala, a
migração natural é `pg_trgm` com índice GIN, ou full-text (`tsvector` + `to_tsquery`) se a busca virar
requisito de produto.

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
o teste é pulado em vez de quebrar o build.

**Signals no frontend, não NgRx.** Estado nativo do Angular, sem actions/reducers/effects para três telas.
O estado do board é um `signal<Task[]>` e as colunas derivam dele — mover um card é uma atualização
imutável desse array. NgRx só se paga quando há muitos consumidores do mesmo estado e necessidade de
time-travel/devtools; aqui seria mais boilerplate do que benefício.

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
- **Tarefa sem responsável não sofre WIP limit** — não há de quem contar as tarefas.
- **`from`/`to` filtram o `deadline`** da tarefa (o parâmetro não foi qualificado no enunciado).
- **Projeto inexistente retorna 404; projeto existente sem vínculo retorna 403.** É a resposta mais útil
  para o cliente da API; o custo é revelar a existência de um id, aceitável neste domínio.
- **Quem cria o projeto vira ADMIN dele** e não pode ser removido dos membros — do contrário o projeto
  poderia ficar sem nenhum administrador.
- **Membros (não só ADMINs) criam, editam e excluem tarefas.** ADMIN é exigido para gerir o projeto e seus
  membros, e para concluir tarefas CRITICAL.
- **`assignee` precisa ser membro do projeto** da tarefa (409 caso contrário).

---

## Testes: o que foi priorizado

A cobertura foi direcionada para o que quebra em produção e é caro de descobrir tarde — as regras de
negócio — e não para percentual de linhas.

**Unitários (sem Spring, com Mockito):**
- `TaskStateMachineTest` — cada transição válida e cada inválida da matriz acima, mais a trava de CRITICAL
  (bloqueia MEMBER, libera ADMIN, não se aplica na reabertura).
- `TaskServiceTest` — WIP limit exatamente no limite e acima dele, tarefa sem responsável, propagação da
  falha de autorização, responsável fora do projeto, registro no audit log.
- `ProjectAccessServiceTest` — membro, não-membro (403), ADMIN vs MEMBER, projeto inexistente (404).

**Integração (1, com Testcontainers):** `TaskFlowIntegrationTest` percorre o fluxo crítico ponta a ponta
contra um PostgreSQL real — login → cria projeto → adiciona membro → cria tarefa → viola transição inválida
(409) → satura o WIP limit (409) → tenta concluir CRITICAL como MEMBER (403) e conclui como ADMIN (200) →
confere o relatório e a busca textual. Um segundo teste cobre 403 para não-membro e 401 sem token.

Não foram escritos testes de controller isolados (`@WebMvcTest`): o teste de integração já exercita
serialização, validação, filtros de segurança e status codes com muito mais fidelidade.

**Frontend:** `login.spec.ts` cobre o componente de login pelo DOM — formulário inválido não chama o
serviço, sucesso navega para `/projects`, e credenciais recusadas exibem mensagem genérica (sem revelar
qual campo falhou).

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
| `GET` | `/api/projects/{id}/tasks` | `status`, `priority`, `assignee`, `from`, `to`, `sort`, `page`, `size` |
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
- **Mais testes E2E** cobrindo os caminhos secundários (edição de projeto, remoção de membro com tarefas
  atribuídas) e testes de contrato da API.
- **Observabilidade**: Actuator, métricas no Micrometer/Prometheus e tracing distribuído, além de logs
  estruturados em JSON com correlation id por requisição.
- **Paginação por cursor** na listagem de tarefas, se as listas crescerem a ponto de o `offset` doer.
- **No frontend**: filtros e busca na tela do board (a API já suporta), histórico de auditoria da tarefa
  no dialog de edição, e testes de componente do board cobrindo o rollback do drag and drop.
