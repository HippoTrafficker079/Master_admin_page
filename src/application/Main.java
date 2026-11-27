package application;

import java.io.InputStream;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
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

            primaryStage.setTitle("YTU – Student Information Management System");
            loadAppIcon(primaryStage);

            primaryStage.setScene(scene);
            primaryStage.show();

            primaryStage.setMinWidth(primaryStage.getWidth());
            primaryStage.setMinHeight(primaryStage.getHeight());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadAppIcon(Stage stage) {
        String[] candidatePaths = {
                "YTU_app_icon.png", "/YTU_app_icon.png",
                "YTU_logos.png", "/YTU_logos.png",
                "YTU_logos.jpg", "/YTU_logos.jpg",
                "YTU_logo.png", "/YTU_logo.png"
        };
        for (String path : candidatePaths) {
            try (InputStream is = getClass().getResourceAsStream(path)) {
                if (is != null) {
                    Image icon = new Image(is);
                    if (!icon.isError()) {
                        stage.getIcons().add(icon);
                        return;
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}