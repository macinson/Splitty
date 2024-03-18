package server.database;

import commons.ParticipantId;
import commons.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Find all payments made by a specific payer
    List<Payment> findByPayerId(ParticipantId payerId);

    // Find all payments received by a specific payee
    List<Payment> findByPayeeId(ParticipantId payeeId);

}