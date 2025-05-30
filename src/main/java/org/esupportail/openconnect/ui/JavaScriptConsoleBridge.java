package org.esupportail.openconnect.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


@Component
public class JavaScriptConsoleBridge {

    @Resource
    LogTextAreaService logTextAreaService;
	
	private final static Logger log = LoggerFactory.getLogger(JavaScriptConsoleBridge.class);

	public void disconnect() {
		log.info("Javascript exit !");
		System.exit(0);
    }
	
	public void consoleerror(String text) {
        logTextAreaService.appendText("Console Javascript : " + text);
    }
	
    public void info(String text) {
        logTextAreaService.appendText("Info Javascript : " + text);
    }

    public void windowerror(String text) {
        logTextAreaService.appendText("Window Javascript : " + text);
    }
    
    public void warn(String text) {
        logTextAreaService.appendText("Warn Javascript : " + text);
    }
}
