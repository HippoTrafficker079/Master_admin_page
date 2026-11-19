package application;

import java.util.Optional;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Window;

public class AlertHelper {

    public static void showInfo(Window owner, String title, String message) {
        show(owner, Alert.AlertType.INFORMATION, title, null, message);
    }

    public static void showInfo(Window owner, String title, String header, String message) {
        show(owner, Alert.AlertType.INFORMATION, title, header, message);
    }

    public static void showWarning(Window owner, String title, String message) {
        show(owner, Alert.AlertType.WARNING, title, null, message);
    }

    public static void showError(Window owner, String title, String message) {
        show(owner, Alert.AlertType.ERROR, title, null, message);
    }

    public static Optional<ButtonType> showConfirm(Window owner, String title, String header, String message, boolean isDanger) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        if (owner != null) {
            alert.initOwner(owner);
        }
        styleAlert(alert, isDanger);
        return alert.showAndWait();
    }

    public static void show(Window owner, Alert.AlertType type, String title, String header, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        if (owner != null) {
            alert.initOwner(owner);
        }
        styleAlert(alert, type == Alert.AlertType.ERROR);
        alert.showAndWait();
    }

    /**
     * Applies the application's stylesheet, modern vector badge icon, and button hover/click styles
     * to any JavaFX Alert instance.
     */
    public static void styleAlert(Alert alert, boolean primaryIsDanger) {
        DialogPane pane = alert.getDialogPane();
        if (AlertHelper.class.getResource("style.css") != null) {
            String css = AlertHelper.class.getResource("style.css").toExternalForm();
            if (!pane.getStylesheets().contains(css)) {
                pane.getStylesheets().add(css);
            }
        }

        pane.getStyleClass().add("custom-alert-pane");
        pane.setMinWidth(440);
        pane.setMinHeight(Region.USE_PREF_SIZE);

        // Replace default OS/JavaFX graphic with a clean matching badge icon
        pane.setGraphic(createBadgeGraphic(alert.getAlertType()));

        // Style all buttons inside the DialogPane
        for (ButtonType bt : pane.getButtonTypes()) {
            Node node = pane.lookupButton(bt);
            if (node instanceof Button btn) {
                btn.getStyleClass().removeAll("dialog-btn-primary", "dialog-btn-danger", "dialog-btn-secondary");
                ButtonBar.ButtonData data = bt.getButtonData();
                if (data == ButtonBar.ButtonData.CANCEL_CLOSE || data == ButtonBar.ButtonData.NO) {
                    btn.getStyleClass().add("dialog-btn-secondary");
                } else if (primaryIsDanger && alert.getAlertType() == Alert.AlertType.CONFIRMATION) {
                    btn.getStyleClass().add("dialog-btn-danger");
                } else {
                    btn.getStyleClass().add("dialog-btn-primary");
                }
            }
        }
    }

    private static Node createBadgeGraphic(Alert.AlertType type) {
        Color fillIce = Color.web("#f1f5f9");
        Color navyGlyph = Color.web("#2e4e83");

        StackPane container = new StackPane();
        container.setAlignment(Pos.CENTER);
        container.setMinSize(48, 48);
        container.setMaxSize(48, 48);

        switch (type) {
            case WARNING -> {
                // Rounded coral-red triangle
                Polygon triangle = new Polygon(
                        24.0, 4.0,
                        44.0, 40.0,
                        4.0, 40.0
                );
                triangle.setFill(fillIce);
                triangle.setStroke(Color.web("#f43f5e"));
                triangle.setStrokeWidth(4.5);
                triangle.setStrokeLineJoin(StrokeLineJoin.ROUND);

                // Tapered exclamation bar + circular dot centered inside triangle
                Polygon exBar = new Polygon(
                        -2.3, 0.0,
                         2.3, 0.0,
                         1.5, 11.0,
                        -1.5, 11.0
                );
                exBar.setFill(navyGlyph);

                Circle exDot = new Circle(2.2, navyGlyph);

                javafx.scene.layout.VBox exGlyph = new javafx.scene.layout.VBox(2.8, exBar, exDot);
                exGlyph.setAlignment(Pos.CENTER);
                exGlyph.setTranslateY(4.5);

                container.getChildren().addAll(triangle, exGlyph);
            }
            case ERROR -> {
                // Rounded coral-red square with crisp navy X
                Rectangle box = new Rectangle(38, 38);
                box.setArcWidth(12);
                box.setArcHeight(12);
                box.setFill(fillIce);
                box.setStroke(Color.web("#f43f5e"));
                box.setStrokeWidth(4.2);

                Line l1 = new Line(-6.5, -6.5, 6.5, 6.5);
                l1.setStroke(navyGlyph);
                l1.setStrokeWidth(3.8);
                l1.setStrokeLineCap(StrokeLineCap.ROUND);

                Line l2 = new Line(6.5, -6.5, -6.5, 6.5);
                l2.setStroke(navyGlyph);
                l2.setStrokeWidth(3.8);
                l2.setStrokeLineCap(StrokeLineCap.ROUND);

                StackPane xPane = new StackPane(l1, l2);
                container.getChildren().addAll(box, xPane);
            }
            case CONFIRMATION -> {
                // Sky-blue ring with crisp navy '?'
                Circle ring = new Circle(19.5);
                ring.setFill(fillIce);
                ring.setStroke(Color.web("#0284c7"));
                ring.setStrokeWidth(4.2);

                Text qMark = new Text("?");
                qMark.setFill(navyGlyph);
                qMark.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 21));

                container.getChildren().addAll(ring, qMark);
            }
            default -> { // INFORMATION
                // Navy ring with distinct dot + stem 'i' separated via VBox
                Circle ring = new Circle(19.5);
                ring.setFill(fillIce);
                ring.setStroke(Color.web("#395d87"));
                ring.setStrokeWidth(4.2);

                Circle iDot = new Circle(2.3, navyGlyph);

                Rectangle iStem = new Rectangle(4.2, 11.5, navyGlyph);
                iStem.setArcWidth(4.2);
                iStem.setArcHeight(4.2);

                javafx.scene.layout.VBox iGlyph = new javafx.scene.layout.VBox(3.2, iDot, iStem);
                iGlyph.setAlignment(Pos.CENTER);

                container.getChildren().addAll(ring, iGlyph);
            }
        }

        return container;
    }
}