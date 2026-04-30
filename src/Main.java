import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main entry point for the Sentence Builder JavaFX application.
 *
 * This class launches the application, loads the main FXML layout,
 * creates the scene, and displays the primary window.
 */
public class Main extends Application {

    /**
     * Called automatically when the JavaFX application starts.
     *
     * @param stage the main application window
     * @throws Exception if the FXML file cannot be loaded
     */
    @Override
    public void start(Stage stage) throws Exception {
        // Load the main UI layout from the FXML file located in resources/view
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main-view.fxml"));

        // Create the main scene with a fixed starting window size
        Scene scene = new Scene(loader.load(), 1100, 750);

        // Set the application window title
        stage.setTitle("Sentence Builder");

        // Attach the scene to the main window
        stage.setScene(scene);

        // Display the JavaFX window
        stage.show();
    }

    /**
     * Standard Java main method.
     * launch(args) starts the JavaFX application lifecycle.
     */
    public static void main(String[] args) {
        launch(args);
    }
}