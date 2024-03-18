package commons;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Column;
import java.util.Date;
import java.util.Objects;

@Entity
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Participant payer; // The participant who made the payment

    @ManyToOne
    private Participant payee; // The participant who received the payment

    @Column(nullable = false)
    private double amount; // The amount of money transferred

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date transactionDate; // The date and time when the transaction occurred

    // Constructors
    public Payment() {
    }

    public Payment(Participant payer, Participant payee, double amount, Date transactionDate) {
        this.payer = payer;
        this.payee = payee;
        this.amount = amount;
        this.transactionDate = transactionDate;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Participant getPayer() {
        return payer;
    }

    public void setPayer(Participant payer) {
        this.payer = payer;
    }

    public Participant getPayee() {
        return payee;
    }

    public void setPayee(Participant payee) {
        this.payee = payee;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public Date getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Payment that = (Payment) o;

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}