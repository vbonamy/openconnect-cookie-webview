package org.esupportail.openconnect;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.io.IOException;
import java.net.URL;

@ComponentScan
public class OpenconnectApplication extends Application {

	private final static Logger log = LoggerFactory.getLogger(OpenconnectApplication.class);

	public void start(final Stage primaryStage) throws IOException {

		long start = System.currentTimeMillis();

		ApplicationContext context = new AnnotationConfigApplicationContext(OpenconnectApplication.class);
		
		primaryStage.setTitle("Openconnect Cookie Webview");

		URL fxmlUrl = this.getClass().getClassLoader().getResource("openconnect-cookie-webview.fxml");
		FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl);
		fxmlLoader.setControllerFactory(cls -> context.getBean(cls));
		VBox root = fxmlLoader.load();
		Scene scene = new Scene(root);
		primaryStage.setScene(scene);

		OpenconnectJfxController OpenconnectJfxController = fxmlLoader.getController();
		OpenconnectJfxController.initializeFromFileLocalStorage(primaryStage);

		//primaryStage.setMaximized(true);
		primaryStage.show();

		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent e) {
				OpenconnectJfxController.exit();
			}
		});

		OpenconnectJfxController.logTextAreaService.appendText(String.format("Application initialized in %.2f seconds", (System.currentTimeMillis()-start)/1000.0));
	}

}
