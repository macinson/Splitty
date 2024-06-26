package client.scenes;

import client.utils.Config;
import client.utils.ServerUtils;
import commons.Event;
import commons.Participant;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.util.Duration;

import javax.inject.Inject;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;


public class StartScreenCtrl implements Initializable {
    private final ServerUtils serverUtils;
    private final MainCtrl mainCtrl;
    private final Config config;

    boolean translating = false;

    @FXML
    private Label myEventsText;
    @FXML
    private Label createEventText;
    @FXML
    private TextField createEventTextField;

    @FXML
    private Label joinEventText;
    @FXML
    private TextField joinEventTextField;
    @FXML
    public Label invalidCodeError;
    @FXML
    public Label codeNotFoundError;
    @FXML
    public Label alreadyParticipantError;

    @FXML
    private Button adminLogin;
    @FXML
    private Button showAllEventsButton;
    @FXML
    private Button join;
    @FXML
    private Button create;

    @FXML
    private ComboBox languageSelect;

    @FXML
    private ImageView imageView;

    @FXML
    private ListView<Event> eventListView;
    @FXML
    public Label myEventsNotFoundError;
    @FXML
    public Label noConnectionError;
    private List<Event> events;
    @FXML
    public Label settingsSavedLabel;
    @FXML
    public Button settingsButton;
    @FXML
    private ProgressIndicator progress;

    @FXML
    private ImageView flag;


    @Inject
    public StartScreenCtrl(ServerUtils serverUtils, MainCtrl mainCtrl, Config config) {
        this.serverUtils = serverUtils;
        this.mainCtrl = mainCtrl;
        this.config = config;
        events = new ArrayList<>();
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setImages();

        // Load the image
        Image image = new Image("Logo_.png"); // Path relative to your resources folder
        // Set the image to the ImageView
        imageView.setImage(image);
        //Image flag = new Image("enFlag.png");
        eventListView.setCellFactory(eventListView -> new ListCell<Event>() {
            @Override
            protected void updateItem(Event event, boolean empty) {
                super.updateItem(event, empty);
                if (empty || event == null) {
                    setText(null);
                } else {
                    setText(event.getName());
                }
            }
        });
    }

    private void setImages() {
        ImageView settings = new ImageView(new Image("Settings-icon.png"));
        settings.setFitWidth(15);
        settings.setFitHeight(15);
        settingsButton.setGraphic(settings);
        ImageView admin = new ImageView(new Image("adminIcon.png"));
        admin.setFitWidth(15);
        admin.setFitHeight(15);
        adminLogin.setGraphic(admin);
    }

    public void fetchList() {
        eventListView.getItems().clear();

        events = mainCtrl.getMyEvents();
        if (events != null) {
            ObservableList<Event> newEventList = FXCollections.observableArrayList();
            ObservableList<Event> currentEventList = FXCollections.observableArrayList(events);
            currentEventList.stream().sorted(Comparator.comparing(Event::getLastActivity))
                    .forEach(newEventList::add);
            newEventList = FXCollections.observableArrayList(newEventList.reversed());
            eventListView.setItems(newEventList);
        }
    }


    private void setup(Event event, Button button, Label label) {
        if (event == null) {
            button.setVisible(false);
            label.setVisible(false);
            return;
        }
        button.setVisible(true);
        label.setVisible(true);

        button.setOnAction(something -> {
            mainCtrl.showSplittyOverview(event.getId());
        });

        button.setText(event.getName());
        label.setText(event.getDate() + ": " + event.getDescription());
    }

    /**
     * Creates an event with the title specified in the createEventTextField
     * TO DO - actually create an event
     */
    public void createEvent() {
        String name = createEventTextField.getText();
        if (name == null || name.isEmpty()) {
            name = "New event";
        }
        mainCtrl.showCreateEvent(name);
        //TO DO: add event to database, fill in more information about the event.
        //This will happen in the CreateEventCtrl class!
    }


