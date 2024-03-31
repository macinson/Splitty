package client.scenes;

import client.utils.ServerUtils;
import commons.Expense;
import commons.Participant;
import commons.Type;
import commons.dto.DebtDTO;
import commons.dto.ExpenseDTO;
import commons.dto.ParticipantDTO;
import javafx.animation.PauseTransition;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import javafx.util.Duration;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

public class EditExpenseCtrl {

    private final ServerUtils serverUtils;
    private final int eventCode = 1;
    private Expense expense;
    private final MainCtrl mainCtrl;
    private final SplittyOverviewCtrl splittyCtrl;


    @FXML
    public Label titleLabel;

    @FXML
    private ComboBox<Participant> personComboBox;

    //all the things needed for the addExpense
    @FXML
    private TextArea whatFor;

    @FXML
    private DatePicker dateSelect;

    @FXML
    private ListView splitList;

    @FXML
    private TextField amount;

    @FXML
    private ComboBox category;
    @FXML
    private Label error;
    private List<Participant> participant;

    @FXML
    private Label editExpenseLabel;
    @FXML
    private Label whoPaid;
    @FXML
    private Label howMuch;
    @FXML
    private Label when;
    @FXML
    private Label howToSplit;
    @FXML
    private Label description;
    @FXML
    private Label expenseTypetext;

    @FXML
    private Button cancel;
    @FXML
    private Button edit;
    @FXML
    private Button abort;
    @FXML
    private RadioButton selectAll;
    @FXML
    private RadioButton selectSome;

    @FXML
    private RadioButton sharedExpense;

    @FXML
    private RadioButton givingMoneyToSomeone;

    @FXML
    private ToggleGroup selectionToggles;

    @FXML
    private HBox receiverHBox;

    @FXML
    private ListView<Participant> receiverListView;

    private Participant payer;

    private ObservableList<Participant> rest;

    private Set<Participant> owing;

    private boolean isSharedExpense;

    @FXML
    public Label editExpenseError;

    @FXML
    public Label payerError;

    @FXML
    private Label amountError;

    @FXML
    private Label dateInvalidError;

    @Inject
    public EditExpenseCtrl(ServerUtils serverUtils, MainCtrl mainCtrl,
                          SplittyOverviewCtrl splittyCtrl) {
        this.serverUtils = serverUtils;
        this.mainCtrl = mainCtrl;
        this.splittyCtrl = splittyCtrl;
        rest = FXCollections.observableArrayList();
        owing = new HashSet<>();
    }

    @FXML
    public void initialize(URL location, ResourceBundle resources) {
        setSplitListsUp();
        setTogglesUp();
        setCategoriesUp();
    }

