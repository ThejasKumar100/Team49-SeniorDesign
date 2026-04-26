package controller;

import dao.BookDAO;
import dao.GeneratedSentenceDAO;
import dao.WordDAO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import model.Book;
import model.GeneratedSentence;
import model.Word;
import service.AutocompleteService;
import service.SentenceGeneratorService;
import service.TextImportService;

import java.io.File;
import java.util.List;

public class MainController {

    @FXML private TextField filePathField;
    @FXML private ProgressIndicator importProgress;
    @FXML private Label statusLabel;

    @FXML private TextField startWordField;
    @FXML private TextField maxWordsField;
    @FXML private TextArea generatedSentenceArea;

    @FXML private TextArea autocompleteTypingArea;
    @FXML private Label lastCompletedWordLabel;
    @FXML private HBox suggestionBox;

    @FXML private TableView<Word> wordsTable;
    @FXML private TableColumn<Word, Integer> wordIdColumn;
    @FXML private TableColumn<Word, String> wordTextColumn;
    @FXML private TableColumn<Word, Integer> totalOccurrencesColumn;
    @FXML private TableColumn<Word, Integer> startCountColumn;
    @FXML private TableColumn<Word, Integer> endCountColumn;
    @FXML private TableColumn<Word, Boolean> canStartColumn;
    @FXML private TableColumn<Word, Boolean> canEndColumn;

    @FXML private TextField editWordIdField;
    @FXML private TextField editTotalOccurrencesField;
    @FXML private TextField editStartCountField;
    @FXML private TextField editEndCountField;
    @FXML private CheckBox editCanStartCheckBox;
    @FXML private CheckBox editCanEndCheckBox;

    @FXML private ListView<String> booksList;
    @FXML private ListView<String> generatedSentencesList;
    @FXML private ListView<String> duplicatesList;

    private final TextImportService importService = new TextImportService();
    private final SentenceGeneratorService generatorService = new SentenceGeneratorService();
    private final AutocompleteService autocompleteService = new AutocompleteService();
    private final WordDAO wordDAO = new WordDAO();
    private final BookDAO bookDAO = new BookDAO();
    private final GeneratedSentenceDAO generatedSentenceDAO = new GeneratedSentenceDAO();

    @FXML
    public void initialize() {
        setupWordsTable();
        importProgress.setVisible(false);
        maxWordsField.setText("15");
        lastCompletedWordLabel.setText("Last completed word: none");
        loadAllReports();
    }

    private void setupWordsTable() {
        wordIdColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(data.getValue().getWordId()).asObject());

        wordTextColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getWordText()));

        totalOccurrencesColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(data.getValue().getTotalOccurrences()).asObject());

        startCountColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(data.getValue().getStartCount()).asObject());

        endCountColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(data.getValue().getEndCount()).asObject());

        canStartColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleBooleanProperty(data.getValue().isCanStart()).asObject());

        canEndColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleBooleanProperty(data.getValue().isCanEnd()).asObject());

        wordsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selectedWord) -> {
            if (selectedWord != null) {
                populateEditFields(selectedWord);
            }
        });
    }

    @FXML
    private void handleBrowseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Text File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );

        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            filePathField.setText(selectedFile.getAbsolutePath());
        }
    }

    @FXML
    private void handleImportFile() {
        String filePath = filePathField.getText().trim();

        if (filePath.isEmpty()) {
            showError("Please choose or enter a text file path.");
            return;
        }

        importProgress.setVisible(true);
        statusLabel.setText("Importing file. Please wait...");

        Thread importThread = new Thread(() -> {
            try {
                importService.importTextFile(filePath);

                Platform.runLater(() -> {
                    importProgress.setVisible(false);
                    statusLabel.setText("Import complete.");
                    loadAllReports();
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    importProgress.setVisible(false);
                    statusLabel.setText("Import failed.");
                    showError("Import failed: " + e.getMessage());
                });
            }
        });

        importThread.setDaemon(true);
        importThread.start();
    }

    @FXML
    private void handleGenerateMostFrequent() {
        try {
            String startWord = startWordField.getText().trim();

            if (startWord.isEmpty()) {
                showError("Please enter a starting word.");
                return;
            }

            int maxWords = parseMaxWords();
            String sentence = generatorService.generateSentenceMostFrequent(startWord, maxWords);

            generatedSentenceArea.setText(sentence);
            loadGeneratedSentences();
            loadDuplicateSentences();
            statusLabel.setText("Generated sentence using most frequent algorithm.");

        } catch (Exception e) {
            showError("Generation failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleGenerateRandomTop3() {
        try {
            String startWord = startWordField.getText().trim();

            if (startWord.isEmpty()) {
                showError("Please enter a starting word.");
                return;
            }

            int maxWords = parseMaxWords();
            String sentence = generatorService.generateSentenceRandomTop3(startWord, maxWords);

            generatedSentenceArea.setText(sentence);
            loadGeneratedSentences();
            loadDuplicateSentences();
            statusLabel.setText("Generated sentence using random top 3 algorithm.");

        } catch (Exception e) {
            showError("Generation failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleAutocompleteTyping(KeyEvent event) {
        try {
            String text = autocompleteTypingArea.getText();

            if (text == null || text.isEmpty()) {
                suggestionBox.getChildren().clear();
                lastCompletedWordLabel.setText("Last completed word: none");
                return;
            }

            char lastChar = text.charAt(text.length() - 1);

            if (lastChar == ' ' || lastChar == ',') {
                String completedWord = getLastCompletedWord(text);

                if (completedWord.isEmpty()) {
                    return;
                }

                autocompleteService.addUnknownWordIfMissing(completedWord);

                List<String> suggestions = autocompleteService.suggestNextWords(completedWord);
                displaySuggestions(suggestions);

                lastCompletedWordLabel.setText("Last completed word: " + completedWord);

                if (suggestions.isEmpty()) {
                    statusLabel.setText("No next-word suggestions found for: " + completedWord);
                } else {
                    statusLabel.setText("Suggestions loaded for: " + completedWord);
                }

                loadWordsAlphabetically();
            }

        } catch (Exception e) {
            showError("Autocomplete failed: " + e.getMessage());
        }
    }

    private void displaySuggestions(List<String> suggestions) {
        suggestionBox.getChildren().clear();

        for (String word : suggestions) {
            Button pill = new Button(word);

            pill.setStyle(
                    "-fx-background-color: #facc15;" +
                    "-fx-text-fill: #0f1028;" +
                    "-fx-background-radius: 20;" +
                    "-fx-padding: 7 16;" +
                    "-fx-font-weight: bold;" +
                    "-fx-cursor: hand;"
            );

            pill.setOnAction(e -> handleSuggestionClick(word));
            suggestionBox.getChildren().add(pill);
        }
    }

    private void handleSuggestionClick(String selectedWord) {
        String currentText = autocompleteTypingArea.getText();

        if (currentText == null) {
            currentText = "";
        }

        if (!currentText.endsWith(" ") && !currentText.endsWith(",")) {
            currentText += " ";
        }

        autocompleteTypingArea.setText(currentText + selectedWord + " ");
        autocompleteTypingArea.positionCaret(autocompleteTypingArea.getText().length());

        try {
            autocompleteService.addUnknownWordIfMissing(selectedWord);

            List<String> nextSuggestions = autocompleteService.suggestNextWords(selectedWord);
            displaySuggestions(nextSuggestions);

            lastCompletedWordLabel.setText("Last completed word: " + selectedWord);
            statusLabel.setText("Inserted suggestion: " + selectedWord);

            loadWordsAlphabetically();

        } catch (Exception e) {
            showError("Could not load next suggestions: " + e.getMessage());
        }
    }

    @FXML
    private void handleClearAutocomplete() {
        autocompleteTypingArea.clear();
        suggestionBox.getChildren().clear();
        lastCompletedWordLabel.setText("Last completed word: none");
        statusLabel.setText("Autocomplete cleared.");
    }

    @FXML
    private void handleLoadWordsAlphabetically() {
        loadWordsAlphabetically();
    }

    @FXML
    private void handleLoadWordsByFrequency() {
        try {
            List<Word> words = wordDAO.getAllWordsByFrequency();
            wordsTable.setItems(FXCollections.observableArrayList(words));
            statusLabel.setText("Words sorted by frequency.");
        } catch (Exception e) {
            showError("Could not load words: " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdateWord() {
        try {
            int wordId = Integer.parseInt(editWordIdField.getText().trim());
            int totalOccurrences = Integer.parseInt(editTotalOccurrencesField.getText().trim());
            int startCount = Integer.parseInt(editStartCountField.getText().trim());
            int endCount = Integer.parseInt(editEndCountField.getText().trim());
            boolean canStart = editCanStartCheckBox.isSelected();
            boolean canEnd = editCanEndCheckBox.isSelected();

            wordDAO.updateWordMetadata(wordId, totalOccurrences, startCount, endCount, canStart, canEnd);

            statusLabel.setText("Word updated.");
            loadWordsAlphabetically();

        } catch (Exception e) {
            showError("Could not update word: " + e.getMessage());
        }
    }

    @FXML
    private void handleRefreshReports() {
        loadAllReports();
    }

    @FXML
    private void handleClearOutput() {
        generatedSentenceArea.clear();
        statusLabel.setText("Generated output cleared.");
    }

    private void loadAllReports() {
        loadWordsAlphabetically();
        loadBooks();
        loadGeneratedSentences();
        loadDuplicateSentences();
    }

    private void loadWordsAlphabetically() {
        try {
            List<Word> words = wordDAO.getAllWordsAlphabetical();
            wordsTable.setItems(FXCollections.observableArrayList(words));
            statusLabel.setText("Words loaded alphabetically.");
        } catch (Exception e) {
            showError("Could not load words: " + e.getMessage());
        }
    }

    private void loadBooks() {
        try {
            List<Book> books = bookDAO.getAllBooks();
            booksList.getItems().clear();

            for (Book book : books) {
                booksList.getItems().add(
                        book.getFileName()
                                + " | words: " + book.getWordCount()
                                + " | imported: " + book.getImportedAt()
                );
            }

        } catch (Exception e) {
            showError("Could not load books: " + e.getMessage());
        }
    }

    private void loadGeneratedSentences() {
        try {
            List<GeneratedSentence> sentences = generatedSentenceDAO.getAllGeneratedSentences();
            generatedSentencesList.getItems().clear();

            for (GeneratedSentence sentence : sentences) {
                generatedSentencesList.getItems().add(
                        sentence.getGeneratedAt() + " | " + sentence.getSentenceText()
                );
            }

        } catch (Exception e) {
            showError("Could not load generated sentences: " + e.getMessage());
        }
    }

    private void loadDuplicateSentences() {
        try {
            List<GeneratedSentence> duplicates = generatedSentenceDAO.getDuplicateSentences();
            duplicatesList.getItems().clear();

            for (GeneratedSentence sentence : duplicates) {
                duplicatesList.getItems().add(
                        sentence.getGeneratedAt() + " | " + sentence.getSentenceText()
                );
            }

        } catch (Exception e) {
            showError("Could not load duplicates: " + e.getMessage());
        }
    }

    private void populateEditFields(Word word) {
        editWordIdField.setText(String.valueOf(word.getWordId()));
        editTotalOccurrencesField.setText(String.valueOf(word.getTotalOccurrences()));
        editStartCountField.setText(String.valueOf(word.getStartCount()));
        editEndCountField.setText(String.valueOf(word.getEndCount()));
        editCanStartCheckBox.setSelected(word.isCanStart());
        editCanEndCheckBox.setSelected(word.isCanEnd());
    }

    private int parseMaxWords() {
        String value = maxWordsField.getText().trim();

        if (value.isEmpty()) {
            return 15;
        }

        int maxWords = Integer.parseInt(value);

        if (maxWords <= 0) {
            throw new IllegalArgumentException("Max words must be greater than 0.");
        }

        return maxWords;
    }

    private String getLastCompletedWord(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        String cleaned = text.trim();

        if (cleaned.endsWith(",")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }

        String[] parts = cleaned.split("\\s+");

        if (parts.length == 0) {
            return "";
        }

        String word = parts[parts.length - 1];
        word = word.replaceAll("^[^a-zA-Z']+|[^a-zA-Z']+$", "");
        return word.toLowerCase();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Sentence Builder");
        alert.setHeaderText("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
}