    /**
     * Join an event with the title specified in the joinEventTextField
     * TO DO - join an event by the event id/URL
     */
    public void joinEvent() {
        String eventInviteCode = joinEventTextField.getText();
        if (eventInviteCode == null || eventInviteCode.isEmpty()) {
            invalidCodeError.setVisible(true);
            alreadyParticipantError.setVisible(false);
            codeNotFoundError.setVisible(false);
            return;
        }
        // already a participant of this event?
        if (mainCtrl.getMyEvents().stream().anyMatch(e ->
                e.getInviteCode().equals(eventInviteCode))) {
            invalidCodeError.setVisible(false);
            codeNotFoundError.setVisible(false);
            alreadyParticipantError.setVisible(true);
            return;
        }
        if (config.getName() == null || config.getName().isEmpty()) {
            if (!setConfirmationJoin()) {
                return;
            }
        }
        try {
            Participant p = mainCtrl.joinEvent(eventInviteCode);
            if (p == null) {
                invalidCodeError.setVisible(false);
                codeNotFoundError.setVisible(true);
                alreadyParticipantError.setVisible(false);
                return;
            }
            mainCtrl.showSplittyOverview(p.getEvent().getId());
            System.out.println("Joined event: " + joinEventTextField.getText());
        } catch (NumberFormatException e) {
            codeNotFoundError.setVisible(false);
            invalidCodeError.setVisible(true);
        } catch (RuntimeException e) {
            invalidCodeError.setVisible(false);
            codeNotFoundError.setVisible(true);
        }
        mainCtrl.setConfirmationJoinedEvent();
    }

