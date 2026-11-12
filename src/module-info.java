module JavaFX {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.xml;
    requires java.desktop;
    requires java.sql;
    requires jbcrypt;
	requires javafx.base;
    
    opens application to javafx.graphics, javafx.fxml;
    exports application;
}