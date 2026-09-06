package com.elotech.taskmanager.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;

/** Aceita apenas datas entre hoje e o limite de anos configurado; nulo fica para o {@code @NotNull}. */
@Documented
@Constraint(validatedBy = DeadlineWindowValidator.class)
@Target({FIELD, PARAMETER, ANNOTATION_TYPE, RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface DeadlineWindow {

    String message() default "o prazo deve estar entre hoje e um ano a partir de hoje";

    int years() default 1;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
