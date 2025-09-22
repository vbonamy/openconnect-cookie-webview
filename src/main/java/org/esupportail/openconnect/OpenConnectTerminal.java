package org.esupportail.openconnect;

import com.kodedu.terminalfx.TerminalBuilder;
import com.kodedu.terminalfx.TerminalTab;
import javafx.beans.property.SimpleBooleanProperty;
import org.esupportail.openconnect.ui.FileLocalStorage;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class OpenConnectTerminal {

    FileLocalStorage fileLocalStorage = FileLocalStorage.getInstance();

    TerminalTab terminal;

    SimpleBooleanProperty isOpenConnectRunning = new SimpleBooleanProperty(false);

    @PostConstruct
    public void init() {
        TerminalBuilder terminalBuilder = new TerminalBuilder();
        terminal = terminalBuilder.newTerminal();
    }

    public TerminalTab getTerminal() {
        return terminal;
    }

    public void startOpenconnect(String vpnUrl, String dsidCookie) {
        String openconnectCommandConfiguration = fileLocalStorage.getItem("openconnectCommand");
        String openconnectCommand = String.format(openconnectCommandConfiguration + "\r", vpnUrl, dsidCookie);
        terminal.getTerminal().command(openconnectCommand);
    }

    public void stop() {
        terminal.getTerminal().command("exit\r");
    }
}