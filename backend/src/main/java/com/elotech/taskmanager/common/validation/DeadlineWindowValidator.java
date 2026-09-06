package com.elotech.taskmanager.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class DeadlineWindowValidator implements ConstraintValidator<DeadlineWindow, LocalDate> {

    private int years;

    @Override
    public void initialize(DeadlineWindow constraint) {
        this.years = constraint.years();
    }

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        LocalDate hoje = LocalDate.now();
        return !value.isBefore(hoje) && !value.isAfter(hoje.plusYears(years));
    }
}
