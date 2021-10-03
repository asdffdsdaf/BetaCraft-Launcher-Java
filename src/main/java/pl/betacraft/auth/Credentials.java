package pl.betacraft.auth;

import java.io.File;
import java.io.FileOutputStream;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class Credentials {

	private static transient Gson gson = new Gson();
	private static transient Gson gsonPretty = new GsonBuilder().setPrettyPrinting().create();
	public String refresh_token;
	public String access_token;
	public String local_uuid;
	public String username;
	public String name;
	public AccountType account_type;

	public static Credentials[] load(File credentials) {
		try {
			String jsonContent = new String(FileUtils.readFileToString(credentials));
			return gson.fromJson(jsonContent, Credentials[].class);
		} catch (Throwable t) {
			t.printStackTrace();
			return null;
		}
	}

	public static boolean save(Credentials[] c, File cFile) {
		try {
			IOUtils.write(gsonPretty.toJson(c).getBytes("UTF-8"), new FileOutputStream(cFile.getPath()));
			return true;
		} catch (Throwable t) {
			t.printStackTrace();
			return false;
		}
	}

	public enum AccountType {
		MOJANG,
		MICROSOFT,
		OFFLINE,
		TWITCH,
		BETACRAFT;
	}
}
