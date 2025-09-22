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

    public final static String DSID_COOKIE_NAME = "DSID";

    WebView webView;

    @Resource
    JavaScriptConsoleBridge javaScriptConsoleBridge;

    FileLocalStorage fileLocalStorage = FileLocalStorage.getInstance();

    @Resource
    LogTextAreaService logTextAreaService;

    @Resource
    OpenConnectTerminal openConnectTerminal;

    public void init() throws HeadlessException {

        CookieManager cookieManager = (CookieManager) CookieHandler.getDefault();

        webView = new WebView();
        webView.getEngine().setJavaScriptEnabled(true);

        webView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            logTextAreaService.appendText("webView state : " + newValue);
            ArrayList<String> cookies = new ArrayList<>();
            if(Worker.State.SUCCEEDED.equals(newValue)) {
                List<HttpCookie> cookieList = cookieManager.getCookieStore().getCookies();
                if (cookieList != null && !cookieList.isEmpty()) {
                    for (HttpCookie cookie : cookieList) {
                        cookies.add("webView cookie : " + cookie.getName().concat("=").concat(cookie.getValue()));
                        if (DSID_COOKIE_NAME.equalsIgnoreCase(cookie.getName())) {
                            String dsid = cookie.getValue();
                            logTextAreaService.appendText(DSID_COOKIE_NAME + " : " + dsid);
                            fileLocalStorage.setItem(DSID_COOKIE_NAME, dsid);
                            String vpnUrl = fileLocalStorage.getItem("vpnUrl");
                            openConnectTerminal.startOpenconnect(vpnUrl, dsid);
                        }
                    }
                } else {
                    logTextAreaService.appendText("webView cookie : no cookies with the CookieHandler from webview !?");
                }
                logTextAreaService.appendText("cookies : " + StringUtils.join(cookies, "\n"));
            }
        });

        webView.getEngine().getLoadWorker().exceptionProperty().addListener((ov, t, t1) -> log.error(");Received exception: " + t1.getMessage(), t1));

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

    public void freeze() {
        Utils.jfxRunLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                webView.getEngine().load("about:blank");
            }
        });
    }

    public void unfreeze() {
        reload();
    }
}
