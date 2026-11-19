package application;

import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;

public class MainController {

    @FXML private BorderPane mainBorderPane;
    @FXML private Button btnDashboard;
    @FXML private Button btnManageTeacher;
    @FXML private Button btnManageStudent;
    @FXML private Button btnAttendance;

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
            setActiveButton(btnDashboard);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void showDashboardView(ActionEvent event) {
        mainBorderPane.setCenter(dashboardView);
        setActiveButton(btnDashboard);
        if (dashboardController != null) {
            DashboardController.refreshDashboard();
        }
    }

    @FXML
    public void showTeacherView(ActionEvent event) {
        mainBorderPane.setCenter(teacherView);
        setActiveButton(btnManageTeacher);
        TeacherController.refreshLive();
    }

    @FXML
    public void showStudentView(ActionEvent event) {
        mainBorderPane.setCenter(studentView);
        setActiveButton(btnManageStudent);
        StudentController.refreshLive();
    }

    @FXML
    public void showAttendanceView(ActionEvent event) {
        mainBorderPane.setCenter(attendanceView);
        setActiveButton(btnAttendance);
        AttendanceController.refreshLive();
    }

    private void setActiveButton(Button activeBtn) {
        if (btnDashboard != null) {
            btnDashboard.getStyleClass().remove("sidebar-button-active");
            if (!btnDashboard.getStyleClass().contains("sidebar-button")) {
                btnDashboard.getStyleClass().add("sidebar-button");
            }
        }
        if (btnManageTeacher != null) {
            btnManageTeacher.getStyleClass().remove("sidebar-button-active");
            if (!btnManageTeacher.getStyleClass().contains("sidebar-button")) {
                btnManageTeacher.getStyleClass().add("sidebar-button");
            }
        }
        if (btnManageStudent != null) {
            btnManageStudent.getStyleClass().remove("sidebar-button-active");
            if (!btnManageStudent.getStyleClass().contains("sidebar-button")) {
                btnManageStudent.getStyleClass().add("sidebar-button");
            }
        }
        if (btnAttendance != null) {
            btnAttendance.getStyleClass().remove("sidebar-button-active");
            if (!btnAttendance.getStyleClass().contains("sidebar-button")) {
                btnAttendance.getStyleClass().add("sidebar-button");
            }
        }
        if (activeBtn != null) {
            activeBtn.getStyleClass().remove("sidebar-button");
            if (!activeBtn.getStyleClass().contains("sidebar-button-active")) {
                activeBtn.getStyleClass().add("sidebar-button-active");
            }
        }
    }
}