    private void setCategoriesUp() {
        category.setCellFactory(param -> new ListCell<Type>() {
            @Override
            protected void updateItem(Type item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString());
                }
            }
        });
        category.setButtonCell(new ListCell<Type>(){
            @Override
            protected void updateItem(Type type, boolean b) {
                super.updateItem(type, b);
                if(type == null || b){
                    setText("Select category");
                }else{
                    setText("" + type);
                }
            }
        });
        this.category.setItems(
            FXCollections.observableArrayList(Type.Food, Type.Drinks, Type.Travel, Type.Other));
    }

    private void setTogglesUp() {
        selectAll.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue,
                                Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    if (payer == null) {
                        System.out.println("Select payer");
                        return;
                    }
                    owing.addAll(rest);
                    owing.remove(payer);
                    splitList.setVisible(false);
                }
            }
        });
        selectSome.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue,
                                Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    if (payer == null) {
                        System.out.println("Select payer");
                        return;
                    }
                    owing.clear();
                    splitList.refresh();
                    splitList.setVisible(true);
                }
            }
        });

        sharedExpense.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue,
                                Boolean oldValue, Boolean newValue) {
                if(newValue){
                    isSharedExpense = true;
                    selectAll.setVisible(true);
                    selectSome.setVisible(true);
                    howToSplit.setVisible(true);
                    receiverHBox.setVisible(false);
                    if(selectSome.isSelected()) splitList.setVisible(true);
                }
            }
        });

        givingMoneyToSomeone.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue,
                                Boolean oldValue, Boolean newValue) {
                if(newValue){
                    isSharedExpense = false;
                    selectAll.setVisible(false);
                    selectSome.setVisible(false);
                    howToSplit.setVisible(false);
                    splitList.setVisible(false);
                    receiverHBox.setVisible(true);
                }
            }
        });
    }

    @FXML
    public void back() {
        mainCtrl.showSplittyOverview(eventCode);
    }

    /**
     * Sets the title of the event
     *
     * @param title event's title
     */
    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    /**
     * this collects all data and asks the splittycontroller to add the
     * expense to the list but that might not be necessary when the database works
     */
    @FXML
    @Transactional
    public void editExpense() {
        boolean error = false;
        Date date = null;
        //link these to participants and then add the expense
        if (dateSelect.getValue() == null) {
            dateSelect.setPromptText("invalid Date");
        }

        Type type = (Type) category.getValue();
        if (type == null) type= Type.Other;
        Participant oldPayer = personComboBox.getValue();
        if (oldPayer == null) {
            error = true;
            payerError.setVisible(true);
        }

        double amountDouble = 0.0;
        try {
            if (amount.getText() == null || amount.getText().isEmpty()) {
                amountError.setText("An amount is required");
                amountError.setVisible(true);
                return;
            }
            amountDouble = Double.parseDouble(amount.getText());
            if (amountDouble <= 0.0) {
                amountError.setVisible(true);
                amountError.setText("Amount cannot be negative or zero*");
                return;
            }
        } catch (NumberFormatException e) {
            amountError.setVisible(true);
            amountError.setText("Not a number, format e.g 13.99");
            error = true;
        }

        Participant receiver = receiverListView.getSelectionModel().getSelectedItem();
        if(!isSharedExpense && receiver == null) {
            error = true;
        }
        if(error) return;

        String description = whatFor.getText();

        try {
            LocalDate localDate = dateSelect.getValue();
            date = java.sql.Date.valueOf(localDate);
            //add to database
            ExpenseDTO
                exp =
                new ExpenseDTO(eventCode, description, type, date, amountDouble, payer.getUuid(),isSharedExpense);
            Expense editedExpense = serverUtils.updateExpense(expense.getExpenseId(), exp);
            if(isSharedExpense) editSharedExpense(editedExpense, oldPayer, amountDouble);
            else editGivingMoneyToSomeone(editedExpense, oldPayer, amountDouble, receiver);
            serverUtils.generatePaymentsForEvent(eventCode);
            back();
        } catch (Exception e) {
            editExpenseError.setVisible(true);
            PauseTransition visiblePause = new PauseTransition(Duration.seconds(3));
            visiblePause.setOnFinished(event1 -> editExpenseError.setVisible(false));
            visiblePause.play();
        }
        splittyCtrl.fetchExpenses();
    }

    private void editGivingMoneyToSomeone(Expense editedExpense, Participant oldPayer, double amountDouble, Participant receiver) {
        Participant newReceiver = serverUtils.getParticipantById(receiver.getEvent().getId(),receiver.getUuid());
        serverUtils.saveDebt(
            new DebtDTO(-amountDouble, eventCode, editedExpense.getExpenseId(), newReceiver.getUuid()));
        serverUtils.updateParticipant(newReceiver.getUuid(),
            new ParticipantDTO(newReceiver.getName(), newReceiver.getBalance() - amountDouble, newReceiver.getIBan(),
                newReceiver.getBIC(), newReceiver.getEmail(), newReceiver.getAccountHolder(), newReceiver.getEvent().getId(),
                newReceiver.getUuid()));
        Participant newPayer = serverUtils.getParticipantById(oldPayer.getEvent().getId(), oldPayer.getUuid());
        serverUtils.saveDebt(
            new DebtDTO(amountDouble, eventCode, editedExpense.getExpenseId(), newPayer.getUuid()));
        serverUtils.updateParticipant(newPayer.getUuid(),
            new ParticipantDTO(newPayer.getName(), newPayer.getBalance() + amountDouble, newPayer.getIBan(),
                newPayer.getBIC(), newPayer.getEmail(), newPayer.getAccountHolder(), newPayer.getEvent().getId(),
                newPayer.getUuid()));
    }

    private void editSharedExpense(Expense editedExpense, Participant oldPayer, double amountDouble) {
        double amountPerPerson = editedExpense.getTotalExpense() / (owing.size()+1);
        for (Participant oldP : owing) {
            Participant p = serverUtils.getParticipantById(oldP.getEvent().getId(),oldP.getUuid());
            serverUtils.saveDebt(
                new DebtDTO(-amountPerPerson, eventCode, editedExpense.getExpenseId(), p.getUuid()));
            serverUtils.updateParticipant(p.getUuid(),
                new ParticipantDTO(p.getName(), p.getBalance() - amountPerPerson, p.getIBan(),
                    p.getBIC(), p.getEmail(), p.getAccountHolder(), p.getEvent().getId(),
                    p.getUuid()));
        }
        Participant newPayer = serverUtils.getParticipantById(oldPayer.getEvent().getId(), oldPayer.getUuid());
        serverUtils.saveDebt(
            new DebtDTO(amountDouble - amountPerPerson, eventCode, editedExpense.getExpenseId(), newPayer.getUuid()));
        serverUtils.updateParticipant(newPayer.getUuid(),
            new ParticipantDTO(newPayer.getName(), newPayer.getBalance() + amountDouble - amountPerPerson, newPayer.getIBan(),
                newPayer.getBIC(), newPayer.getEmail(), newPayer.getAccountHolder(), newPayer.getEvent().getId(),
                newPayer.getUuid()));
    }


    public void setParticipant(List<Participant> participant) {
        this.participant = participant;
    }

    // This part is never used because expenses doesn't save who should pay for it only the payer
    // this should be changed eventually but that is not part of the ExpenseController

    public void setSplitListsUp() {
        splitList.setItems(rest);
        splitList.setCellFactory(new Callback<ListView<Participant>, ListCell<Participant>>() {
            @Override
            public ListCell call(ListView listView) {
                return new ListCell<Participant>() {
                    @Override
                    protected void updateItem(Participant participant, boolean b) {
                        super.updateItem(participant, b);
                        if (participant == null || b) {
                            setGraphic(null);
                        } else {
                            RadioButton button = new RadioButton();
                            if(owing.contains(participant)) button.setSelected(true);
                            button.selectedProperty().addListener(new ChangeListener<Boolean>() {
                                @Override
                                public void changed(
                                    ObservableValue<? extends Boolean> observableValue,
                                    Boolean wasPreviouslySelected, Boolean isNowSelected) {
                                    if (isNowSelected) {
                                        owing.add(participant);
                                    } else {
                                        owing.remove(participant);
                                    }
                                }
                            });
                            button.setText(participant.getName());
                            setGraphic(button);
                        }
                    }
                };
            }
        });
        receiverListView.setItems(rest);
        receiverListView.setCellFactory(
            new Callback<ListView<Participant>, ListCell<Participant>>() {
                @Override
                public ListCell<Participant> call(ListView<Participant> participantListView) {
                    return new ListCell<>(){
                        @Override
                        protected void updateItem(Participant participant, boolean b) {
                            super.updateItem(participant, b);
                            if(participant == null || b){
                                setText(null);
                            }else{
                                setText(participant.getName());
                            }
                        }
                    };
                }
            });
    }


    //Setters for all the text attributes