    public boolean setConfirmationJoin() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Joining Event");
        alert.setContentText("You are about to join an event without having a name, " +
                "you can set your name in the settings. If you wish to continue," +
                " your name will be set to 'Unknown'");
        AtomicBoolean ok = new AtomicBoolean(true);
        alert.showAndWait().ifPresent(action -> {
            if (action != ButtonType.OK) {
                ok.set(false);
            }
        });
        return ok.get();
    }


    @FXML
    public void showAllEvents() {
        mainCtrl.showUserEventList();
    }


    public void setMyEventsText(String txt) {
        Platform.runLater(() -> {
            myEventsText.setText(txt);
        });
    }

    public void showAdminLogin(ActionEvent actionEvent) {
        mainCtrl.showAdminLogin();
    }

    public void setCreateEventText(String text) {
        Platform.runLater(() -> {
            createEventText.setText(text);
        });
    }

    public void setJoinEventText(String text) {
        Platform.runLater(() -> {
            joinEventText.setText(text);
        });
    }

    public void setAdminLogin(String text) {
        Platform.runLater(() -> {
            adminLogin.setText(text);
        });
    }

    public void setShowAllEvents(String text) {
        Platform.runLater(() -> {
            showAllEventsButton.setText(text);
        });
    }

    public void setJoinButtonText(String text) {
        Platform.runLater(() -> {
            join.setText(text);
        });
    }

    public void setCreateButtonText(String text) {
        Platform.runLater(() -> {
            create.setText(text);
        });
    }

    public void setInvalidCodeError(String text) {
        Platform.runLater(() -> {
            invalidCodeError.setText(text);
        });
    }

    public void setCodeNotFoundError(String text) {
        Platform.runLater(() -> {
            codeNotFoundError.setText(text);
        });
    }

    public void setAlreadyParticipantError(String text) {
        Platform.runLater(() -> {
            alreadyParticipantError.setText(text);
        });
    }

    public void setNoConnectionError(String text) {
        Platform.runLater(() -> {
            noConnectionError.setText(text);
        });
    }

    public void setMyEventsNotFoundError(String text) {
        Platform.runLater(() -> {
            myEventsNotFoundError.setText(text);
        });
    }

    public void setmyEventsText(String text) {
        Platform.runLater(() -> {
            myEventsText.setText(text);
        });
    }


    public void setSettingsSavedLabel(String text) {
        Platform.runLater(() -> {
            settingsSavedLabel.setText(text);
        });
    }


    public void setNoEventLabel(String text) {
//        noEventLabel.setText(text);
    }

    public void setSettings(String text) {
        Platform.runLater(() -> {
            this.settingsButton.setText(text);
        });

    }

    public void setJoinEventTextField(String text) {
        Platform.runLater(() -> {
            this.joinEventTextField.setPromptText(text);
        });
    }

    public void setCreateEventTextField(String text) {
        Platform.runLater(() -> {
            this.createEventTextField.setPromptText(text);
        });
    }


    public void setLanguageSelect() {
        translating = true;
        //TODO add a check if this list is the same as the actual list otherwise
        // set it or find a way to initialize this once without the actual values because those are null before you init
        ObservableList<String> languages = FXCollections.observableArrayList();
        mainCtrl.language = config.getLanguage();
        if (mainCtrl.language == null) {
            mainCtrl.language = "en";
        }
        if (!mainCtrl.languages.contains(mainCtrl.language)) {
            mainCtrl.language = "en";
            config.setLanguage("en");
            config.write();
        }
        languages.addAll(mainCtrl.languages);
        languageSelect.setItems(languages);
        languageSelect.setValue(mainCtrl.language);
        //languageSelect.setValue(flag);
        Image flag = mainCtrl.getFlag();
        setFlag(flag);
//        if (!mainCtrl.language.equals(currentLang)) {
//            changeLanguage();
//        }
        translating = false;
    }

    private boolean starting = true;

    @FXML
    public void changeLanguage() {
        if (translating) return;
        try {
            if (languageSelect.getSelectionModel().getSelectedItem() != null) {
                String selected = (String) languageSelect.getSelectionModel().getSelectedItem();
                if (selected.equals(mainCtrl.language) && !starting) {
                    return;
                }
                starting = false;
                //Language toLang = Language.valueOf(selected);
                if (mainCtrl.languages.contains(selected)) {
                    progress.setVisible(true);
                    config.setLanguage(selected);
                    config.write();
                    String toLang = selected;
                    mainCtrl.changeLanguage(toLang);
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        languageSelect.setVisible(false);
    }

    @FXML
    public void showLangOptions() {
        languageSelect.show();
        imageView.getScene().addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (!languageSelect.getBoundsInParent().contains(event.getX(), event.getY())) {
                // Clicked outside the choice box, hide it
                languageSelect.setVisible(false);
            }
        });
        imageView.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            // Hide the choice box when any key is pressed
            languageSelect.setVisible(false);
        });
    }

    public void setProgress(boolean state) {
        this.progress.setVisible(state);
    }

    public void showSettings() {
        mainCtrl.showSettings(noConnectionError.visibleProperty().get());
    }


    public void handleMouseClick(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            Event event = (Event) eventListView.getSelectionModel().getSelectedItem();
            if (event != null) {
                mainCtrl.showSplittyOverview(event.getId());
            }
        }
    }

    @FXML
    public void onKeyPressed(KeyEvent press) {
        KeyCodeCombination k = new KeyCodeCombination(KeyCode.N,
                KeyCombination.CONTROL_DOWN, KeyCodeCombination.SHIFT_DOWN);
        if (k.match(press)) {
            createEvent();
        }
    }

    public void resetErrors() {
        codeNotFoundError.setVisible(false);
        invalidCodeError.setVisible(false);
        alreadyParticipantError.setVisible(false);
    }

    public void setFlag(Image image) {
        flag.setImage(image);
    }


    public void addEvent(Event event) {
        if (events == null) {
            events = new ArrayList<>();
        }
        events.add(event);
        ObservableList<Event> currentEventList = FXCollections.observableArrayList(events);
        eventListView.setItems(currentEventList);
    }

    public void setSettingsSavedLabel() {
        settingsSavedLabel.setVisible(true);
        PauseTransition visiblePause = new PauseTransition(Duration.seconds(5));
        visiblePause.setOnFinished(
                event1 -> settingsSavedLabel.setVisible(false)
        );
        visiblePause.play();
    }

    public void setNoEventsError(boolean b) {
        myEventsNotFoundError.setVisible(b);
        noConnectionError.setVisible(b);
    }

    public void handleKeyPress(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            Event event = eventListView.getSelectionModel().getSelectedItem();
            if (event != null) {
                mainCtrl.showSplittyOverview(event.getId());
            }
        }
    }


    public void processKeyPressJoinEvent(KeyEvent keyEvent) {
        resetErrors();
        if (keyEvent.getCode() == KeyCode.ENTER) {
            joinEvent();
        }
    }

    public void processKeyPressedCreateEvent(KeyEvent keyEvent) {

        if (keyEvent.getCode() == KeyCode.ENTER) {
            createEvent();
        }
    }
}
