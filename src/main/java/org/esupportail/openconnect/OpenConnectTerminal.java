package org.esupportail.openconnect;

import com.kodedu.terminalfx.TerminalBuilder;
import com.kodedu.terminalfx.TerminalTab;
import org.esupportail.openconnect.ui.FileLocalStorage;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Component
public class OpenConnectTerminal {

    @Resource
    FileLocalStorage fileLocalStorage;

    TerminalTab terminal;

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

}
