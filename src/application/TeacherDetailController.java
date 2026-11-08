package application;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
                Label bannerLabel = new Label(String.format("⭐     %d     |     %s     |     %s     |     %s     |     %s",
                    teacher.getTeacherId(), teacher.getTeacherCode(), teacher.getName(), teacher.getDepartment(), teacher.getPosition()));
                bannerLabel.setStyle("-fx-text-fill: #1e3a8a; -fx-font-weight: bold; -fx-font-size: 15px;");
                banner.getChildren().add(bannerLabel);
                contentContainer.getChildren().add(0, banner);
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

            if (teacher.getPhotoPath() != null) {
                try {
                    String path = teacher.getPhotoPath();
                    Image img = path.startsWith("file:") ? new Image(path, 86, 110, false, true) : new Image(getClass().getResourceAsStream(path));
                    passportImageView.setImage(img);
                } catch (Exception e) {}
            }
        }
    }

    @FXML
    private void handleReturn() {
        try {
            Parent teacherView = FXMLLoader.load(getClass().getResource("TeacherContents.fxml"));
            BorderPane mainLayout = (BorderPane) contentContainer.getScene().getRoot();
            mainLayout.setCenter(teacherView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    public void handleResetData() {
        // Read-only detail view reset handler
    }
}