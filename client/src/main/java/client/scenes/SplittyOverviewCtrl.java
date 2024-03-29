package client.scenes;

import client.utils.Config;
import client.utils.ServerUtils;
import commons.Expense;
import commons.Participant;
import commons.Type;
import commons.dto.ExpenseDTO;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;

import javafx.util.Duration;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Callback;



import javax.inject.Inject;
import java.net.URL;
import java.util.*;

public class SplittyOverviewCtrl implements Initializable {

    //We need to store the eventCode right here
    private int eventCode;

    private final ServerUtils serverUtils;
    private final MainCtrl mainCtrl;
    private boolean admin;

    private Config config;

    //these are for the css:
    @FXML
    private AnchorPane background;
    @FXML
    private Label expenses;
    @FXML
    private Label participants;
    @FXML
    private Button backButton;
    @FXML
    private Button settleDebtsButton;
    @FXML
    private Button statisticsButton;
    @FXML
    private Button addExpenseButton;
    @FXML
    public Label noExpenseError;
    @FXML
    public Label expenseNotDeletedError;
    @FXML
    private Button editEvent;

    @FXML
    private Label eventCreatedLabel;

    @FXML
    private Tab paidByMe;

    @FXML
    private Tab involvingMe;

    @FXML
    private Button deleteExpenseButton;


    @FXML
    private Button sendInvites;
    @FXML
    private Label titleLabel;

    @FXML
    public Tab allExpenses;
    private ListView<Expense> allExpensesList;
    private ListView<Expense> paidByMeList;
    private ListView<Expense> includingMeList;
    @FXML
    private TabPane expensesTabPane;

    @FXML
    public Button leaveButton;
    @FXML
    public Button leaveConfirmationButton;
    @FXML
    public Button cancelLeaveButton;
    @FXML
    public Label confirmationLabel;
    @FXML
    public Label joinedEventLabel;

    @FXML
    private ListView<Participant> participantListView;
    @Inject
    public SplittyOverviewCtrl(ServerUtils server, MainCtrl mainCtrl, Config config){
        this.serverUtils = server;
        this.mainCtrl = mainCtrl;

        admin = false;

        this.config = config;
    }

