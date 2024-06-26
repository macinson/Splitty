package commons;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.Date;

import java.util.List;
import java.util.Objects;

@Entity
@IdClass(ExpenseId.class)
public class Expense {
    @Id
    @PrimaryKeyJoinColumn
    @ManyToOne
    private Event event;

    @JsonIgnore
    @OneToMany(mappedBy = "expense", cascade = CascadeType.REMOVE)
    private List<Debt> debts;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public int expenseId;
    @Column(nullable = false)
    private String description;
    // all associated participants of the expense and how much they owe or are owed

    @ManyToOne
    private Tag tag; // type of expense (i.e. food, drinks, travel)

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date date; // date of expense
    @Column(nullable = false)
    private double totalExpense; // the amount of money of the expense
    @PrimaryKeyJoinColumn
    @ManyToOne
    private Participant payer; // the participant who paid

    @Column(nullable = false)
    private boolean sharedExpense;

    public Expense() {

    }

    public Expense(Event event, String description, Tag tag, Date date,
                   double totalExpense, Participant payer, boolean sharedExpense) {
        this.event = event;
        this.description = description;
        this.tag = tag;
        this.date = date;
        this.totalExpense = totalExpense;
        this.payer = payer;
        this.sharedExpense = sharedExpense;
    }

    @PreUpdate
    @PrePersist
    @PreRemove
    public void updateLastActivity(){
        payer.getEvent().updateActivityDate();
    }

    public void setSharedExpense(boolean sharedExpense) {
        this.sharedExpense = sharedExpense;
    }

    public boolean isSharedExpense() {
        return sharedExpense;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public Tag getTag() {
        return tag;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
    }



    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public double getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(double totalExpense) {
        this.totalExpense = totalExpense;
    }

    public Participant getPayer() {
        return payer;
    }

    public void setPayer(Participant payer) {
        this.payer = payer;
    }

    public Event getEvent() {
        return event;
    }

    public int getExpenseId() {
        return expenseId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Expense expense = (Expense) o;
        return expenseId == expense.expenseId &&
            Double.compare(totalExpense, expense.totalExpense) == 0 &&
            sharedExpense == expense.sharedExpense && Objects.equals(event, expense.event) &&
            Objects.equals(description, expense.description) && tag.equals(expense.tag) &&
            Objects.equals(date, expense.date) &&
            Objects.equals(payer, expense.payer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(event, expenseId, description, tag, date, totalExpense, payer,
            sharedExpense);
    }

    @Override
    public String toString() {
        return "This is an expense:\n" + description + "\nThe expense type is: " + this.tag.getName()
            + ".\nThe total amount spent is: "
            + totalExpense + "."
            + "\nThe person who paid was: " + payer.getUuid() + ", on " + date
            + ".";

    }

}
