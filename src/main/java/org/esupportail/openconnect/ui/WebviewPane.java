package org.esupportail.openconnect.ui;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import org.apache.commons.lang3.StringUtils;
import org.esupportail.openconnect.OpenConnectTerminal;
import org.esupportail.openconnect.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.awt.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

@Component
public class WebviewPane extends StackPane {

    private final static Logger log = LoggerFactory.getLogger(WebviewPane.class);

    WebView webView;

    @Resource
    JavaScriptConsoleBridge javaScriptConsoleBridge;

    @Resource
    FileLocalStorage fileLocalStorage;

    @Resource
    LogTextAreaService logTextAreaService;

    @Resource
    OpenConnectTerminal openConnectTerminal;

    public void init() throws HeadlessException {

        CookieManager cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);

        webView = new WebView();
        webView.getEngine().setJavaScriptEnabled(true);

        String url = fileLocalStorage.getItem("vpnUrl");
        URI uri = null;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        final URI finalUri = uri;

        logTextAreaService.appendText("webView load : " + url);
        webView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            logTextAreaService.appendText("webView state : " + newValue);
            ArrayList<String> cookies = new ArrayList<>();
            if(Worker.State.SUCCEEDED.equals(newValue)) {
                List<HttpCookie> cookieList = cookieManager.getCookieStore().getCookies();
                if (cookieList != null && !cookieList.isEmpty()) {
                    for (HttpCookie cookie : cookieManager.getCookieStore().getCookies()) {
                        cookies.add("webView cookie : " + cookie.getName().concat("=").concat(cookie.getValue()));
                        if ("DSID".equals(cookie.getName())) {
                            String dsid = cookie.getValue();
                            logTextAreaService.appendText("DSID : " + dsid);
                            fileLocalStorage.setItem("dsid", dsid);
                            openConnectTerminal.startOpenconnect(url, dsid);
                        }
                    }
                } else {
                    logTextAreaService.appendText("webView cookie : no cookies with the CookieHandler from webview");
                    // verify java version : jdk must be 17
                    String javaVersion = System.getProperty("java.version");
                    if (!javaVersion.startsWith("17")) {
                        logTextAreaService.appendText("Java version is not 17, please use Java 17 !s");
                    }
                }
                logTextAreaService.appendText("cookies : " + StringUtils.join(cookies, "\n"));
            }
        });

        webView.getEngine().getLoadWorker().exceptionProperty().addListener((ov, t, t1) -> log.error(");Received exception: " + t1.getMessage(), t1));

        webView.getEngine().load(url);

        webView.getEngine().locationProperty().addListener(new ChangeListener<String>() {
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                log.info("webView load : " + newValue);
            }

        });
        StackPane webviewPane = new StackPane(webView);
        getChildren().add(webviewPane);
    }


    public void reload() {
        String url = fileLocalStorage.getItem("vpnUrl");
        logTextAreaService.appendText("webView reload : " + url);
        Utils.jfxRunLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                webView.getEngine().load(url);
            }
        });
    }
}
