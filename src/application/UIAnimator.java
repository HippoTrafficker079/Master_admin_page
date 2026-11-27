package application;

import java.util.ArrayList;
import java.util.List;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

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
     * Spring-scale pop (0.7 -> 1.08 -> 1.0) for AlertHelper vector badge icons.
     */
    public static void playBadgePop(Node badge) {
        if (badge == null) return;

        badge.setScaleX(0.7);
        badge.setScaleY(0.7);
        badge.setOpacity(0.0);

        FadeTransition fade = new FadeTransition(Duration.millis(140), badge);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        ScaleTransition overshoot = new ScaleTransition(Duration.millis(140), badge);
        overshoot.setFromX(0.7);
        overshoot.setFromY(0.7);
        overshoot.setToX(1.08);
        overshoot.setToY(1.08);
        overshoot.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition settle = new ScaleTransition(Duration.millis(90), badge);
        settle.setToX(1.0);
        settle.setToY(1.0);
        settle.setInterpolator(Interpolator.EASE_BOTH);

        new ParallelTransition(fade, new SequentialTransition(overshoot, settle)).play();
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
    public static void animateBarFill(Region bar, double targetWidth, int delayMs, int durationMs) {
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

            row.toFront();
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

    /**
     * Plays a staggered fade + slide-up entrance across all ".section-card" boxes inside a view.
     */
    public static void playSectionCardStagger(Node root) {
        if (root == null) return;
        List<Node> cards = new ArrayList<>();
        collectSectionCards(root, cards);

        for (int i = 0; i < cards.size(); i++) {
            Node card = cards.get(i);
            card.setOpacity(0.0);
            card.setTranslateY(12.0);

            PauseTransition delay = new PauseTransition(Duration.millis(i * 45.0));

            FadeTransition fade = new FadeTransition(Duration.millis(210), card);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.setInterpolator(Interpolator.EASE_OUT);

            TranslateTransition slide = new TranslateTransition(Duration.millis(210), card);
            slide.setFromY(12.0);
            slide.setToY(0.0);
            slide.setInterpolator(Interpolator.EASE_OUT);

            new SequentialTransition(delay, new ParallelTransition(fade, slide)).play();
        }
    }

    private static void collectSectionCards(Node root, List<Node> out) {
        if (root == null) return;
        if (root.getStyleClass().contains("section-card")) {
            out.add(root);
            return;
        }
        if (root instanceof ScrollPane sp && sp.getContent() != null) {
            collectSectionCards(sp.getContent(), out);
        }
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectSectionCards(child, out);
            }
        }
    }

    /**
     * Slides the 4px sidebar accent bar smoothly to align vertically with the active nav button.
     */
    public static void moveSidebarIndicator(Node indicator, Button activeBtn, boolean animate) {
        if (indicator == null || activeBtn == null) return;

        Platform.runLater(() -> {
            double indicatorH = indicator.getLayoutBounds().getHeight() > 0
                    ? indicator.getLayoutBounds().getHeight() : 33.0;
            double btnH = activeBtn.getHeight() > 0 ? activeBtn.getHeight() : 45.0;
            double targetY = activeBtn.getLayoutY() + (btnH - indicatorH) / 2.0;

            if (!animate) {
                indicator.setTranslateY(targetY);
                return;
            }

            TranslateTransition slide = new TranslateTransition(Duration.millis(180), indicator);
            slide.setToY(targetY);
            slide.setInterpolator(Interpolator.EASE_BOTH);
            slide.play();
        });
    }

    /**
     * Displays a non-blocking dark-navy toast notification at the bottom-right of the window.
     */
    public static void showToast(Node anchor, String message) {
        if (anchor == null || anchor.getScene() == null || anchor.getScene().getWindow() == null) {
            return;
        }
        showToast(anchor.getScene().getWindow(), message);
    }

    public static void showToast(Window owner, String message) {
        if (owner == null || message == null || message.trim().isEmpty()) return;

        Platform.runLater(() -> {
            Popup popup = new Popup();
            popup.setAutoFix(true);
            popup.setAutoHide(false);
            popup.setHideOnEscape(true);

            Label icon = new Label("✔");
            icon.setStyle("-fx-text-fill: #34d399; -fx-font-weight: bold; -fx-font-size: 13.5px;");

            Label text = new Label(message);
            text.setStyle("-fx-text-fill: #f8fafc; -fx-font-family: 'Segoe UI', sans-serif; -fx-font-weight: bold; -fx-font-size: 13px;");

            HBox pill = new HBox(10.0, icon, text);
            pill.setAlignment(Pos.CENTER_LEFT);
            pill.setMouseTransparent(true);
            pill.setStyle(
                    "-fx-background-color: #0f172a; " +
                    "-fx-background-radius: 8px; " +
                    "-fx-border-color: #334155; " +
                    "-fx-border-radius: 8px; " +
                    "-fx-border-width: 1px; " +
                    "-fx-padding: 11px 18px; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(15, 23, 42, 0.35), 14, 0, 0, 5);"
            );

            popup.getContent().add(pill);
            pill.setOpacity(0.0);
            pill.setTranslateY(14.0);

            // Show briefly to compute real width/height, then anchor at bottom-right
            popup.show(owner);
            double w = pill.getWidth() > 0 ? pill.getWidth() : pill.prefWidth(-1);
            double h = pill.getHeight() > 0 ? pill.getHeight() : pill.prefHeight(-1);
            popup.setX(owner.getX() + owner.getWidth() - w - 26.0);
            popup.setY(owner.getY() + owner.getHeight() - h - 28.0);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(190), pill);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.setInterpolator(Interpolator.EASE_OUT);

            TranslateTransition slideUp = new TranslateTransition(Duration.millis(190), pill);
            slideUp.setFromY(14.0);
            slideUp.setToY(0.0);
            slideUp.setInterpolator(Interpolator.EASE_OUT);

            PauseTransition hold = new PauseTransition(Duration.millis(2100));

            FadeTransition fadeOut = new FadeTransition(Duration.millis(220), pill);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setInterpolator(Interpolator.EASE_IN);

            TranslateTransition slideDown = new TranslateTransition(Duration.millis(220), pill);
            slideDown.setFromY(0.0);
            slideDown.setToY(8.0);

            SequentialTransition seq = new SequentialTransition(
                    new ParallelTransition(fadeIn, slideUp),
                    hold,
                    new ParallelTransition(fadeOut, slideDown)
            );
            seq.setOnFinished(e -> popup.hide());
            seq.play();
        });
    }
    
    /**
     * Clips an ImageView with rounded corners so it sits cleanly inside a rounded border frame
     * without sharp image corners poking outside the border.
     */
    public static void applyRoundedPhotoClip(javafx.scene.image.ImageView imageView, double width, double height, double cornerRadius) {
        if (imageView == null) return;
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        imageView.setPreserveRatio(false);

        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(width, height);
        clip.setArcWidth(cornerRadius * 2.0);
        clip.setArcHeight(cornerRadius * 2.0);
        imageView.setClip(clip);
    }
    
    /**
     * Sets a clean, YTU-styled empty-state placeholder on any TableView.
     */
    public static void setEmptyTablePlaceholder(TableView<?> table, String icon, String title, String subtitle) {
        if (table == null) return;

        Label iconLbl = new Label(icon);
        iconLbl.getStyleClass().add("empty-state-icon");

        javafx.scene.layout.StackPane iconCircle = new javafx.scene.layout.StackPane(iconLbl);
        iconCircle.getStyleClass().add("empty-state-icon-circle");

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("empty-state-title");

        Label subtitleLbl = new Label(subtitle);
        subtitleLbl.getStyleClass().add("empty-state-subtitle");
        subtitleLbl.setWrapText(true);
        subtitleLbl.setMaxWidth(340);

        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(iconCircle, titleLbl, subtitleLbl);
        box.getStyleClass().add("empty-state-box");

        table.setPlaceholder(box);
    }

    /**
     * Recursively applies a default styled placeholder to any TableView inside a view
     * that does not already have a custom placeholder set (e.g., Dashboard tables).
     */
    public static void applyDefaultEmptyPlaceholders(Node root, String icon, String title, String subtitle) {
        if (root == null) return;

        if (root instanceof TableView<?> tv) {
            if (tv.getPlaceholder() == null || tv.getPlaceholder() instanceof Label) {
                setEmptyTablePlaceholder(tv, icon, title, subtitle);
            }
            return;
        }

        if (root instanceof ScrollPane sp && sp.getContent() != null) {
            applyDefaultEmptyPlaceholders(sp.getContent(), icon, title, subtitle);
        }

        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                applyDefaultEmptyPlaceholders(child, icon, title, subtitle);
            }
        }
    }

    /**
     * Attaches view-scoped keyboard shortcuts safely (only fires while viewRoot is attached & visible).
     * Prevents Delete from firing while the user is typing in a TextField/TextArea.
     */
    public static void attachShortcutHandler(Node viewRoot, javafx.event.EventHandler<javafx.scene.input.KeyEvent> handler) {
        if (viewRoot == null || handler == null) return;

        javafx.event.EventHandler<javafx.scene.input.KeyEvent> scopedHandler = event -> {
            if (event.isConsumed()) return;
            if (viewRoot.getScene() == null || !viewRoot.isVisible()) return;
            // For views swapped inside BorderPane.center, skip when detached from parent
            if (viewRoot.getScene().getRoot() != viewRoot && viewRoot.getParent() == null) return;
            handler.handle(event);
        };

        viewRoot.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) {
                oldScene.removeEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, scopedHandler);
            }
            if (newScene != null) {
                newScene.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, scopedHandler);
            }
        });

        if (viewRoot.getScene() != null) {
            viewRoot.getScene().addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, scopedHandler);
        }
    }

    /**
     * Returns true if the current keyboard focus is inside a text input control.
     */
    public static boolean isTypingInTextInput(javafx.scene.Scene scene) {
        if (scene == null) return false;
        Node focusOwner = scene.getFocusOwner();
        return focusOwner instanceof javafx.scene.control.TextInputControl;
    }
    
    /**
     * 2. Sky-Blue Shimmer Sweep: Glides a diagonal light sheen strictly across the opaque
     * pixels of YTU_logo.png every ~4.2 seconds.
     */
    public static void attachLogoShimmer(javafx.scene.image.ImageView logo) {
        if (logo == null || logo.getImage() == null
                || Boolean.TRUE.equals(logo.getProperties().get("uiAnimatorShimmer"))) {
            return;
        }
        if (!(logo.getParent() instanceof javafx.scene.layout.Pane parent)) {
            return;
        }
        logo.getProperties().put("uiAnimatorShimmer", Boolean.TRUE);

        double boundsW = logo.getBoundsInLocal().getWidth();
        double boundsH = logo.getBoundsInLocal().getHeight();
        double w = boundsW > 0 ? boundsW : (logo.getFitWidth() > 0 ? logo.getFitWidth() : 185.0);
        double h = boundsH > 0 ? boundsH : (logo.getFitHeight() > 0 ? logo.getFitHeight() : 85.0);

        // Clone the logo ImageView to act as an exact alpha mask
        javafx.scene.image.ImageView mask = new javafx.scene.image.ImageView(logo.getImage());
        mask.setFitWidth(logo.getFitWidth());
        mask.setFitHeight(logo.getFitHeight());
        mask.setPreserveRatio(logo.isPreserveRatio());
        mask.setSmooth(true);

        // Diagonal sky-blue & white sheen gradient
        javafx.scene.paint.LinearGradient sheenGradient = new javafx.scene.paint.LinearGradient(
                0, 0, 1, 0, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0.00, Color.TRANSPARENT),
                new javafx.scene.paint.Stop(0.42, Color.rgb(186, 230, 253, 0.45)),
                new javafx.scene.paint.Stop(0.50, Color.rgb(255, 255, 255, 0.85)),
                new javafx.scene.paint.Stop(0.58, Color.rgb(186, 230, 253, 0.45)),
                new javafx.scene.paint.Stop(1.00, Color.TRANSPARENT)
        );

        javafx.scene.shape.Rectangle beam = new javafx.scene.shape.Rectangle(w * 0.42, h * 2.0, sheenGradient);
        beam.setRotate(22);
        beam.setTranslateX(-w);
        beam.setTranslateY(-h * 0.45);

        javafx.scene.layout.Pane shimmerLayer = new javafx.scene.layout.Pane(beam);
        shimmerLayer.setPrefSize(w, h);
        shimmerLayer.setMaxSize(w, h);
        shimmerLayer.setMouseTransparent(true);
        shimmerLayer.setClip(mask);

        // Swap logo into a StackPane at the exact same index & layout position
        int idx = parent.getChildren().indexOf(logo);
        javafx.geometry.Insets oldVBoxMargin = javafx.scene.layout.VBox.getMargin(logo);
        double oldLayoutX = logo.getLayoutX();
        double oldLayoutY = logo.getLayoutY();

        parent.getChildren().remove(idx);

        javafx.scene.layout.StackPane wrapper = new javafx.scene.layout.StackPane(logo, shimmerLayer);
        wrapper.setAlignment(javafx.geometry.Pos.CENTER);
        wrapper.setMaxSize(javafx.scene.layout.Region.USE_PREF_SIZE, javafx.scene.layout.Region.USE_PREF_SIZE);
        wrapper.setLayoutX(oldLayoutX);
        wrapper.setLayoutY(oldLayoutY);
        if (oldVBoxMargin != null) {
            javafx.scene.layout.VBox.setMargin(wrapper, oldVBoxMargin);
            javafx.scene.layout.VBox.setMargin(logo, null);
        }

        parent.getChildren().add(idx, wrapper);
        logo.getProperties().put("uiAnimatorLogoWrapper", wrapper);

        // Sweep animation: 950ms glide, then 4.2s pause, repeating smoothly
        TranslateTransition sweep = new TranslateTransition(Duration.millis(950), beam);
        sweep.setFromX(-w * 0.85);
        sweep.setToX(w * 1.35);
        sweep.setInterpolator(Interpolator.EASE_BOTH);

        javafx.animation.SequentialTransition loop = new javafx.animation.SequentialTransition(
                new javafx.animation.PauseTransition(Duration.seconds(1.2)),
                sweep,
                new javafx.animation.PauseTransition(Duration.seconds(4.2))
        );
        loop.setCycleCount(javafx.animation.Animation.INDEFINITE);
        loop.play();
    }

    /**
     * 1. Startup Reveal: Smooth fade + scale + soft glow flash when the app launches.
     */
    public static void playLogoEntrance(javafx.scene.image.ImageView logo) {
        if (logo == null) return;
        Node target = (logo.getProperties().get("uiAnimatorLogoWrapper") instanceof Node w) ? w : logo;

        target.setOpacity(0.0);
        target.setScaleX(0.90);
        target.setScaleY(0.90);

        javafx.scene.effect.Glow glow = new javafx.scene.effect.Glow(0.65);
        target.setEffect(glow);

        FadeTransition fade = new FadeTransition(Duration.millis(480), target);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scale = new ScaleTransition(Duration.millis(480), target);
        scale.setFromX(0.90);
        scale.setFromY(0.90);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(Interpolator.EASE_OUT);

        Timeline fadeGlow = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(glow.levelProperty(), 0.65)),
                new KeyFrame(Duration.millis(700), new KeyValue(glow.levelProperty(), 0.0, Interpolator.EASE_OUT))
        );

        ParallelTransition entrance = new ParallelTransition(fade, scale, fadeGlow);
        entrance.setOnFinished(e -> {
            if (target.getEffect() == glow) {
                target.setEffect(null);
            }
        });
        entrance.play();
    }

    /**
     * 4. Interactive Hover Aura (hover-only, no navigation on click).
     */
    public static void attachLogoHover(javafx.scene.image.ImageView logo) {
        if (logo == null || Boolean.TRUE.equals(logo.getProperties().get("uiAnimatorLogoHover"))) {
            return;
        }
        logo.getProperties().put("uiAnimatorLogoHover", Boolean.TRUE);
        Node target = (logo.getProperties().get("uiAnimatorLogoWrapper") instanceof Node w) ? w : logo;

        // Subtle cyan-blue edge glow matching your active sidebar accent bar (#38BDF8)
        DropShadow hoverGlow = new DropShadow(12.0, 0.0, 2.0, Color.rgb(56, 189, 248, 0.42));
        // Alternative: crisp dark-navy 3D lift shadow instead of a glow:
        // DropShadow hoverGlow = new DropShadow(14.0, 0.0, 4.0, Color.rgb(15, 23, 42, 0.55));

        target.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(160), target);
            st.setToX(1.04);
            st.setToY(1.04);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
            target.setEffect(hoverGlow);
        });

        target.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(160), target);
            st.setToX(1.0);
            st.setToY(1.0);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
            target.setEffect(null);
        });
    }
    
}