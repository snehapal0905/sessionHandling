package com.spendly.spendly.service;

import com.spendly.spendly.domain.AppUser;
import com.spendly.spendly.domain.SavingsGoal;
import com.spendly.spendly.dto.GoalForm;
import com.spendly.spendly.repository.SavingsGoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final SavingsGoalRepository goalRepository;

    public SavingsGoal get(AppUser user, Long id) {
        return goalRepository.findByIdAndUser(id, user).orElseThrow(() -> new NotFoundException("Goal", id));
    }

    public List<SavingsGoal> all(AppUser user) {
        return goalRepository.findByUserOrderByTargetDateAscNameAsc(user);
    }

    @Transactional
    public SavingsGoal save(AppUser user, GoalForm form) {
        SavingsGoal g = form.isNew() ? new SavingsGoal() : get(user, form.getId());
        g.setUser(user);
        g.setName(form.getName().trim());
        g.setTargetAmount(form.getTargetAmount());
        g.setSavedAmount(form.getSavedAmount());
        g.setTargetDate(form.getTargetDate());
        g.setColor(form.getColor().toLowerCase());
        g.setNotes(form.getNotes() == null || form.getNotes().isBlank() ? null : form.getNotes().trim());
        return goalRepository.save(g);
    }

    /** Adds money to a goal; a negative amount takes money out (never below zero). */
    @Transactional
    public SavingsGoal contribute(AppUser user, Long id, BigDecimal amount) {
        if (amount == null || amount.signum() == 0) {
            throw new BusinessException("Enter an amount to add or withdraw");
        }
        SavingsGoal g = get(user, id);
        BigDecimal saved = g.getSavedAmount().add(amount);
        if (saved.signum() < 0) {
            throw new BusinessException("You can withdraw at most " + g.getSavedAmount().stripTrailingZeros().toPlainString() + " from \"" + g.getName() + "\"");
        }
        g.setSavedAmount(saved);
        return g;
    }

    @Transactional
    public void delete(AppUser user, Long id) {
        goalRepository.delete(get(user, id));
    }
}
