package com.talan.creditplatform.controller.ui;

import com.talan.creditplatform.view.client.BackendClient;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Label statusLabel;

    @FXML
    public void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isBlank() || password.isBlank()) {
            statusLabel.setText("Please enter username and password.");
            return;
        }

        statusLabel.setText("Authenticating...");
        loginButton.setDisable(true);

        // Run in background if possible, but keep simple for now
        boolean success = BackendClient.login(username, password);
        
        if (success) {
            statusLabel.setStyle("-fx-text-fill: green;");
            statusLabel.setText("Success! Role: " + BackendClient.currentRole);
            // transition logic would go here
        } else {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Invalid credentials.");
        }
        
        loginButton.setDisable(false);
    }
}
