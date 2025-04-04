package org.esupportail.openconnect;

import com.kodedu.terminalfx.TerminalBuilder;
import com.kodedu.terminalfx.TerminalTab;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.TilePane;
import javafx.stage.Stage;
import org.esupportail.openconnect.ui.FileLocalStorage;
import org.esupportail.openconnect.ui.LogTextAreaService;
import org.esupportail.openconnect.ui.WebviewPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

@Component
public class OpenconnectJfxController implements Initializable {

	final static Logger log = LoggerFactory.getLogger(OpenconnectJfxController.class);

	@Resource
	FileLocalStorage fileLocalStorage;

	@FXML
	public SplitPane mainPane;

	@FXML
	public SplitPane mainPane2;

	@FXML
	MenuItem exit;

	@FXML
	MenuItem configurationVpnUrl;

	@FXML
	MenuItem configurationOpenconnectCommand;

	@FXML
	public TextArea logTextarea;

	@Resource
	WebviewPane webviewPane;

	@Resource
	LogTextAreaService logTextAreaService;

	@Resource
	OpenConnectTerminal openConnectTerminal;

	Stage stage;

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {

		logTextAreaService.initLogTextArea(logTextarea);

		logTextAreaService.appendText("Openconnect Cookie Webview ");

		exit.setOnAction(event -> {
			this.exit();
		});

		String vpnUrl = fileLocalStorage.getItem("vpnUrl");
		TextInputDialog td = new TextInputDialog(vpnUrl);
		td.setHeaderText("Url de votre VPN");
		td.setContentText("https://vpn.example.org");
		td.getDialogPane().setMinWidth(1000);
		td.setResizable(true);
		configurationVpnUrl.setOnAction(actionEvent -> {
			Optional<String> result = td.showAndWait();
			result.ifPresent(vpnUrlNew -> {
				new Thread(() -> {
					fileLocalStorage.setItem("vpnUrl", vpnUrlNew);
					webviewPane.reload();
				}).start();
			});
		});

		String openconnectCommand = fileLocalStorage.getItem("openconnectCommand");
		TextInputDialog td2 = new TextInputDialog(openconnectCommand);
		td2.setHeaderText("Commande Openconnect");
		td2.setContentText("su -c '/sbin/openconnect --protocol=nc %s -C %s'");
		td2.getDialogPane().setMinWidth(1000);
		td2.setResizable(true);
		configurationOpenconnectCommand.setOnAction(actionEvent -> {
			Optional<String> result = td2.showAndWait();
			result.ifPresent(openconnectCommandNew -> {
				new Thread(() -> {
					fileLocalStorage.setItem("openconnectCommand", openconnectCommandNew);
				}).start();
			});
		});

		webviewPane.init();
		mainPane.getItems().add(0, webviewPane);

		TabPane tabPane = new TabPane();
		tabPane.getTabs().add(openConnectTerminal.getTerminal());
		mainPane2.getItems().add(tabPane);
	}

	public void initializeFromFileLocalStorage(Stage stage) {
		this.stage = stage;
	}

	public void exit() {
		logTextAreaService.appendText("Arrêt demandé");
		stage.close();
		Platform.exit();
		System.exit(0);
	}
}