//    public void setError(String text) {
//        this.error = error;
//    }

    public void refresh(Expense expense){
        isSharedExpense = expense.isSharedExpense();
        ObservableList<Participant> list = FXCollections.observableArrayList();
        List<Participant> allparticipants;
        try {
            allparticipants = serverUtils.getParticipants(eventCode);
        } catch (Exception e) {
            allparticipants = new ArrayList<>();
        }
        list.addAll(allparticipants);
        setComboboxUp(list);
        setSplitListsUp();
        setCategoriesUp();
        setTogglesUp();
        this.expense = expense;
        owing.clear();
        List<Participant> owingFromDb = serverUtils.getDebtByExpense(expense.getEvent().getId(), expense.getExpenseId()).stream().filter(x -> x.getBalance() < 0).map(x -> x.getParticipant()).toList();
        personComboBox.setValue(expense.getPayer());
        dateSelect.setValue(expense.getDate().toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate());
        whatFor.setText(expense.getDescription());
        category.setValue(expense.getType());
        amount.setText(expense.getTotalExpense() + "");
        payer = expense.getPayer();
        rest.clear();
        rest.addAll(allparticipants);
        rest.remove(payer);
        if(isSharedExpense) {
            sharedExpense.setSelected(true);
            if (owingFromDb.size() == rest.size()) {
                selectAll.setSelected(true);
            } else {
                selectSome.setSelected(true);
                owing.addAll(owingFromDb);
            }
        }else{
            givingMoneyToSomeone.setSelected(true);
            Participant receiver = owingFromDb.getFirst();
            receiverListView.getSelectionModel().select(receiver);
        }
    }

    private void setComboboxUp(ObservableList<Participant> list) {
        personComboBox.setItems(list);
        personComboBox.setCellFactory(param -> new ListCell<Participant>() {
            @Override
            protected void updateItem(Participant item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                    setEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<Event>() {
                        @Override
                        public void handle(Event event) {
                            payer = item;
                            rest.clear();
                            rest.addAll(list);
                            rest.remove(item);
                            if (selectAll.isSelected()) {
                                owing.addAll(rest);
                                owing.remove(payer);
                                splitList.setVisible(false);
                            }
                            if (selectSome.isSelected()) {
                                owing.clear();
                                splitList.setVisible(true);
                            }
                        }
                    });
                }
            }
        });
        personComboBox.setButtonCell(new ListCell<Participant>() {
            @Override
            protected void updateItem(Participant item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("Select who paid");
                } else {
                    setText(item.getName());
                }
            }
        });
    }

    public void setEditExpenseLabel(String text) {
        this.editExpenseLabel.setText(text);
    }

    public void setWhoPaid(String text) {
        this.whoPaid.setText(text);
    }

    public void setHowMuch(String text) {
        this.howMuch.setText(text);
    }

    public void setWhen(String text) {
        this.when.setText(text);
    }

    public void setHowToSplit(String text) {
        this.howToSplit.setText(text);
    }

    public void setDescription(String text) {
        this.description.setText(text);
    }

    public void setExpenseTypetext(String text) {
        this.expenseTypetext.setText(text);
    }

    public void setCancel(String text) {
        this.cancel.setText(text);
    }

    public void setEdit(String text) {
        this.edit.setText(text);
    }

    public void setAbort(String text) {
        this.abort.setText(text);
    }

    public void setSelectAll(String text) {
        this.selectAll.setText(text);
    }

    public void setSelectWhoPaid(String text) {
        this.personComboBox.setPromptText(text);
    }

    public void setExpenseTypeBox(String text) {
        this.category.setPromptText(text);
    }

    @FXML
    public void onKeyPressed(KeyEvent press) {
        if (press.getCode() == KeyCode.ESCAPE) {
            back();
        }
    }

    public void resetPayerErrors() {
        payerError.setVisible(false);
    }

    public void resetAmountErrors(KeyEvent keyEvent) {
        amountError.setVisible(false);
    }

    public void resetDateErrors() {
        dateInvalidError.setVisible(false);
    }
}
