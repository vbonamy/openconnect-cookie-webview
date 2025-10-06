# OpenConnect Cookie WebView

**OpenConnect Cookie WebView** is a Java-based client that allows launching OpenConnect using a DSID cookie retrieved from a web browser session after user authentication.

This approach is particularly useful to bypass two-factor authentication (2FA) or one-time password (OTP) challenges typically required during VPN authentication via a browser.

The DSID cookie, obtained after MFA login, is a session cookie used by the VPN service. Once retrieved, it is sufficient to authenticate the user for VPN access.

## Installation (Linux/Debian)
To use OpenConnect Cookie WebView on a Debian-based Linux system, you need to install the following dependencies:

Install maven, openconnect, *JDK 25* (or greater) , openjfx :

```
apt install maven openconnect
```

To simplify JavaFX installation, install a Zulu JDK with JavaFX included (version 25 or greater). You can find the Zulu JDK-FX on the [Azul website](https://www.azul.com/downloads/?package=jdk-fx#zulu).

## Build and Run from Source

Clone the project :

```
git clone https://github.com/vbonamy/openconnect-cookie-webview.git
cd openconnect-cookie-webview
```

Next run it as a standard Java application with proper JavaFX module options :

```bash
mvn clean package
java --module-path /usr/share/openjfx/lib/ --add-modules javafx.controls,javafx.fxml,javafx.base,javafx.media,javafx.graphics,javafx.swing,javafx.web -jar target/openconnect-cookie-webview-1.4.jar
```

If uou tak a Zulu JDK-FX or JRE-FX, you can skip the `--module-path` and `--add-modules` options.
 :
```bash
java -jar target/openconnect-cookie-webview-1.4.jar
```

## Usage

In the main menu, modify configurations if necessary. You can set the following parameters:
- **VPN URL**: The URL of your VPN service.
- **OpenConnect command**: The command template for OpenConnect. 

Authenticate through the integrated WebView. This will open your institution’s login page in an embedded browser.

Once logged in (via 2FA/OTP if required), the application will extract the DSID cookie from the session.

It will then use this cookie to authenticate and initiate a VPN connection using OpenConnect.


## Technologies Used

This application is built with **Java**, **Spring**, and **JavaFX**, and uses **Maven** for building and dependency management.

- Requires **OpenJDK 25** or newer.
- Requires **OpenJFX**.
- Designed to run on **Linux** systems.

## Development Environment

You can use **Eclipse** (e.g., via Spring Tools) or **IntelliJ IDEA** for development.

Run the application as a standard Java application with the JavaFX module options specified.

For example:

```bash
--module-path /usr/share/openjfx/lib/ --add-modules javafx.controls,javafx.fxml,javafx.base,javafx.media,javafx.graphics,javafx.swing,javafx.web
```

Make sure to adjust the path to match your local JavaFX installation.

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! If you have suggestions for improvements or new features, feel free to open an issue or submit a pull request.

## Screenshots

![Configuration](src/etc/openconnect-mfa-0.png)

![Authentication with MFA, login/password page](src/etc/openconnect-mfa-1.png)

![Authentication with MFA, One-Time Password (OTP) page](src/etc/openconnect-mfa-2.png)

![Authentication, DSID cookie extraction and OpenConnect command execution](src/etc/openconnect-mfa-3.png)