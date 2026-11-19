package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

	@Override
	public void start(Stage primaryStage) {
		try {
			String css = this.getClass().getResource("style.css").toExternalForm();
			Parent root = FXMLLoader.load(getClass().getResource("MainLayout.fxml"));

			// Default window size on launch
			double defaultWidth = 1067;
			double defaultHeight = 700;

			Scene scene = new Scene(root, defaultWidth, defaultHeight);
			scene.getStylesheets().add(css);

			primaryStage.setScene(scene);
			primaryStage.show();

			
			primaryStage.setMinWidth(primaryStage.getWidth());
			primaryStage.setMinHeight(primaryStage.getHeight());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
}