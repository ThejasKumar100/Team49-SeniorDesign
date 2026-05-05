package controller;

import dao.BookDAO;
import dao.GeneratedSentenceDAO;
import dao.WordDAO;
import dao.WordFollowerDAO;
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
    private final WordFollowerDAO wordFollowerDAO = new WordFollowerDAO();

    private int lastCompletedWordId = -1;  // Track the last word's ID for word follower relationships
    private String lastProcessedWordText = null;  // Guard against processing same word twice
    private boolean lastProcessedWasSentenceEnd = false;  // Track if last processing was sentence-end
    private String lastProcessedTextContent = null;  // Track text content to prevent reprocessing with only whitespace changes

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

            // Guard: if only whitespace was added after the last period, skip extraction
            // This prevents re-extracting the same sentence-ending word when user types space/tab
            String trimmedText = text.replaceAll("\\s+$", "");  // Remove trailing whitespace
            if (trimmedText.equals(lastProcessedTextContent)) {
                System.out.println("DEBUG: Text content unchanged (only whitespace added), skipping extraction");
                return;
            }

            char lastChar = text.charAt(text.length() - 1);
            boolean endsWithPeriod = text.endsWith(".");

            // Extract and process word if space, comma, or preceded by period
            if (lastChar == ' ' || lastChar == ',' || endsWithPeriod) {
                String completedWord;
                boolean isSentenceEnd = false;
                
                if (endsWithPeriod) {
                    // Extract word before period
                    String withoutPeriod = text.substring(0, text.length() - 1);
                    completedWord = getLastCompletedWord(withoutPeriod);
                    isSentenceEnd = true;
                } else {
                    // Extract word normally (after space or comma)
                    completedWord = getLastCompletedWord(text);
                }

                System.out.println("DEBUG: Completed word extracted: '" + completedWord + "', isSentenceEnd: " + isSentenceEnd);

                if (completedWord.isEmpty()) {
                    System.out.println("DEBUG: Completed word is empty, returning");
                    return;
                }

                // Guard against processing the same word twice with same flags (happens on multiple keystroke events)
                if (completedWord.equals(lastProcessedWordText) && isSentenceEnd == lastProcessedWasSentenceEnd) {
                    System.out.println("DEBUG: Word '" + completedWord + "' already processed with same flags, skipping duplicate");
                    return;
                }

                // Determine if this word starts a sentence (before we potentially reset lastCompletedWordId)
                boolean startssentence = lastCompletedWordId == -1;
                System.out.println("DEBUG: startssentence = " + startssentence + ", isSentenceEnd = " + isSentenceEnd);

                // Check if word exists in database
                boolean wordExists = wordDAO.findByText(completedWord).isPresent();
                System.out.println("DEBUG: Word '" + completedWord + "' exists in database: " + wordExists);
                
                if (!wordExists) {
                    System.out.println("DEBUG: Word not found, calling promptToAddUnknownWord");
                    // Pass information about whether this word starts or ends a sentence
                    promptToAddUnknownWord(completedWord, startssentence, isSentenceEnd);
                } else {
                    // Word already exists
                    java.util.Optional<Word> currentWordOpt = wordDAO.findByText(completedWord);
                    if (currentWordOpt.isPresent()) {
                        Word currentWord = currentWordOpt.get();
                        int currentWordId = currentWord.getWordId();
                        
                        try {
                            // Use updateWordCounts to properly increment counts and set flags
                            wordDAO.updateWordCounts(currentWordId, startssentence, isSentenceEnd);
                            if (startssentence) {
                                System.out.println("DEBUG: Updated existing word '" + completedWord + "' as sentence-starting");
                            }
                            if (isSentenceEnd) {
                                System.out.println("DEBUG: Updated existing word '" + completedWord + "' as sentence-ending");
                            }
                        } catch (Exception e) {
                            System.err.println("DEBUG: Exception updating word counts: " + e);
                        }
                        
                        // Create follower relationship if we have a previous word
                        if (lastCompletedWordId != -1) {
                            System.out.println("DEBUG: Creating word follower relationship: " + lastCompletedWordId + " -> " + currentWordId);
                            wordFollowerDAO.insertOrUpdateFollower(lastCompletedWordId, currentWordId);
                        }
                        
                        lastCompletedWordId = currentWordId;
                        System.out.println("DEBUG: Updated lastCompletedWordId to: " + lastCompletedWordId);
                    }
                }

                List<String> suggestions = autocompleteService.suggestNextWords(completedWord);
                displaySuggestions(suggestions);

                lastCompletedWordLabel.setText("Last completed word: " + completedWord);

                if (suggestions.isEmpty()) {
                    statusLabel.setText("No next-word suggestions found for: " + completedWord);
                } else {
                    statusLabel.setText("Suggestions loaded for: " + completedWord);
                }

                // Track this word to prevent duplicate processing
                lastProcessedWordText = completedWord;
                lastProcessedWasSentenceEnd = isSentenceEnd;
                lastProcessedTextContent = text.replaceAll("\\s+$", "");  // Save text without trailing whitespace
                System.out.println("DEBUG: Tracked word '" + completedWord + "' as last processed (isSentenceEnd: " + isSentenceEnd + ")");
                System.out.println("DEBUG: Saved text content: '" + lastProcessedTextContent + "'");

                // If sentence ended, reset for next sentence
                if (isSentenceEnd) {
                    lastCompletedWordId = -1;
                    lastProcessedWordText = null;  // Allow new sentence to start with any word
                    lastProcessedWasSentenceEnd = false;
                    System.out.println("DEBUG: Sentence ended, reset lastCompletedWordId for next sentence");
                }

                loadWordsAlphabetically();
            }

        } catch (Exception e) {
            System.err.println("DEBUG: Exception in handleAutocompleteTyping: " + e);
            e.printStackTrace();
            showError("Autocomplete failed: " + e.getMessage());
        }
    }

    private void promptToAddUnknownWord(String word, boolean startssentence, boolean isSentenceEnd) {
        System.out.println("DEBUG: Prompting to add unknown word: " + word + ", startssentence: " + startssentence + ", isSentenceEnd: " + isSentenceEnd);
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unknown Word");
        alert.setHeaderText("Word not found in database");
        alert.setContentText("\"" + word + "\" is not in the word database.\n\nWould you like to add it?");

        ButtonType yesButton = new ButtonType("Add Word", ButtonBar.ButtonData.YES);
        ButtonType noButton = new ButtonType("Skip", ButtonBar.ButtonData.NO);

        alert.getButtonTypes().setAll(yesButton, noButton);
        
        System.out.println("DEBUG: Dialog created, about to show");

        alert.showAndWait().ifPresent(result -> {
            System.out.println("DEBUG: Dialog result received: " + result);
            if (result == yesButton) {
                try {
                    System.out.println("DEBUG: User clicked Add Word");
                    System.out.println("DEBUG: Using passed flags - startssentence: " + startssentence + ", ends sentence: " + isSentenceEnd);
                    
                    int wordId = wordDAO.insertWord(word, startssentence, isSentenceEnd);
                    
                    // If there was a previous word, create the follower relationship
                    if (lastCompletedWordId != -1) {
                        System.out.println("DEBUG: Creating word follower relationship: " + lastCompletedWordId + " -> " + wordId);
                        wordFollowerDAO.insertOrUpdateFollower(lastCompletedWordId, wordId);
                    }
                    
                    lastCompletedWordId = wordId;
                    System.out.println("DEBUG: Word added with ID: " + wordId);
                    statusLabel.setText("Word added: " + word);
                    loadWordsAlphabetically();
                } catch (Exception e) {
                    System.err.println("DEBUG: Exception while adding word: " + e);
                    showError("Failed to add word: " + e.getMessage());
                }
            } else {
                System.out.println("DEBUG: User clicked Skip");
                statusLabel.setText("Word skipped: " + word);
            }
        });
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
            // No need to add here - clicked suggestions are already in the database
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
        lastCompletedWordId = -1;  // Reset word ID tracker
        lastProcessedWordText = null;  // Reset tracking for new sentence
        lastProcessedWasSentenceEnd = false;
        lastProcessedTextContent = null;  // Reset text content tracking
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