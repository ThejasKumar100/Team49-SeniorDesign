import dao.BookDAO;
import dao.GeneratedSentenceDAO;
import dao.WordDAO;
import model.Book;
import model.GeneratedSentence;
import model.Word;
import service.AutocompleteService;
import service.SentenceGeneratorService;
import service.TextImportService;

import java.util.List;
import java.util.Scanner;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        TextImportService importService = new TextImportService();
        SentenceGeneratorService generatorService = new SentenceGeneratorService();
        AutocompleteService autocompleteService = new AutocompleteService();
        WordDAO wordDAO = new WordDAO();
        BookDAO bookDAO = new BookDAO();
        GeneratedSentenceDAO generatedSentenceDAO = new GeneratedSentenceDAO();

        while (true) {
            printMenu();
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1":
                        System.out.print("Enter full path to .txt file: ");
                        String path = scanner.nextLine().trim();
                        importService.importTextFile(path);
                        System.out.println("Import complete.");
                        break;

                    case "2":
                        System.out.print("Enter starting word: ");
                        String startWord1 = scanner.nextLine().trim();
                        System.out.print("Enter max words: ");
                        int maxWords1 = Integer.parseInt(scanner.nextLine().trim());
                        String sentence1 = generatorService.generateSentenceMostFrequent(startWord1, maxWords1);
                        System.out.println("Generated sentence:");
                        System.out.println(sentence1);
                        break;

                    case "3":
                        System.out.print("Enter starting word: ");
                        String startWord2 = scanner.nextLine().trim();
                        System.out.print("Enter max words: ");
                        int maxWords2 = Integer.parseInt(scanner.nextLine().trim());
                        String sentence2 = generatorService.generateSentenceRandomTop3(startWord2, maxWords2);
                        System.out.println("Generated sentence:");
                        System.out.println(sentence2);
                        break;

                    case "4":
                        System.out.print("Enter completed word: ");
                        String completedWord = scanner.nextLine().trim();
                        List<String> suggestions = autocompleteService.suggestNextWords(completedWord);
                        if (suggestions.isEmpty()) {
                            System.out.println("No suggestions found.");
                        } else {
                            System.out.println("Suggestions: " + suggestions);
                        }
                        break;

                    case "5":
                        List<Word> wordsAlpha = wordDAO.getAllWordsAlphabetical();
                        for (Word w : wordsAlpha) {
                            System.out.println(w);
                        }
                        break;

                    case "6":
                        List<Word> wordsFreq = wordDAO.getAllWordsByFrequency();
                        for (Word w : wordsFreq) {
                            System.out.println(w);
                        }
                        break;

                    case "7":
                        List<Book> books = bookDAO.getAllBooks();
                        for (Book b : books) {
                            System.out.println(b);
                        }
                        break;

                    case "8":
                        List<GeneratedSentence> sentences = generatedSentenceDAO.getAllGeneratedSentences();
                        for (GeneratedSentence gs : sentences) {
                            System.out.println(gs);
                        }
                        break;

                    case "9":
                        List<GeneratedSentence> duplicates = generatedSentenceDAO.getDuplicateSentences();
                        if (duplicates.isEmpty()) {
                            System.out.println("No duplicate generated sentences found.");
                        } else {
                            for (GeneratedSentence gs : duplicates) {
                                System.out.println(gs);
                            }
                        }
                        break;

                    case "10":
                        System.out.print("Enter a typed word to add if missing: ");
                        String typedWord = scanner.nextLine().trim();
                        autocompleteService.addUnknownWordIfMissing(typedWord);
                        System.out.println("Word processed.");
                        break;

                    case "11":
                        System.out.print("Enter max words: ");
                        int maxWords3 = Integer.parseInt(scanner.nextLine().trim());
                        String sentence3 = generatorService.generateSentenceRandomStartMostFrequent(maxWords3);
                        System.out.println("Generated sentence:");
                        System.out.println(sentence3);
                        break;

                    case "0":
                        System.out.println("Goodbye.");
                        return;

                    default:
                        System.out.println("Invalid choice.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                e.printStackTrace();
            }

            System.out.println();
        }
    }

    private static void printMenu() {
        System.out.println("======================================");
        System.out.println("Sentence Builder Menu");
        System.out.println("1. Import text file");
        System.out.println("2. Generate sentence (most frequent next word)");
        System.out.println("3. Generate sentence (random from top 3 next words)");
        System.out.println("4. Auto-complete suggestions");
        System.out.println("5. Show all words alphabetically");
        System.out.println("6. Show all words by frequency");
        System.out.println("7. Show imported books");
        System.out.println("8. Show all generated sentences");
        System.out.println("9. Show duplicate generated sentences");
        System.out.println("10. Add typed word if missing");
        System.out.println("11. Generate sentence (random sentence start)");
        System.out.println("0. Exit");
        System.out.print("Choose an option: ");
    }
}