package server.database;

import commons.Debt;
import commons.Event;
import commons.Expense;
import commons.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DebtRepository extends JpaRepository<Debt, Integer> {
    @Query("SELECT d FROM Debt d JOIN Expense e ON e = d.expense WHERE e.event = :event")
    List<Debt> findByEvent(@Param("event") Event event);

    List<Debt> findByExpense(Expense expense);

    List<Debt> findByParticipant(Participant participant);

    List<Debt> deleteDebtsByExpense(Expense expense);
}
