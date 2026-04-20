// imports
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import service.AutocompleteService;

import java.sql.SQLException;
import java.util.List;

// text editor UI
public class TextEditorUI extends Application {
    private TextArea textArea;
    private HBox suggestionsBox;
    private Button[] suggestionButtons;
    private AutocompleteService autocompleteService;
    private static final int NUM_SUGGESTIONS = 4;

    @Override
    public void start(Stage primaryStage) {
        autocompleteService = new AutocompleteService();

        // layout
        BorderPane root = new BorderPane();
        root.setStyle("-fx-padding: 0;");

        // text area
        textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setStyle("-fx-font-size: 14; -fx-padding: 20;");
        textArea.setPromptText("Start typing here...");
        textArea.setOnKeyReleased(this::handleTextInput);

        // vbox with left/right margins (word doc style)
        VBox centerContent = new VBox(textArea);
        centerContent.setStyle("-fx-padding: 20 60 20 60;");
        VBox.setVgrow(textArea, Priority.ALWAYS);
        root.setCenter(centerContent);

        // suggestions bar
        VBox bottomSection = createSuggestionsSection();
        root.setBottom(bottomSection);

        // scene
        Scene scene = new Scene(root, 900, 700);
        primaryStage.setTitle("Sentence Builder - Text Editor");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createSuggestionsSection() {
        VBox section = new VBox();
        section.setStyle("-fx-background-color: #3a3a3a; -fx-padding: 20;");
        section.setSpacing(12);

        // label
        Label suggestionsLabel = new Label("Autocomplete Suggestions:");
        suggestionsLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: #ffffff;");

        // suggestions box
        suggestionsBox = new HBox();
        suggestionsBox.setSpacing(15);
        suggestionsBox.setPrefHeight(50);
        suggestionsBox.setAlignment(Pos.CENTER);
        suggestionsBox.setStyle("-fx-padding: 10;");

        // suggestions buttons
        suggestionButtons = new Button[NUM_SUGGESTIONS];
        for (int i = 0; i < NUM_SUGGESTIONS; i++) {
            Button suggestionButton = createSuggestionButton("");
            suggestionButtons[i] = suggestionButton;
            HBox.setHgrow(suggestionButton, Priority.ALWAYS);
            suggestionsBox.getChildren().add(suggestionButton);
        }
        
        // add label and suggestions box to section
        section.getChildren().addAll(suggestionsLabel, suggestionsBox);
        return section;
    }

    // create styled suggestion button
    private Button createSuggestionButton(String text) {
        Button button = new Button(text);
        button.setStyle(
                "-fx-background-color: #d3d3d3; " +
                "-fx-text-fill: #333333; " +
                "-fx-font-size: 12; " +
                "-fx-padding: 10 15; " +
                "-fx-border-radius: 5; " +
                "-fx-cursor: hand;"
        );
        button.setMaxWidth(Double.MAX_VALUE);
        button.setWrapText(true);

        // hover effect
        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: #b0b0b0; " +
                "-fx-text-fill: #000000; " +
                "-fx-font-size: 12; " +
                "-fx-padding: 10 15; " +
                "-fx-border-radius: 5; " +
                "-fx-cursor: hand;"
        ));

        // exit hover effect
        button.setOnMouseExited(e -> button.setStyle(
                "-fx-background-color: #d3d3d3; " +
                "-fx-text-fill: #333333; " +
                "-fx-font-size: 12; " +
                "-fx-padding: 10 15; " +
                "-fx-border-radius: 5; " +
                "-fx-cursor: hand;"
        ));

        // click action
        button.setOnAction(e -> insertSuggestion(button.getText()));

        return button;
    }

    // handle text input and update suggestions
    private void handleTextInput(KeyEvent event) {
        try {
            String text = textArea.getText();
            String lastWord = extractLastWord(text);

            // if no last word, clear suggestions
            if (lastWord.isEmpty()) {
                clearSuggestions();
            } else {
                updateSuggestions(lastWord);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching suggestions: " + e.getMessage());
        }
    }

    // extract last word from text for suggestions
    private String extractLastWord(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // remove trailing whitespace and get last word
        text = text.trim();
        int lastSpaceIndex = text.lastIndexOf(' ');
        if (lastSpaceIndex == -1) {
            return text.toLowerCase();
        }

        // return last word in lowercase for consistent suggestions
        return text.substring(lastSpaceIndex + 1).toLowerCase();
    }

    // update suggestion buttons based on autocomplete results
    private void updateSuggestions(String completedWord) throws SQLException {
        try {
            List<String> suggestions = autocompleteService.suggestNextWords(completedWord);

            // update buttons with suggestions
            for (int i = 0; i < NUM_SUGGESTIONS; i++) {
                if (i < suggestions.size()) {
                    suggestionButtons[i].setText(suggestions.get(i));
                    suggestionButtons[i].setDisable(false);
                    suggestionButtons[i].setOpacity(1.0);
                } else {
                    suggestionButtons[i].setText("");
                    suggestionButtons[i].setDisable(true);
                    suggestionButtons[i].setOpacity(0.4);
                }
            }
        } catch (SQLException e) {
            clearSuggestions();
        }
    }

    // clear suggestion buttons when no valid input
    private void clearSuggestions() {
        for (Button button : suggestionButtons) {
            button.setText("");
            button.setDisable(true);
            button.setOpacity(0.4);
        }
    }

    // insert selected suggestion into text area, replacing last word
    private void insertSuggestion(String word) {
        if (word == null || word.isEmpty()) {
            return;
        }

        // get current text and caret position
        String currentText = textArea.getText();
        int caretPosition = textArea.getCaretPosition();

        // find start of last word based on caret position
        int lastSpaceIndex = currentText.lastIndexOf(' ', caretPosition - 1);
        int startOfLastWord = lastSpaceIndex + 1;

        // replace last word with the selected suggestion
        String beforeLastWord = currentText.substring(0, startOfLastWord);
        String afterCaret = currentText.substring(caretPosition);

        // construct new text with the suggestion inserted
        String newText = beforeLastWord + word + " " + afterCaret;
        textArea.setText(newText);

        // move caret
        textArea.positionCaret(beforeLastWord.length() + word.length() + 1);

        // update suggestions for newly inserted word
        try {
            updateSuggestions(word);
        } catch (SQLException e) {
            System.err.println("Error updating suggestions: " + e.getMessage());
        }
    }

    // main method to launch application
    public static void main(String[] args) {
        launch(args);
    }
}
