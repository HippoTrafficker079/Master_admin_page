package application;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class StudentDetailController {

    @FXML private Label lblRollNo, lblDob, lblName, lblGender, lblMajor, lblNationality, lblYear, lblSemester, lblNrc;
    @FXML private Label lblPhone, lblEmail, lblCurrentAddress, lblPermanentAddress;
    @FXML private Label lblFatherName, lblFatherOcc, lblFatherPhone, lblFatherEmail;
    @FXML private Label lblMotherName, lblMotherOcc, lblMotherPhone, lblMotherEmail;
    @FXML private Label lblPrevSchool, lblYearCompleted, lblAdditionalInfo;
    @FXML private ImageView passportImageView;
    @FXML private VBox contentContainer;

    private HBox bannerBox = null;

    public void initData(Student student) {
        if (student != null) {
            if (contentContainer != null) {
                if (bannerBox != null && contentContainer.getChildren().contains(bannerBox)) {
                    contentContainer.getChildren().remove(bannerBox);
                }

                bannerBox = new HBox();
                bannerBox.setStyle(
                        "-fx-background-color: #dbeafe; -fx-border-color: #2563eb; -fx-border-width: 1.5; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 12 18;");

                String bannerText = String.format("⭐     %d     |     %s     |     %s     |     %s     |     %s",
                        student.getDisplayNo(),
                        safe(student.getRollNo()),
                        safe(student.getName()),
                        safe(student.getMajor()),
                        safe(student.getYear()));

                Label bannerLabel = new Label(bannerText);
                bannerLabel.setStyle("-fx-text-fill: #1e3a8a; -fx-font-weight: bold; -fx-font-size: 15px;");
                bannerBox.getChildren().add(bannerLabel);

                int insertIndex = contentContainer.getChildren().isEmpty() ? 0 : 1;
                contentContainer.getChildren().add(insertIndex, bannerBox);

                UIAnimator.playBannerPop(bannerBox);
                UIAnimator.attachDetailCardHover(bannerBox);
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

            if (lblRollNo != null) lblRollNo.setText(safe(student.getRollNo()));
            if (lblDob != null) lblDob.setText(safe(student.getDob()));
            if (lblName != null) lblName.setText(safe(student.getName()));
            if (lblGender != null) lblGender.setText(safe(student.getGender()));
            if (lblMajor != null) lblMajor.setText(safe(student.getMajor()));
            if (lblNationality != null) lblNationality.setText(safe(student.getNationality(), "Myanmar"));
            if (lblYear != null) lblYear.setText(safe(student.getYear()));
            if (lblSemester != null) lblSemester.setText(safe(student.getSemester()));
            if (lblNrc != null) lblNrc.setText(safe(student.getNrc()));

            if (lblPhone != null) lblPhone.setText(safe(student.getPhoneNo()));
            if (lblEmail != null) lblEmail.setText(safe(student.getEmail()));
            if (lblCurrentAddress != null) lblCurrentAddress.setText(safe(student.getCurrentAddress()));
            if (lblPermanentAddress != null) lblPermanentAddress.setText(safe(student.getPermanentAddress()));

            if (lblFatherName != null) lblFatherName.setText(safe(student.getFatherName()));
            if (lblFatherOcc != null) lblFatherOcc.setText(safe(student.getFatherOccupation()));
            if (lblFatherPhone != null) lblFatherPhone.setText(safe(student.getFatherPhone()));
            if (lblFatherEmail != null) lblFatherEmail.setText(safe(student.getFatherEmail()));

            if (lblMotherName != null) lblMotherName.setText(safe(student.getMotherName()));
            if (lblMotherOcc != null) lblMotherOcc.setText(safe(student.getMotherOccupation()));
            if (lblMotherPhone != null) lblMotherPhone.setText(safe(student.getMotherPhone()));
            if (lblMotherEmail != null) lblMotherEmail.setText(safe(student.getMotherEmail()));

            if (lblPrevSchool != null) lblPrevSchool.setText(safe(student.getPrevSchool()));
            if (lblYearCompleted != null) lblYearCompleted.setText(safe(student.getYearCompleted()));
            if (lblAdditionalInfo != null) lblAdditionalInfo.setText(safe(student.getAdditionalInfo()));

            loadPhoto(student.getPhotoPath());
        }
    }

    private void loadPhoto(String path) {
        if (passportImageView == null) return;
        if (path != null && !path.trim().isEmpty() && !path.trim().equalsIgnoreCase("null")) {
            try {
                Image img = (path.startsWith("file:") || path.startsWith("http:") || path.startsWith("https:"))
                        ? new Image(path, 170, 220, false, true)
                        : new Image(getClass().getResourceAsStream(path));
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

    private String safe(String val) {
        return safe(val, "");
    }

    private String safe(String val, String defaultVal) {
        if (val == null || val.trim().equalsIgnoreCase("null") || val.trim().equals("-")) {
            return defaultVal;
        }
        return val.trim();
    }

    @FXML
    private void handleReturn() {
        if (contentContainer == null || contentContainer.getScene() == null) return;
        Parent studentView = StudentController.getView();
        if (studentView != null) {
            BorderPane mainLayout = (BorderPane) contentContainer.getScene().lookup("#mainBorderPane");
            if (mainLayout != null) {
                mainLayout.setCenter(studentView);
                UIAnimator.playViewEntrance(studentView);
            } else if (contentContainer.getScene().getRoot() instanceof BorderPane) {
                ((BorderPane) contentContainer.getScene().getRoot()).setCenter(studentView);
                UIAnimator.playViewEntrance(studentView);
            }
        }
    }
}