package com.elotech.taskmanager.task;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Traduz {@code sort=priority} para a coluna derivada {@code priorityRank}, para que a ordenacao
 * siga a ordem semantica (CRITICAL > HIGH > MEDIUM > LOW) e nao a alfabetica do enum gravado.
 */
final class TaskSortMapper {

    private static final String PRIORITY = "priority";
    private static final String PRIORITY_RANK = "priorityRank";

    private TaskSortMapper() {
    }

    static Pageable resolve(Pageable pageable) {
        Sort sort = pageable.getSort();
        if (sort.getOrderFor(PRIORITY) == null) {
            return pageable;
        }
        Sort mapped = Sort.by(sort.stream()
                .map(order -> PRIORITY.equals(order.getProperty())
                        ? new Sort.Order(order.getDirection(), PRIORITY_RANK)
                        : order)
                .toList());
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), mapped);
    }
}
