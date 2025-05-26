package org.esupportail.openconnect;

import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.SystemTray;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.esupportail.openconnect.ui.FileLocalStorageCookieStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URL;

@ComponentScan
public class OpenconnectApplication extends Application {

	private final static Logger log = LoggerFactory.getLogger(OpenconnectApplication.class);

	public void start(final Stage primaryStage) throws IOException {

		long start = System.currentTimeMillis();

		// Set the default cookie manager to handle cookies
		// that must called before any http request so that HttpClient use it
		FileLocalStorageCookieStore cookieStore = new FileLocalStorageCookieStore();
		CookieManager cookieManager = new CookieManager(cookieStore, null);
		CookieHandler.setDefault(cookieManager);
		log.info("cookiemanger is ok");

		primaryStage.setTitle("Openconnect Cookie Webview");

		ApplicationContext context = new AnnotationConfigApplicationContext(OpenconnectApplication.class);
		URL fxmlUrl = this.getClass().getClassLoader().getResource("openconnect-cookie-webview.fxml");
		FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl);
		fxmlLoader.setControllerFactory(cls -> context.getBean(cls));
		VBox root = fxmlLoader.load();
		Scene scene = new Scene(root);
		primaryStage.setScene(scene);

		OpenconnectJfxController openconnectJfxController = fxmlLoader.getController();
		openconnectJfxController.initializeFromFileLocalStorage(primaryStage);

		primaryStage.show();

		boolean systemTrayEnabled = false;
		// call setupSystemTray in thread and wait for it to finish
		// timeout after 2 seconds
		Thread trayThread = new Thread(() -> {
			try {
				setupSystemTray(primaryStage);
			} catch (Exception e) {
				log.error("Error setting up system tray", e);
			}
		});
		trayThread.start();
		try {
			trayThread.join(2000);
			systemTrayEnabled = true;
			log.info("system tray enabled");
		} catch (InterruptedException e) {
			log.error("Error waiting for system tray setup", e);
		}
		Boolean finalSystemTrayEnabled = Boolean.valueOf(systemTrayEnabled);
		if(finalSystemTrayEnabled) {
			setupDummyStage();
		}
		primaryStage.setOnCloseRequest(event -> {
			if(finalSystemTrayEnabled) {
				event.consume();
				Platform.runLater(() -> {
					primaryStage.hide();
				});
			}
		});

		openconnectJfxController.logTextAreaService.appendText(String.format("Application initialized in %.2f seconds", (System.currentTimeMillis()-start)/1000.0));
	}

	/*
	 * HACK : dummy stage to keep JavaFX alive
	 */
	private void setupDummyStage() {
		Stage dummyStage = new Stage();
		dummyStage.initStyle(StageStyle.UTILITY);
		dummyStage.setOpacity(0);
		dummyStage.setWidth(1);
		dummyStage.setHeight(1);
		dummyStage.setIconified(true);
		dummyStage.setTitle("hidden-keepalive");
		dummyStage.show();
	}

	void setupSystemTray(Stage primaryStage) {
		SystemTray tray = SystemTray.get();
		tray.setImage(getClass().getResource("/icon-openconnect-cookie-webview.png"));
		tray.getMenu().add(new MenuItem("Display it", e -> {
			System.out.println("Clicked display");
			Platform.runLater(() -> {
				System.out.println("Platform.runLater triggered");
				if (primaryStage != null) {
					primaryStage.show();
					primaryStage.toFront();
				}
			});
		}));
		tray.getMenu().add(new MenuItem("Exit", e -> {
			tray.shutdown();
			Platform.exit();
		}));
	}

}
