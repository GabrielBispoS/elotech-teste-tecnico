package com.elotech.taskmanager.project;

import com.elotech.taskmanager.common.exception.ForbiddenOperationException;
import com.elotech.taskmanager.common.exception.ResourceNotFoundException;
import com.elotech.taskmanager.project.domain.Project;
import com.elotech.taskmanager.project.domain.ProjectMembership;
import com.elotech.taskmanager.project.domain.ProjectRole;
import com.elotech.taskmanager.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectAccessServiceTest {

    private static final Long PROJECT_ID = 1L;
    private static final Long USER_ID = 10L;

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectMembershipRepository membershipRepository;

    @InjectMocks
    private ProjectAccessService accessService;

    private Project project;
    private User user;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(PROJECT_ID);
        user = new User("Ana", "ana@elotech.com", "hash");
        user.setId(USER_ID);
    }

    @Test
    void retornaProjetoQuandoUsuarioEMembro() {
        givenProjectExists();
        givenMembership(ProjectRole.MEMBER);

        assertThat(accessService.requireMember(PROJECT_ID, USER_ID)).isEqualTo(project);
    }

    @Test
    void negaAcessoQuandoUsuarioNaoEMembro() {
        givenProjectExists();
        when(membershipRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accessService.requireMember(PROJECT_ID, USER_ID))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("nao e membro");
    }

    @Test
    void negaOperacaoDeAdminQuandoUsuarioEApenasMembro() {
        givenProjectExists();
        givenMembership(ProjectRole.MEMBER);

        assertThatThrownBy(() -> accessService.requireAdmin(PROJECT_ID, USER_ID))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("administradores");
    }

    @Test
    void permiteOperacaoDeAdminQuandoUsuarioEAdmin() {
        givenProjectExists();
        givenMembership(ProjectRole.ADMIN);

        assertThat(accessService.requireAdmin(PROJECT_ID, USER_ID)).isEqualTo(project);
    }

    @Test
    void retorna404QuandoProjetoNaoExiste() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accessService.requireMember(PROJECT_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private void givenProjectExists() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
    }

    private void givenMembership(ProjectRole role) {
        when(membershipRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
                .thenReturn(Optional.of(new ProjectMembership(project, user, role)));
    }
}
