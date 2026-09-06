package com.elotech.taskmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo critico ponta a ponta contra um PostgreSQL real, com as migrations do Flyway aplicadas.
 * Desabilitado automaticamente quando nao ha Docker disponivel na maquina.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class TaskFlowIntegrationTest {

    private static final String ADMIN_EMAIL = "ana@elotech.com";
    private static final String MEMBER_EMAIL = "bruno@elotech.com";
    private static final String PASSWORD = "password123";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void percorreFluxoDeProjetoTarefaERegrasDeNegocio() throws Exception {
        String adminToken = login(ADMIN_EMAIL);
        String memberToken = login(MEMBER_EMAIL);

        long projectId = createProject(adminToken);
        long memberId = addMember(adminToken, projectId);

        mockMvc.perform(get("/api/projects/{id}", projectId).header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myRole").value("MEMBER"));

        long taskId = createTask(adminToken, projectId, "Ajustar relatorio", "MEDIUM", memberId);

        // transicao invalida: TODO -> DONE
        changeStatus(memberToken, taskId, "DONE").andExpect(status().isConflict());

        changeStatus(memberToken, taskId, "IN_PROGRESS").andExpect(status().isOk());

        // WIP limit: o responsavel ja tem 1 em andamento, mais 4 completam o limite de 5
        for (int i = 0; i < 4; i++) {
            long extraId = createTask(adminToken, projectId, "Tarefa " + i, "LOW", memberId);
            changeStatus(memberToken, extraId, "IN_PROGRESS").andExpect(status().isOk());
        }
        long excedente = createTask(adminToken, projectId, "Excedente", "LOW", memberId);
        changeStatus(memberToken, excedente, "IN_PROGRESS").andExpect(status().isConflict());

        // tarefa CRITICAL so pode ser concluida por ADMIN do projeto
        long criticalId = createTask(adminToken, projectId, "Incidente em producao", "CRITICAL", null);
        changeStatus(memberToken, criticalId, "IN_PROGRESS").andExpect(status().isOk());
        changeStatus(memberToken, criticalId, "DONE").andExpect(status().isForbidden());
        changeStatus(adminToken, criticalId, "DONE").andExpect(status().isOk());

        mockMvc.perform(get("/api/projects/{id}/report", projectId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byStatus.IN_PROGRESS").value(5))
                .andExpect(jsonPath("$.byStatus.DONE").value(1))
                .andExpect(jsonPath("$.byPriority.CRITICAL").value(1));

        mockMvc.perform(get("/api/projects/{id}/tasks/search", projectId)
                        .param("q", "producao")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // ordenacao por prioridade segue a ordem do negocio, nao a alfabetica do enum
        mockMvc.perform(get("/api/projects/{id}/tasks", projectId)
                        .param("sort", "priority,desc")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].priority").value("CRITICAL"))
                .andExpect(jsonPath("$.content[1].priority").value("MEDIUM"));

        // WIP limit tambem barra a troca de responsavel de uma tarefa ja em andamento
        long semResponsavel = createTask(adminToken, projectId, "Sem responsavel", "LOW", null);
        changeStatus(adminToken, semResponsavel, "IN_PROGRESS").andExpect(status().isOk());
        updateTask(adminToken, projectId, semResponsavel, "Sem responsavel", "LOW", memberId)
                .andExpect(status().isConflict());

        // prazo e obrigatorio e limitado a um ano a partir de hoje
        mockMvc.perform(post("/api/projects/{id}/tasks", projectId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "Sem prazo", "priority", "LOW"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.deadline").exists());

        mockMvc.perform(post("/api/projects/{id}/tasks", projectId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "Prazo distante", "priority", "LOW",
                                "deadline", LocalDate.now().plusYears(1).plusDays(1).toString()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.deadline").exists());

        // o relatorio e cacheado, mas cada escrita de tarefa invalida o projeto
        mockMvc.perform(get("/api/projects/{id}/report", projectId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byStatus.IN_PROGRESS").value(6));
    }

    @Test
    void bloqueiaAcessoDeQuemNaoEMembroDoProjeto() throws Exception {
        String adminToken = login(ADMIN_EMAIL);
        long projectId = createProject(adminToken);

        mockMvc.perform(get("/api/projects/{id}/tasks", projectId)
                        .header("Authorization", bearer(login("carla@elotech.com"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/projects/{id}/tasks", projectId))
                .andExpect(status().isUnauthorized());
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return readField(result, "token");
    }

    private long createProject(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/projects")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Portal do Cidadao", "description", "Modulo de atendimento"))))
                .andExpect(status().isCreated())
                .andReturn();
        return Long.parseLong(readField(result, "id"));
    }

    private long addMember(String token, long projectId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/projects/{id}/members", projectId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", MEMBER_EMAIL, "role", "MEMBER"))))
                .andExpect(status().isCreated())
                .andReturn();
        return Long.parseLong(readField(result, "userId"));
    }

    private long createTask(String token, long projectId, String title, String priority, Long assigneeId)
            throws Exception {
        var body = new HashMap<String, Object>();
        body.put("title", title);
        body.put("description", "Descricao de " + title);
        body.put("priority", priority);
        body.put("deadline", LocalDate.now().plusDays(30).toString());
        body.put("assigneeId", assigneeId);

        MvcResult result = mockMvc.perform(post("/api/projects/{id}/tasks", projectId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return Long.parseLong(readField(result, "id"));
    }

    private ResultActions updateTask(String token, long projectId, long taskId, String title,
                                     String priority, Long assigneeId) throws Exception {
        var body = new HashMap<String, Object>();
        body.put("title", title);
        body.put("priority", priority);
        body.put("deadline", LocalDate.now().plusDays(30).toString());
        body.put("assigneeId", assigneeId);

        return mockMvc.perform(put("/api/projects/{id}/tasks/{taskId}", projectId, taskId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body)));
    }

    private ResultActions changeStatus(String token, long taskId, String status) throws Exception {
        return mockMvc.perform(patch("/api/tasks/{id}/status", taskId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("status", status))));
    }

    private String readField(MvcResult result, String field) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get(field).asText();
    }

    private String json(Map<String, ?> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
