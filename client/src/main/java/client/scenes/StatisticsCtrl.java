package client.scenes;


import client.utils.Config;
import client.utils.ServerUtils;
import commons.Currency;
import commons.Participant;
import javafx.animation.PauseTransition;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Callback;
import javafx.util.Duration;

import javax.inject.Inject;
import java.util.Date;

public class StatisticsCtrl {
    @FXML
    private PieChart pieChart;
    private double food = 0;
    private double drinks = 0;
    private double transport = 0;
    private double other = 0;
    private double[] stat;

    @FXML
    private Button back;

    @FXML
    private Label titleLabel;
    @FXML
    private Label statisticsText;
    @FXML
    private Label totalCost;
    @FXML
    private Label totalCostText;
    @FXML
    private Label errorLabel;
    @FXML
    private ListView<Participant> shareListView;
    @FXML
    private Label shareOfExpensesLabel;

    private double total;

    ObservableList<Participant> listViewData;

    private int eventId;

    private MainCtrl mainCtrl;
    private ServerUtils serverUtils;
    private Config config;

    @Inject
    public StatisticsCtrl(MainCtrl mainCtrl, ServerUtils serverUtils, Config config) {
        this.mainCtrl = mainCtrl;
        this.serverUtils = serverUtils;
        this.config = config;
        listViewData = FXCollections.observableArrayList();
    }

    private void setListViewUp() {
        shareListView.setVisible(!(total < 0.001));
        shareOfExpensesLabel.setVisible(!(total < 0.001));

        shareListView.setCellFactory(new Callback<ListView<Participant>, ListCell<Participant>>() {
            @Override
            public ListCell<Participant> call(ListView<Participant> participantListView) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(Participant participant, boolean b) {
                        super.updateItem(participant, b);
                        if (participant == null || b) {
                            setText(null);
                        } else {
                            double share = serverUtils.getExpenseByUuid(eventId,
                                    participant.getUuid())
                                .stream()
                                .mapToDouble(x -> x.getTotalExpense() * 100)
                                .sum() / total;
                            setText(participant.getName()
                                + ": "
                                + mainCtrl.getFormattedDoubleString(share)
                                + "%");
                        }
                    }
                };
            }
        });
    }

    /**
     * initialize the chart with the current values for food, drinks, transport and other
     */
    public void setPieChart() {
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        String foodName = mainCtrl.translate("Food");
        String drinkName = mainCtrl.translate("Drinks");
        String transportName = mainCtrl.translate("Transport");
        String otherName = mainCtrl.translate("Other");
        if(this.food != 0.0) data.add(new PieChart.Data(foodName, food));
        if(this.drinks != 0.0) data.add(new PieChart.Data(drinkName, drinks));
        if(this.transport != 0.0) data.add(new PieChart.Data(transportName, transport));
        if(this.other != 0.0) data.add(new PieChart.Data(otherName, other));

        data.forEach(d -> d.nameProperty().bind(Bindings.concat(
                    d.getName(),
                    ": ",
                    mainCtrl.getFormattedDoubleString(d.pieValueProperty().getValue()),
                    java.util.Currency.getInstance(config.getCurrency().toString()).getSymbol()
                )
            )

        );

        pieChart.setData(data);
    }

    //these all set the things for the diagram, theoretically they could all
    // be done in one method but I wasn't sure how we were going to implement this
    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    public void setEventCode(int eventCode) {
        this.eventId = eventCode;
    }

    public void fetchStat() {
        this.stat = serverUtils.getStatisticsByEventID(eventId);
        setFood();
        setDrinks();
        setTransport();
        setOther();
        setTotalCost(total);
    }

    public void setFood() {
        try {
            this.food = mainCtrl.getAmountInDifferentCurrency(Currency.EUR, config.getCurrency(),
                new Date(), stat[0]);
        } catch (RuntimeException e) {
            displayError();
        }
    }

    public void setDrinks() {
        try {
            this.drinks = mainCtrl.getAmountInDifferentCurrency(Currency.EUR, config.getCurrency(),
                new Date(), stat[1]);
        } catch (RuntimeException e) {
            displayError();
        }
    }

    public void setTransport() {
        try {
            this.transport = mainCtrl.getAmountInDifferentCurrency(Currency.EUR,
                config.getCurrency(), new Date(), stat[2]);
        } catch (RuntimeException e) {
            displayError();
        }
    }

    public void setOther() {
        try {
            this.other = mainCtrl.getAmountInDifferentCurrency(Currency.EUR,
                config.getCurrency(), new Date(), stat[3]);
        } catch (RuntimeException e) {
            displayError();
        }
    }

    public void setTotalCost(double totalCost) {
        try {
            this.totalCost.setText(mainCtrl.getFormattedDoubleString(totalCost)
                + java.util.Currency.getInstance(config.getCurrency().toString()).getSymbol());
        } catch (RuntimeException e) {
            this.totalCost.setText("-");
            displayError();
        }
    }

    @FXML
    public void goBack() {
        mainCtrl.showSplittyOverview(eventId);
    }

    @FXML
    public void onKeyPressed(KeyEvent press) {
        if (press.getCode() == KeyCode.ESCAPE) {
            goBack();
        }
    }

    private void displayError() {
        errorLabel.setVisible(true);
        PauseTransition visiblePause = new PauseTransition(Duration.seconds(3));
        visiblePause.setOnFinished(event1 -> errorLabel.setVisible(false));
        visiblePause.play();
    }

    public void refresh() {
        listViewData.clear();
        listViewData.addAll(serverUtils.getParticipants(eventId));
        total = serverUtils.getTotalCostEvent(eventId);
        try {
            total = mainCtrl.getAmountInDifferentCurrency(Currency.EUR, config.getCurrency(),
                new Date(), total);
            shareListView.setItems(listViewData);
            setListViewUp();
        } catch (RuntimeException e) {
            this.totalCost.setText("-");
            displayError();
        }
    }

    public void setStatisticsText(String txt) {
        this.statisticsText.setText(txt);
    }

    public void setTotalCostText(String txt) {
        this.totalCostText.setText(txt);
    }
    public void setBackButton(String txt){
        this.back.setText(txt);
    }
}
