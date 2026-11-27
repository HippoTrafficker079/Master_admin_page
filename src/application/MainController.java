package application;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;

public class MainController {

    @FXML private BorderPane mainBorderPane;
    @FXML private Button btnDashboard;
    @FXML private Button btnManageTeacher;
    @FXML private Button btnManageStudent;
    @FXML private Button btnAttendance;
    @FXML private Region sidebarIndicator;
    @FXML private javafx.scene.image.ImageView ytuLogoImageView;

    private Parent dashboardView;
    private DashboardController dashboardController;
    private Parent teacherView;
    private Parent studentView;
    private Parent attendanceView;

    @FXML
    public void initialize() {
        try {
            FXMLLoader dashboardLoader = new FXMLLoader(getClass().getResource("DashboardContent.fxml"));
            dashboardView = dashboardLoader.load();
            dashboardController = dashboardLoader.getController();

            teacherView = FXMLLoader.load(getClass().getResource("TeacherContents.fxml"));
            studentView = FXMLLoader.load(getClass().getResource("StudentContent.fxml"));
            attendanceView = FXMLLoader.load(getClass().getResource("AttendanceContent.fxml"));

            // Default startup view (Dashboard tab active)
            mainBorderPane.setCenter(dashboardView);
            UIAnimator.playViewEntrance(dashboardView);
            setActiveButton(btnDashboard, false);
         // Apply default styled placeholders to any tables on the Dashboard
            UIAnimator.applyDefaultEmptyPlaceholders(
                    dashboardView,
                    "\uD83D\uDCCA",
                    "No Records Available",
                    "Dashboard table data will appear here once records are added.");

            // Global sidebar navigation shortcuts (Ctrl + 1 / 2 / 3 / 4)
            UIAnimator.attachShortcutHandler(mainBorderPane, event -> {
                if (event.isControlDown() && !event.isShiftDown() && !event.isAltDown()) {
                    switch (event.getCode()) {
                        case DIGIT1 -> { showDashboardView(null); event.consume(); }
                        case DIGIT2 -> { showStudentView(null); event.consume(); }
                        case DIGIT3 -> { showTeacherView(null); event.consume(); }
                        case DIGIT4 -> { showAttendanceView(null); event.consume(); }
                        default -> {}
                    }
                }
            });
            if (ytuLogoImageView != null) {
                UIAnimator.attachLogoShimmer(ytuLogoImageView);
                UIAnimator.playLogoEntrance(ytuLogoImageView);
                UIAnimator.attachLogoHover(ytuLogoImageView);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void showDashboardView(ActionEvent event) {
        UIAnimator.playTapPulse(btnDashboard);
        mainBorderPane.setCenter(dashboardView);
        UIAnimator.playViewEntrance(dashboardView);
        setActiveButton(btnDashboard, true);
        if (dashboardController != null) {
            DashboardController.refreshDashboard();
        }
    }

    @FXML
    public void showTeacherView(ActionEvent event) {
        UIAnimator.playTapPulse(btnManageTeacher);
        mainBorderPane.setCenter(teacherView);
        UIAnimator.playViewEntrance(teacherView);
        setActiveButton(btnManageTeacher, true);
        TeacherController.refreshLive();
    }

    @FXML
    public void showStudentView(ActionEvent event) {
        UIAnimator.playTapPulse(btnManageStudent);
        mainBorderPane.setCenter(studentView);
        UIAnimator.playViewEntrance(studentView);
        setActiveButton(btnManageStudent, true);
        StudentController.refreshLive();
    }

    @FXML
    public void showAttendanceView(ActionEvent event) {
        UIAnimator.playTapPulse(btnAttendance);
        mainBorderPane.setCenter(attendanceView);
        UIAnimator.playViewEntrance(attendanceView);
        setActiveButton(btnAttendance, true);
        AttendanceController.refreshLive();
    }

    private void setActiveButton(Button activeBtn, boolean animateIndicator) {
        Button[] allButtons = { btnDashboard, btnManageStudent, btnManageTeacher, btnAttendance };
        for (Button btn : allButtons) {
            if (btn != null) {
                btn.getStyleClass().remove("sidebar-button-active");
                if (!btn.getStyleClass().contains("sidebar-button")) {
                    btn.getStyleClass().add("sidebar-button");
                }
            }
        }
        if (activeBtn != null) {
            activeBtn.getStyleClass().remove("sidebar-button");
            if (!activeBtn.getStyleClass().contains("sidebar-button-active")) {
                activeBtn.getStyleClass().add("sidebar-button-active");
            }
            UIAnimator.moveSidebarIndicator(sidebarIndicator, activeBtn, animateIndicator);
        }
    }
}