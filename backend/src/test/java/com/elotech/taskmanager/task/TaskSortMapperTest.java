package com.elotech.taskmanager.task;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

class TaskSortMapperTest {

    @Test
    void traduzOrdenacaoPorPrioridadeParaAColunaDeOrdemSemantica() {
        Pageable resolved = TaskSortMapper.resolve(
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "priority")));

        Sort.Order order = resolved.getSort().getOrderFor("priorityRank");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(resolved.getSort().getOrderFor("priority")).isNull();
    }

    @Test
    void preservaAsDemaisPropriedadesEAOrdemDosCriterios() {
        Pageable resolved = TaskSortMapper.resolve(PageRequest.of(1, 10,
                Sort.by(Sort.Order.asc("priority"), Sort.Order.desc("deadline"))));

        assertThat(resolved.getSort()).containsExactly(
                Sort.Order.asc("priorityRank"), Sort.Order.desc("deadline"));
        assertThat(resolved.getPageNumber()).isEqualTo(1);
        assertThat(resolved.getPageSize()).isEqualTo(10);
    }

    @Test
    void devolveOMesmoPageableQuandoNaoHaOrdenacaoPorPrioridade() {
        Pageable original = PageRequest.of(0, 20, Sort.by("createdAt"));

        assertThat(TaskSortMapper.resolve(original)).isSameAs(original);
    }
}
