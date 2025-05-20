package org.esupportail.openconnect.ui;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class FileLocalStorage {

	private final static Logger log = LoggerFactory.getLogger(FileLocalStorage.class);

	private File file;

	static FileLocalStorage singleton;

	public static FileLocalStorage getInstance() {
		if (singleton == null) {
			singleton = new FileLocalStorage();
		}
		return singleton;
	}

	FileLocalStorage() {
		String localStorageName = "openconnect-cookie-webview.storage";
		Properties prop = new Properties();
		Resource resource = new ClassPathResource("openconnect-cookie-webview.properties");
		try {
			prop.load(resource.getInputStream());
			log.info("load props");
		} catch (IOException e1) {
			log.error("props not found");
		}
		String localStorageDir = System.getProperty("localStorageDir", prop.getProperty("localStorageDir"));
		String OS = System.getProperty("os.name").toLowerCase();
		if (OS.indexOf("win") >= 0) {
			File directory = new File(String.valueOf(System.getProperty("user.home")+ localStorageDir));
			if(!directory.exists()){
				directory.mkdir();
			}
			file = new File(System.getProperty("user.home")+ localStorageDir + localStorageName);
		} else {
			file = new File(localStorageName);
		}
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(StringUtils.isBlank(getItem("vpnUrl"))) {
			setItem("vpnUrl", System.getProperty("vpnUrl", prop.getProperty("vpnUrl")));
		}
		if(StringUtils.isBlank(getItem("openconnectCommand"))) {
			setItem("openconnectCommand", System.getProperty("openconnectCommand", prop.getProperty("openconnectCommand")));
		}
	}

	public String getItem(String key) {
		return getItem(key, String.class);
	}

	public <T> T getItem(String key, Class<T> clazz) {
		Object value = "";
		try {
			FileInputStream fis = new FileInputStream(file);
			ObjectInputStream ois = new ObjectInputStream(fis);
			HashMap<String, String> item = (HashMap<String, String>) ois.readObject();
			ois.close();
			fis.close();
			value = item.get(key);
			log.debug("get key : " + key + " value : " + value);
		} catch (EOFException e) {
			log.warn("error on read localstorage");
		} catch (Exception e) {
			log.error("error on read/create localstorage", e);
		}
		if (clazz != null && clazz.isInstance(value)) {
			return clazz.cast(value);
		}
		return null;
	}

	public synchronized <T> void setItem(String key, T value) {
		log.trace("init write : " + key);
		Map<String, Object> item = new HashMap<String, Object>();
		try {
			FileInputStream fis = new FileInputStream(file);
			try {
				ObjectInputStream ois = new ObjectInputStream(fis);
				item = (HashMap<String, Object>) ois.readObject();
				ois.close();
			} catch (Exception e) {
				log.warn("error on read localstorage");
			}
			fis.close();
			FileOutputStream fos = new FileOutputStream(file);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			item.put(key, value);
			oos.writeObject(item);
			oos.flush();
			oos.close();
			fos.close();
			log.debug(key + "=" + value + " write to localstorage");
		} catch (IOException e) {
			log.error("error on write to localstorage", e);
		}
	}

	public void removeItem(String key) {
		log.info("remove : " + key);
		Map<String, String> item = new HashMap<String, String>();
		try {
			try {
				FileInputStream fis = new FileInputStream(file);
				ObjectInputStream ois = new ObjectInputStream(fis);
				item = (HashMap<String, String>) ois.readObject();
				ois.close();
				fis.close();
				item.remove(key);
			} catch (ClassNotFoundException e) {
				log.warn("error on read localstorage");
			}
			FileOutputStream fos = new FileOutputStream(file);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(item);
			oos.flush();
			oos.close();
			fos.close();
		} catch (IOException e) {
			log.error("error on remove localstorage", e);
		}
	}

	public void clear() {
		file.delete();
	}

}