    @FXML
    public void initialize(URL location, ResourceBundle resources) {
        mainCtrl.setButtonRedProperty(deleteExpenseButton);
        mainCtrl.setButtonRedProperty(leaveButton);
        mainCtrl.setButtonRedProperty(leaveConfirmationButton);
        mainCtrl.setButtonGreenProperty(cancelLeaveButton);
        participantListView.setCellFactory(param -> new ListCell<Participant>(){
            @Override
            protected void updateItem(Participant item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
//        fetchExpenses();
//        fetchParticipants();
    }

    public void setEventCode(int eventCode){
        this.eventCode = eventCode;
    }

    /**
     * Shows the invitation scene (sends it the title to retain it)
     */
    @FXML
    public void sendInvitesOnClick(){
        mainCtrl.showInvitation(titleLabel.getText());
    }

    /**
     * Sets the title of the event
     * @param title event's title
     */
    public void setTitle(String title){
        titleLabel.setText(title);
    }


    @FXML
    public void showAddExpense() {
        mainCtrl.showAddExpense(titleLabel.getText());
    }

    @FXML
    public void viewParticipantManager(){
        mainCtrl.showParticipantManager(titleLabel.getText());
    }


    @FXML
    public void showStatistics(){
        mainCtrl.showStatistics(titleLabel.getText(), this.eventCode);
    }

    /**
     * go back to Start screen
     */
    @FXML
    public void back() {
        if (admin) {
            mainCtrl.showAdminOverview();
        } else {
            mainCtrl.showStartScreen();
        }
    }
    @FXML
    private void viewDebts(){
        mainCtrl.viewDeptsPerEvent();
    }

    public void addExpense(String description, Type type, Date date,
                           Double totalExpense, String payerEmail) throws NotFoundException{

        ExpenseDTO exp = new ExpenseDTO(eventCode, description, type, date, totalExpense, payerEmail);
        serverUtils.addExpense(exp);
        serverUtils.send("/app/addExpense", exp);
        serverUtils.generatePaymentsForEvent(eventCode);

    }

    @FXML
    public void editExpense(){
        Expense toEdit = ((ListView<Expense>) expensesTabPane.getSelectionModel().getSelectedItem().getContent()).getSelectionModel().getSelectedItems().getFirst();

        if(toEdit == null){
            throw new NoSuchElementException("No element selected");
        }
        mainCtrl.showEditExpense(toEdit);
    }
    @FXML
    public Expense deleteExpense() {
        Expense toDelete;
        try {
            toDelete = ((ListView<Expense>) expensesTabPane.getSelectionModel().getSelectedItem().getContent()).getSelectionModel().getSelectedItems().getFirst();
            if (toDelete == null) {
                throw new NoSuchElementException();
            }
        } catch (NoSuchElementException e) {
            expenseNotDeletedError.setVisible(false);
            noExpenseError.setVisible(true);
            PauseTransition visiblePause = new PauseTransition(Duration.seconds(3));
            visiblePause.setOnFinished(
                    event -> noExpenseError.setVisible(false)
            );
            visiblePause.play();
            return null;

        }
        try{
            serverUtils.deleteExpense(toDelete);
        }catch (RuntimeException e){
            noExpenseError.setVisible(false);
            expenseNotDeletedError.setVisible(true);
            PauseTransition visiblePause = new PauseTransition(Duration.seconds(3));
            visiblePause.setOnFinished(
                    event -> expenseNotDeletedError.setVisible(false)
            );
            visiblePause.play();
        }
        System.out.println("OK");
        fetchExpenses();
        serverUtils.generatePaymentsForEvent(eventCode);
        return toDelete;
    }



    /**
     * fetches all expenses of this event and shows them in the assigned box
     */
    public void fetchExpenses(){
        Callback<ListView<Expense>, ListCell<Expense>> cellFactory = new Callback<ListView<Expense>, ListCell<Expense>>() {
            @Override
            public ListCell<Expense> call(ListView<Expense> expenseListView) {
                return new ListCell<Expense>(){
                    @Override
                    protected void updateItem(Expense expense, boolean b) {
                        super.updateItem(expense, b);
                        if(expense == null || b) setGraphic(null);
                        else {
                            GridPane grid = new GridPane();
                            Date date = expense.getDate();
                            Label dateLabel = new Label(
                                date.getDate() + "." +(date.getMonth() < 9 ? "0" : "") + (date.getMonth()+1) + "." + (date.getYear()+1900));
                            dateLabel.setStyle("-fx-font-size: 12px");
                            dateLabel.setPrefWidth(70);
                            Text mainInfo = new Text(expense.getPayer().getName() + " paid " +
                                expense.getTotalExpense() + " for " + expense.getType());
                            List<String> involved =
                                serverUtils.getDebtByExpense(expense.getEvent().getId(),
                                        expense.getExpenseId()).stream()
                                    .map(x -> x.getParticipant().getName()).toList();
                            grid.add(dateLabel, 0, 0);
                            grid.add(mainInfo, 1, 0);
                            grid.add(new Text(involved.toString()), 1, 1);
                            setGraphic(grid);
                        }
                    }
                };
            }
        };
        List<Expense> expenses = new ArrayList<>();
        try{
            expenses = serverUtils.getExpense(eventCode);
        }catch (BadRequestException e){
            System.out.println(e);
        }
        allExpensesList = new ListView<>();
        allExpensesList.getItems().addAll(expenses);
        allExpensesList.setCellFactory(cellFactory);
        allExpenses.setContent(allExpensesList);
        paidByMeList = new ListView<>();
        paidByMeList.getItems().addAll(expenses.stream().filter(x -> x.getPayer().getUuid().equals(config.getId())).toList());
        paidByMeList.setCellFactory(cellFactory);
        paidByMe.setContent(paidByMeList);
        includingMeList = new ListView<>();
        for(Expense e : expenses){
            List<String> owing = serverUtils.getDebtByExpense(e.getEvent().getId(), e.getExpenseId()).stream().filter(x -> x.getBalance() < 0).map(x -> x.getParticipant().getUuid()).toList();
            if(owing.contains(config.getId())) includingMeList.getItems().add(e);
        }
        includingMeList.setCellFactory(cellFactory);
        involvingMe.setContent(includingMeList);
    }

    public void fetchParticipants(){

        List<Participant> participants = new ArrayList<>();
        try{
            participants = serverUtils.getParticipants(eventCode);
        }catch (BadRequestException | NotFoundException e){
            System.out.println(e);
        }
        participantListView.setItems(FXCollections.observableArrayList(participants));
    }




    public void setExpensesText(String text) {
        this.expenses.setText(text);
    }

    public void setParticipants(String text) {
        this.participants.setText(text);
    }

    public void setBackButton(String text) {
        this.backButton.setText(text);
    }

    public void setSettleDebtsButton(String text) {
        this.settleDebtsButton.setText(text);
    }

    public void setStatisticsButton(String text) {
        this.statisticsButton.setText(text);
    }

    public void setAddExpenseButton(String text) {
        this.addExpenseButton.setText(text);
    }


    public void setPaidByMe(String text) {
        this.paidByMe.setText(text);
    }

    public void setDeleteExpenseButton(String text) {
        this.deleteExpenseButton.setText(text);
    }

    public void setSendInvites(String text) {
        this.sendInvites.setText(text);
    }

    public void setAllExpenses(String text) {
        this.allExpenses.setText(text);
    }

    public void leaveEvent(ActionEvent actionEvent) {
        serverUtils.deleteParticipant(mainCtrl.getMyUuid(), eventCode);
        mainCtrl.showStartScreen();
        confirmationLabel.setVisible(false);
        cancelLeaveButton.setVisible(false);
        leaveConfirmationButton.setVisible(false);
    }


    public void editEvent() {
        mainCtrl.editEvent();
    }
    @FXML
    public void onKeyPressed(KeyEvent press) {
        if (press.getCode() == KeyCode.ESCAPE) {
            back();
        }
        KeyCodeCombination k = new KeyCodeCombination(KeyCode.N,
                KeyCombination.CONTROL_DOWN, KeyCodeCombination.SHIFT_DOWN);
        if (k.match(press)) {
            showAddExpense();
        }

    }
    public void setAdmin(Boolean admin) {
        this.admin = admin;
    }
    public void setEventCreatedLabel() {
        eventCreatedLabel.setVisible(true);
        PauseTransition visiblePause = new PauseTransition(Duration.seconds(3));
        visiblePause.setOnFinished(
                event1 -> eventCreatedLabel.setVisible(false)
        );
        visiblePause.play();
    }
    public void setJoinedEventLabel() {
        joinedEventLabel.setVisible(true);
        PauseTransition visiblePause = new PauseTransition(Duration.seconds(3));
        visiblePause.setOnFinished(
                event1 -> joinedEventLabel.setVisible(false)
        );
        visiblePause.play();
    }

    public void setConfirmation(ActionEvent actionEvent) {
        confirmationLabel.setVisible(true);
        cancelLeaveButton.setVisible(true);
        leaveConfirmationButton.setVisible(true);
    }

    public void cancelLeave(ActionEvent actionEvent) {
        confirmationLabel.setVisible(false);
        cancelLeaveButton.setVisible(false);
        leaveConfirmationButton.setVisible(false);
    }
}

