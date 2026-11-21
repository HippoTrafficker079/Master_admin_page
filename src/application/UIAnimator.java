package application;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

public final class UIAnimator {

    private UIAnimator() {
    }

    /**
     * Subtle fade + upward glide when switching views, tabs, or sub-pages.
     */
    public static void playViewEntrance(Node node) {
        if (node == null) return;

        node.setOpacity(0.0);
        node.setTranslateY(8.0);

        FadeTransition fade = new FadeTransition(Duration.millis(200), node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slide = new TranslateTransition(Duration.millis(200), node);
        slide.setFromY(8.0);
        slide.setToY(0.0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fade, slide).play();
    }

    /**
     * Smooth hover lift (-3px) + soft elevation shadow for Dashboard KPI cards.
     */
    public static void attachCardHover(Node card) {
        if (card == null) return;

        DropShadow restShadow = new DropShadow(8.0, 0.0, 2.0, Color.rgb(15, 23, 42, 0.05));
        DropShadow hoverShadow = new DropShadow(16.0, 0.0, 6.0, Color.rgb(37, 99, 235, 0.14));
        card.setEffect(restShadow);

        card.setOnMouseEntered(e -> {
            TranslateTransition lift = new TranslateTransition(Duration.millis(160), card);
            lift.setToY(-3.0);
            lift.setInterpolator(Interpolator.EASE_OUT);

            ScaleTransition scale = new ScaleTransition(Duration.millis(160), card);
            scale.setToX(1.015);
            scale.setToY(1.015);
            scale.setInterpolator(Interpolator.EASE_OUT);

            card.setEffect(hoverShadow);
            new ParallelTransition(lift, scale).play();
        });

        card.setOnMouseExited(e -> {
            TranslateTransition drop = new TranslateTransition(Duration.millis(160), card);
            drop.setToY(0.0);
            drop.setInterpolator(Interpolator.EASE_OUT);

            ScaleTransition scale = new ScaleTransition(Duration.millis(160), card);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.setInterpolator(Interpolator.EASE_OUT);

            card.setEffect(restShadow);
            new ParallelTransition(drop, scale).play();
        });
    }

    /**
     * Clean hover scale (1.03x) + soft shadow for action buttons (safe to call multiple times).
     */
    public static void attachButtonHover(Node button) {
        if (button == null || Boolean.TRUE.equals(button.getProperties().get("uiAnimatorHover"))) {
            return;
        }
        button.getProperties().put("uiAnimatorHover", Boolean.TRUE);

        DropShadow hoverShadow = new DropShadow(12.0, 0.0, 3.0, Color.rgb(30, 58, 138, 0.24));

        button.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(130), button);
            st.setToX(1.03);
            st.setToY(1.03);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
            button.setEffect(hoverShadow);
        });

        button.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(130), button);
            st.setToX(1.0);
            st.setToY(1.0);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
            button.setEffect(null);
        });
    }

    /**
     * Automatically attaches hover lift + click tap pulse to every Button inside a view or ScrollPane.
     * Skips tiny NRC help ("!") buttons and sidebar buttons.
     */
    public static void attachAllButtonAnimations(Node root) {
        if (root == null) return;

        if (root instanceof Button btn) {
            if (btn.getStyleClass().contains("nrc-help-button")
                    || btn.getStyleClass().contains("sidebar-button")
                    || btn.getStyleClass().contains("sidebar-button-active")) {
                return;
            }
            attachButtonHover(btn);
            if (!Boolean.TRUE.equals(btn.getProperties().get("uiAnimatorTap"))) {
                btn.getProperties().put("uiAnimatorTap", Boolean.TRUE);
                btn.addEventHandler(ActionEvent.ACTION, e -> playTapPulse(btn));
            }
            return;
        }

        if (root instanceof ScrollPane sp && sp.getContent() != null) {
            attachAllButtonAnimations(sp.getContent());
        }

        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                attachAllButtonAnimations(child);
            }
        }
    }

    /**
     * Soft blue focus glow for search bars and input controls.
     */
    public static void attachFocusGlow(Node field) {
        if (field == null) return;

        DropShadow focusShadow = new DropShadow(10.0, 0.0, 0.0, Color.rgb(37, 99, 235, 0.28));
        field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            field.setEffect(isFocused ? focusShadow : null);
        });
    }

    /**
     * Quick tactile click pulse for action buttons, tabs, and status toggles.
     */
    public static void playTapPulse(Node node) {
        if (node == null) return;

        ScaleTransition down = new ScaleTransition(Duration.millis(65), node);
        down.setToX(0.94);
        down.setToY(0.94);

        ScaleTransition up = new ScaleTransition(Duration.millis(95), node);
        up.setToX(1.0);
        up.setToY(1.0);
        up.setInterpolator(Interpolator.EASE_OUT);

        down.setOnFinished(e -> up.play());
        down.play();
    }

    /**
     * Fast subtle fade when applying table search or dropdown filters.
     */
    public static void playTableFilterFade(Node table) {
        if (table == null) return;

        FadeTransition fade = new FadeTransition(Duration.millis(160), table);
        fade.setFromValue(0.65);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_OUT);
        fade.play();
    }

    /**
     * Subtle horizontal shake for missing selection or validation feedback.
     */
    public static void playShake(Node node) {
        if (node == null) return;

        Timeline shake = new Timeline(
                new KeyFrame(Duration.millis(0),   new KeyValue(node.translateXProperty(), 0)),
                new KeyFrame(Duration.millis(45),  new KeyValue(node.translateXProperty(), -5)),
                new KeyFrame(Duration.millis(90),  new KeyValue(node.translateXProperty(), 5)),
                new KeyFrame(Duration.millis(135), new KeyValue(node.translateXProperty(), -4)),
                new KeyFrame(Duration.millis(180), new KeyValue(node.translateXProperty(), 3)),
                new KeyFrame(Duration.millis(220), new KeyValue(node.translateXProperty(), 0))
        );
        shake.play();
    }

    /**
     * Crisp entrance pop for detail/modify summary banners.
     */
    public static void playBannerPop(Node banner) {
        if (banner == null) return;

        banner.setOpacity(0.0);
        banner.setScaleX(0.98);
        banner.setScaleY(0.98);

        FadeTransition fade = new FadeTransition(Duration.millis(220), banner);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scale = new ScaleTransition(Duration.millis(220), banner);
        scale.setFromX(0.98);
        scale.setFromY(0.98);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fade, scale).play();
    }

    /**
     * Smooth integer count-up animation for KPI summary numbers.
     */
    public static Timeline animateIntCount(Label label, int target, boolean useCommas, int durationMs) {
        if (label == null) return null;
        if (target <= 0) {
            label.setText(useCommas ? String.format("%,d", target) : String.valueOf(target));
            return null;
        }

        IntegerProperty counter = new SimpleIntegerProperty(0);
        counter.addListener((obs, oldVal, newVal) -> {
            int val = newVal.intValue();
            label.setText(useCommas ? String.format("%,d", val) : String.valueOf(val));
        });

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(counter, 0)),
                new KeyFrame(Duration.millis(durationMs), new KeyValue(counter, target, Interpolator.EASE_OUT))
        );
        timeline.play();
        return timeline;
    }

    /**
     * Smooth percentage count-up animation (e.g., Today's Attendance).
     */
    public static Timeline animatePercentCount(Label label, double target, int durationMs) {
        if (label == null) return null;
        if (target <= 0.0) {
            label.setText(String.format("%.1f%%", target));
            return null;
        }

        DoubleProperty counter = new SimpleDoubleProperty(0.0);
        counter.addListener((obs, oldVal, newVal) ->
                label.setText(String.format("%.1f%%", newVal.doubleValue())));

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(counter, 0.0)),
                new KeyFrame(Duration.millis(durationMs), new KeyValue(counter, target, Interpolator.EASE_OUT))
        );
        timeline.play();
        return timeline;
    }
    
    /**
     * Smoothly animates a horizontal progress bar's width from 0 to its target width.
     */
    public static void animateBarFill(javafx.scene.layout.Region bar, double targetWidth, int delayMs, int durationMs) {
        if (bar == null) return;

        bar.setPrefWidth(0.0);
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(delayMs), new KeyValue(bar.prefWidthProperty(), 0.0)),
                new KeyFrame(Duration.millis(delayMs + durationMs),
                        new KeyValue(bar.prefWidthProperty(), Math.max(0.0, targetWidth), Interpolator.EASE_OUT))
        );
        timeline.play();
    }
    
    /**
     * Smooth hover lift + soft blue glow for ComboBoxes, segmented toggles, and pills.
     */
    public static void attachControlHover(Node control) {
        if (control == null || Boolean.TRUE.equals(control.getProperties().get("uiAnimatorControlHover"))) {
            return;
        }
        control.getProperties().put("uiAnimatorControlHover", Boolean.TRUE);

        DropShadow hoverShadow = new DropShadow(12.0, 0.0, 3.0, Color.rgb(37, 99, 235, 0.22));

        control.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            TranslateTransition lift = new TranslateTransition(Duration.millis(140), control);
            lift.setToY(-1.5);
            lift.setInterpolator(Interpolator.EASE_OUT);

            ScaleTransition scale = new ScaleTransition(Duration.millis(140), control);
            scale.setToX(1.025);
            scale.setToY(1.025);
            scale.setInterpolator(Interpolator.EASE_OUT);

            control.setEffect(hoverShadow);
            new ParallelTransition(lift, scale).play();
        });

        control.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            TranslateTransition drop = new TranslateTransition(Duration.millis(140), control);
            drop.setToY(0.0);
            drop.setInterpolator(Interpolator.EASE_OUT);

            ScaleTransition scale = new ScaleTransition(Duration.millis(140), control);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.setInterpolator(Interpolator.EASE_OUT);

            control.setEffect(null);
            new ParallelTransition(drop, scale).play();
        });
    }

    /**
     * KPI-card-style hover lift + soft blue elevation glow for an individual TableRow.
     * Safe for recycled rows and preserves existing row CSS styles (Present/Absent/Low Attendance).
     */
    public static void attachRowHover(TableRow<?> row) {
        if (row == null || Boolean.TRUE.equals(row.getProperties().get("uiAnimatorRowHover"))) {
            return;
        }
        row.getProperties().put("uiAnimatorRowHover", Boolean.TRUE);

        DropShadow rowHoverShadow = new DropShadow(14.0, 0.0, 3.0, Color.rgb(37, 99, 235, 0.22));

        row.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            if (row.isEmpty() || row.getItem() == null) return;

            row.toFront(); // Keeps the soft shadow visible above adjacent rows
            TranslateTransition lift = new TranslateTransition(Duration.millis(140), row);
            lift.setToY(-1.5);
            lift.setInterpolator(Interpolator.EASE_OUT);

            ScaleTransition scale = new ScaleTransition(Duration.millis(140), row);
            scale.setToX(1.003);
            scale.setToY(1.02);
            scale.setInterpolator(Interpolator.EASE_OUT);

            row.setEffect(rowHoverShadow);
            new ParallelTransition(lift, scale).play();
        });

        row.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            TranslateTransition drop = new TranslateTransition(Duration.millis(140), row);
            drop.setToY(0.0);
            drop.setInterpolator(Interpolator.EASE_OUT);

            ScaleTransition scale = new ScaleTransition(Duration.millis(140), row);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.setInterpolator(Interpolator.EASE_OUT);

            row.setEffect(null);
            new ParallelTransition(drop, scale).play();
        });

        // Reset immediately if JavaFX recycles the row into an empty slot while scrolling
        row.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
            if (isNowEmpty) {
                row.setTranslateY(0.0);
                row.setScaleX(1.0);
                row.setScaleY(1.0);
                row.setEffect(null);
            }
        });
    }

    /**
     * One-line helper for TableViews that do not already define a custom RowFactory.
     */
    public static <T> void attachTableRowHover(TableView<T> table) {
        if (table == null) return;
        table.setRowFactory(tv -> {
            TableRow<T> row = new TableRow<>();
            attachRowHover(row);
            return row;
        });
    }
    
    /**
     * KPI-style hover lift (-3px) + soft blue elevation glow tuned for wider detail section cards.
     */
    public static void attachDetailCardHover(Node card) {
        if (card == null || Boolean.TRUE.equals(card.getProperties().get("uiAnimatorDetailCardHover"))) {
            return;
        }
        card.getProperties().put("uiAnimatorDetailCardHover", Boolean.TRUE);

        DropShadow restShadow = new DropShadow(8.0, 0.0, 2.0, Color.rgb(15, 23, 42, 0.05));
        DropShadow hoverShadow = new DropShadow(16.0, 0.0, 6.0, Color.rgb(37, 99, 235, 0.16));
        card.setEffect(restShadow);

        card.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            TranslateTransition lift = new TranslateTransition(Duration.millis(160), card);
            lift.setToY(-3.0);
            lift.setInterpolator(Interpolator.EASE_OUT);

            ScaleTransition scale = new ScaleTransition(Duration.millis(160), card);
            scale.setToX(1.006);
            scale.setToY(1.006);
            scale.setInterpolator(Interpolator.EASE_OUT);

            card.setEffect(hoverShadow);
            new ParallelTransition(lift, scale).play();
        });

        card.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            TranslateTransition drop = new TranslateTransition(Duration.millis(160), card);
            drop.setToY(0.0);
            drop.setInterpolator(Interpolator.EASE_OUT);

            ScaleTransition scale = new ScaleTransition(Duration.millis(160), card);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.setInterpolator(Interpolator.EASE_OUT);

            card.setEffect(restShadow);
            new ParallelTransition(drop, scale).play();
        });
    }

    /**
     * Automatically attaches KPI-style hover lifts to all ".section-card" containers inside a view.
     */
    public static void attachSectionCardHovers(Node root) {
        if (root == null) return;

        if (root.getStyleClass().contains("section-card")) {
            attachDetailCardHover(root);
        }

        if (root instanceof ScrollPane sp && sp.getContent() != null) {
            attachSectionCardHovers(sp.getContent());
        }

        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                attachSectionCardHovers(child);
            }
        }
    }
}