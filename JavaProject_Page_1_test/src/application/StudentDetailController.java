package application;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
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

                // FIX: Use student.getNo() for UI display row index (e.g. 8) instead of database ID (e.g. 19)
                String bannerText = String.format("⭐     %d     |     %s     |     %s     |     %s     |     %s",
                        student.getDisplayNo(),
                        safe(student.getRollNo()),
                        safe(student.getName()),
                        safe(student.getMajor()),
                        safe(student.getYear()));

                Label bannerLabel = new Label(bannerText);
                bannerLabel.setStyle("-fx-text-fill: #1e3a8a; -fx-font-weight: bold; -fx-font-size: 15px;");
                bannerBox.getChildren().add(bannerLabel);
                contentContainer.getChildren().add(0, bannerBox);
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
        if (path != null && !path.trim().isEmpty()) {
            try {
                Image img = (path.startsWith("file:") || path.startsWith("http:"))
                        ? new Image(path, 86, 110, false, true)
                        : new Image(getClass().getResourceAsStream(path));
                if (img != null) {
                    passportImageView.setImage(img);
                }
            } catch (Exception e) {}
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
        try {
            // Find the root border pane and switch back to the main student list view
            Parent root = contentContainer.getScene().getRoot();
            if (root instanceof BorderPane) {
                BorderPane mainLayout = (BorderPane) root;
                Parent studentView = FXMLLoader.load(getClass().getResource("StudentContent.fxml"));
                mainLayout.setCenter(studentView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}