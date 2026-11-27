package application;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class TeacherDetailController {

    @FXML private Label lblTeacherId, lblDob, lblName, lblGender, lblDept, lblNationality, lblPosition, lblNrc;
    @FXML private Label lblPhone, lblEmail, lblCurrentAddress, lblPermanentAddress;
    @FXML private Label lblDegree, lblUniversity, lblYearCompleted, lblSpecialization, lblEmergency;
    @FXML private ImageView passportImageView;
    @FXML private VBox contentContainer;

    public void initData(Teacher teacher) {
        if (teacher != null) {
            if (contentContainer != null) {
                HBox banner = new HBox();
                banner.setStyle("-fx-background-color: #dbeafe; -fx-border-color: #2563eb; -fx-border-width: 1.5; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 12 18;");

                int displayRowNo = teacher.getDisplayNo() > 0 ? teacher.getDisplayNo() : teacher.getTeacherId();
                Label bannerLabel = new Label(String.format("⭐     %d     |     %s     |     %s     |     %s     |     %s",
                        displayRowNo, teacher.getTeacherCode(), teacher.getName(), teacher.getDepartment(), teacher.getPosition()));
                bannerLabel.setStyle("-fx-text-fill: #1e3a8a; -fx-font-weight: bold; -fx-font-size: 15px;");
                banner.getChildren().add(bannerLabel);
                contentContainer.getChildren().add(1, banner);

                UIAnimator.playBannerPop(banner);
                UIAnimator.attachDetailCardHover(banner);
                UIAnimator.attachSectionCardHovers(contentContainer);
                UIAnimator.playSectionCardStagger(contentContainer);
            }
            
            if (!Boolean.TRUE.equals(contentContainer.getProperties().get("escBound"))) {
                contentContainer.getProperties().put("escBound", Boolean.TRUE);
                UIAnimator.attachShortcutHandler(contentContainer, event -> {
                    if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                        handleReturn();
                        event.consume();
                    }
                });
            }

            lblTeacherId.setText(teacher.getTeacherCode());
            lblDob.setText(teacher.getDob());
            lblName.setText(teacher.getName());
            lblGender.setText(teacher.getGender());
            lblDept.setText(teacher.getDepartment());
            lblNationality.setText(teacher.getNationality());
            lblPosition.setText(teacher.getPosition());
            lblNrc.setText(teacher.getNrc());

            lblPhone.setText(teacher.getPhoneNo());
            lblEmail.setText(teacher.getEmail());
            lblCurrentAddress.setText(teacher.getCurrentAddress());
            lblPermanentAddress.setText(teacher.getPermanentAddress());

            lblDegree.setText(teacher.getDegree());
            lblUniversity.setText(teacher.getUniversity());
            lblYearCompleted.setText(teacher.getYearCompleted());
            lblSpecialization.setText(teacher.getSpecialization());
            lblEmergency.setText(teacher.getEmergencyContact());

            if (teacher.getPhotoPath() != null && !teacher.getPhotoPath().trim().isEmpty()
                    && !teacher.getPhotoPath().trim().equalsIgnoreCase("null")) {
                try {
                    String path = teacher.getPhotoPath().trim();
                    Image img = (path.startsWith("file:") || path.startsWith("http:") || path.startsWith("https:"))
                            ? new Image(path, 170, 220, false, true)
                            : new Image(getClass().getResourceAsStream(path), 170, 220, false, true);
                    if (img != null && !img.isError()) {
                        passportImageView.setImage(img);
                        UIAnimator.applyRoundedPhotoClip(passportImageView, 85.0, 110.0, 4.5);
                    } else {
                        passportImageView.setImage(null);
                    }
                } catch (Exception e) {
                    passportImageView.setImage(null);
                }
            } else {
                passportImageView.setImage(null);
            }
        }
    }

    @FXML
    private void handleReturn() {
        if (contentContainer == null || contentContainer.getScene() == null) {
            return;
        }
        Parent teacherView = TeacherController.getView();
        if (teacherView != null) {
            BorderPane mainLayout = (BorderPane) contentContainer.getScene().lookup("#mainBorderPane");
            if (mainLayout != null) {
                mainLayout.setCenter(teacherView);
                UIAnimator.playViewEntrance(teacherView);
            } else if (contentContainer.getScene().getRoot() instanceof BorderPane) {
                ((BorderPane) contentContainer.getScene().getRoot()).setCenter(teacherView);
                UIAnimator.playViewEntrance(teacherView);
            }
        }
    }
}