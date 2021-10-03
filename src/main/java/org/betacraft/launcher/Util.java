package org.betacraft.launcher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import pl.betacraft.auth.*;
import pl.betacraft.auth.Credentials.AccountType;
import pl.betacraft.json.lib.MouseFixMacOSJson;

import java.io.*;
import java.net.URI;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Util {
	public static final Gson gson = new Gson();
	public static final Gson gsonPretty = new GsonBuilder().setPrettyPrinting().create();
	public static final int jsonVersion = 1;
	public static final File accountsFile = new File(BC.get() + "launcher/accounts.json");
	public static final File cert_file = new File(BC.get() + "launcher/ssl_cert.crt");
	public static final File old_lastlogin = new File(BC.get() + "lastlogin");

	private static void setupAccountConfiguration() {
		try {
			Launcher.auth = new NoAuth("");
			Accounts accs = new Accounts();
			accs.current = Launcher.auth.getCredentials().local_uuid;
			ArrayList<Credentials> list = new ArrayList();
			list.add(Launcher.auth.getCredentials());
			accs.accounts = list;
			Launcher.accounts = accs;
			saveAccounts();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public static void saveAccounts() {
		try {
			if (!accountsFile.exists()) {
				accountsFile.createNewFile();
			}
			IOUtils.write(gsonPretty.toJson(Launcher.accounts).getBytes("UTF-8"), new FileOutputStream(accountsFile.getPath()));
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public static Authenticator getAuthenticator(Credentials c) {
		if (c.account_type == AccountType.MICROSOFT) {
			return new MicrosoftAuth(c);
		} else if (c.account_type == AccountType.MOJANG) {
			return new MojangAuth(c);
		} else if (c.account_type == AccountType.OFFLINE) {
			return new NoAuth(c);
		}
		return null;
	}

	public static void readAccounts() {
		try {
			if (!accountsFile.exists()) {
				setupAccountConfiguration();
				return;
			}
			Accounts accs = null;
			try {
				accs = gson.fromJson(new String(FileUtils.readFileToString(accountsFile)), Accounts.class);
			} catch (Throwable t) {}

			if (accs == null || accs.accounts == null) {
				setupAccountConfiguration();
				return;
			}

			for (Credentials c: accs.accounts) {
				if (c.local_uuid != null && c.local_uuid.equals(accs.current)) {
					Launcher.auth = getAuthenticator(c);
				}
			}
			if (Launcher.auth == null) {
				Launcher.auth = getAuthenticator(accs.accounts.get(0));
				accs.current = Launcher.auth.getCredentials().local_uuid;
			}
			Launcher.accounts = accs;
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public static void write(File file, String[] lines, boolean append) {
		write(file, lines, append, "UTF-8");
	}

	public static void write(File file, String[] lines, boolean append, String charset) {
		try {
			// Create new file, if it doesn't already exist
			file.createNewFile();
		} catch (IOException e) {
			System.out.println(file.getPath());
			e.printStackTrace();
			Logger.printException(e);
		}
		OutputStreamWriter writer = null;
		try {
			// Write in UTF-8
			writer = new OutputStreamWriter(
					new FileOutputStream(file, append), charset);
			for (int i = 0; i < lines.length; i++) {
				// Skip empty lines
				if (lines[i] != null) {
					writer.write(lines[i] + "\n");
				}
			}
		} catch (Exception ex) {
			Logger.a("A critical error occurred while attempting to write to file: " + file);
			ex.printStackTrace();
			Logger.printException(ex);
		} finally {
			// Close the file
			try {writer.close();} catch (Exception ex) {}
		}
	}

	public static void setProperty(File file, String property, String value) {
		setProperty(file, property, value, "UTF-8");
	}

	public static void setProperty(File file, String property, String value, String charset) {
		// Read the lines
		String[] lines = read(file, charset);
		String[] newlines = new String[lines.length + 1];

		// Try to find the property wanted to be set
		boolean found = false;
		for (int i = 0; i < lines.length; i++) {
			if (lines[i] == null) continue;
			if (lines[i].startsWith(property + ":")) {
				// The wanted property has been found, so we're going to replace its value
				newlines[i] = property + ":" + value;
				found = true;
				continue;
			}
			// The property didn't match, just take this line further
			newlines[i] = lines[i];
		}

		if (!found) {
			// There was no wanted property in the file, so we're going to append it to the file 
			write(file, new String[] {property + ":" + value}, true, charset);
			return;
		}

		// Write to file, without appending
		write(file, newlines, false, charset);
	}

	public static String getProperty(File file, String property) {
		return getProperty(file, property, "UTF-8");
	}

	public static String getProperty(File file, String property, String charset) {
		String[] lines = read(file, charset);
		String value = null;
		for (int i = 0; i < lines.length; i++) {
			// If the array is empty, ignore it
			if (lines[i] == null) continue;

			// Check if the property matches
			if (lines[i].startsWith(property + ":")) {
				value = lines[i].substring(property.length()+1, lines[i].length());
				break;
			}
		}
		return value;
	}

	public static boolean hasProperty(File file, String property, String charset) {
		String[] lines = read(file, charset);
		for (int i = 0; i < lines.length; i++) {
			// If the line is empty, ignore it
			if (lines[i] == null) continue;

			// Check if the property matches
			if (lines[i].startsWith(property + ":")) {
				return true;
			}
		}
		return false;
	}

	public static String[] excludeExistant(File file, String[] properties, String charset) {
		String[] lines = read(file, charset);
		for (int i = 0; i < lines.length; i++) {
			// If the line is empty, ignore it
			if (lines[i] == null) continue;

			for (int i1 = 0; i1 < properties.length; i1++) {
				// If the property matches, remove it from array
				if (lines[i].startsWith(properties[i1] + ":")) {
					properties[i1] = null;
				}
			}
		}
		return properties;
	}

	public static Thread unzip(File source, File dest_folder, boolean delete) {
		return unzip(source.getPath(), dest_folder.getPath(), delete);
	}

	public static Thread unzip(final String source, final String dest_folder, final boolean delete) {
		Thread unrarthread = new Thread() {
			public void run() {
				FileInputStream fis;
				byte[] buffer = new byte[1024];
				try {
					fis = new FileInputStream(source);
					ZipInputStream zis = new ZipInputStream(fis);
					ZipEntry entry = zis.getNextEntry();
					while (entry != null) {
						if (entry.isDirectory()) {
							entry = zis.getNextEntry();
							continue;
						}
						String fileName = entry.getName();
						File newFile = new File(dest_folder + File.separator + fileName);

						new File(newFile.getParent()).mkdirs();
						FileOutputStream fos = new FileOutputStream(newFile);
						int length;
						while ((length = zis.read(buffer)) > 0) {
							fos.write(buffer, 0, length);
						}

						fos.close();
						zis.closeEntry();
						entry = zis.getNextEntry();
					}
					zis.closeEntry();
					zis.close();
					fis.close();
				} catch (Exception ex) {
					ex.printStackTrace();
					Logger.printException(ex);
				}
				if (delete) new File(source).delete();
				if (!Util.isStandalone()) Launcher.totalThreads.remove(this);
			}
		};
		unrarthread.start();
		return unrarthread;
	}

	public static String[] read(File file, String charset) {
		try {
			if (!file.exists()) return new String[] {};
			// Create new, if doesn't exist
			if (file.createNewFile()) {
				Logger.a("Created a new file: " + file);
			}
		} catch (IOException e) {
			System.out.println(file.getPath());
			e.printStackTrace();
			Logger.printException(e);
		}
		InputStreamReader reader = null;
		try {
			// Read in UTF-8
			reader = new InputStreamReader(
					new FileInputStream(file), charset);
			StringBuilder inputB = new StringBuilder();
			char[] buffer = new char[1024];
			while (true) {
				int readcount = reader.read(buffer);
				if (readcount < 0) break;
				inputB.append(buffer, 0, readcount);
			}
			return inputB.toString().split("\n");
		} catch (Exception ex) {
			Logger.a("A critical error occurred while reading from file: " + file);
			ex.printStackTrace();
			Logger.printException(ex);
		} finally {
			// Close the file
			try {reader.close();} catch (Exception ex) {}
		}
		return null;
	}

	public static boolean isStandalone() {
		try {
			Launcher.VERSION.length();
			return false;
		} catch (Throwable ex) {
			return true;
		}
	}

	public static String getSHA1(File file) {
		try {
			InputStream fis =  new FileInputStream(file);

			byte[] buffer = new byte[1024];
			MessageDigest msgdig = MessageDigest.getInstance("SHA-1");
			int numRead;

			do {
				numRead = fis.read(buffer);
				if (numRead > 0) {
					msgdig.update(buffer, 0, numRead);
				}
			} while (numRead != -1);

			fis.close();
			byte[] digest = msgdig.digest();
			String str_result = "";
			for (int i = 0; i < digest.length; i++) {
				str_result += Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1);
			}
			return str_result;
		} catch (Throwable t) {
			t.printStackTrace();
			return null;
		}
	}

	public static boolean installMacOSFix(MouseFixMacOSJson json, boolean force) {
		File javaagent = new File(BC.get() + "launcher/macos-javaagent.jar");
		File lwjgl = new File(BC.get() + "launcher/macos-mousefix-lwjgl.jar");

		File classes_folder = new File(BC.get() + "launcher/macos-java-mod/");
		File classes_temp_zip = new File(BC.get() + "launcher/macos-mousefix.zip");

		String local_javaagent_sha1 = javaagent.exists() ? getSHA1(javaagent) : null;
		String local_lwjgl_sha1 = lwjgl.exists() ? getSHA1(lwjgl): null;
		String local_javamod_sha1 = Util.getProperty(BC.SETTINGS, "macosMouseFixClassesVersion");

		try {
			if (local_javaagent_sha1 == null || !local_javaagent_sha1.equals(json.agent_sha1) || force) {
				DownloadResponse agent_req = new DownloadRequest(json.agent_url, javaagent.getPath(), json.agent_sha1, false).perform();
				if (agent_req.result != DownloadResult.OK) {
					Logger.a("Failed to download macos javaagent");
					return false;
				}
			}
			if (local_lwjgl_sha1 == null || !local_lwjgl_sha1.equals(json.lwjgl_sha1) || force) {
				DownloadResponse lwjgl_req = new DownloadRequest(json.lwjgl_url, lwjgl.getPath(), json.lwjgl_sha1, false).perform();
				if (lwjgl_req.result != DownloadResult.OK) {
					Logger.a("Failed to download macos-mousefix.zip");
					return false;
				}
			}
			if (local_javamod_sha1 == null || !local_javamod_sha1.equals(json.classes_sha1) || force) {
				DownloadResponse classes_req = new DownloadRequest(json.classes_url, classes_temp_zip.getPath(), json.classes_sha1, false).perform();
				if (classes_req.result != DownloadResult.OK) {
					Logger.a("Failed to download macos-mousefix.zip");
					return false;
				} else {
					if (classes_folder.exists() && classes_folder.list().length != 0) {
						Launcher.removeRecursively(classes_folder, false, false);
					}
					classes_folder.mkdirs();
					Util.setProperty(BC.SETTINGS, "macosMouseFixClassesVersion", json.classes_sha1);
					Launcher.totalThreads.add(unzip(classes_temp_zip, classes_folder, true));
				}
			}
			return true;
		} catch (Throwable t) {
			t.printStackTrace();
			return false;
		}
	}

	public static String getFullJavaVersion() {
		String line = null;
		try {
			ArrayList<String> arl = new ArrayList();
			arl.add(Launcher.javaRuntime.getPath());
			arl.add("-version");
			ProcessBuilder pb = new ProcessBuilder(arl);
			Process p = pb.start();
			InputStreamReader isr_log = new InputStreamReader(p.getErrorStream());
			BufferedReader br_log = new BufferedReader(isr_log);
			while ((line = br_log.readLine()) != null) {
				if (line.contains("version")) {
					p.destroy();
					break;
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
		// java version "1.8.0_281" ---> 1.8.0_281"
		String verstart = line.substring(line.indexOf("\"")+1);
		// 1.8.0_281" ---> 1.8.0_281
		String fullver = verstart.substring(0, verstart.indexOf("\""));

		return fullver;
	}

	public static String getJavaVersion() {
		String fullver = getFullJavaVersion();

		// This basically filters out the actual core version, skipping all the fancy stuff
		String whitelist = "0123456789.";
		for (int i = 0; i < fullver.length(); i++) {
			if (!whitelist.contains(Character.toString(fullver.charAt(i)))) {
				fullver = fullver.substring(0, i);
			}
		}
		return fullver;
	}

	public static int getMajorJavaVersion() {
		String ver = getJavaVersion();
		if (ver.startsWith("1.")) {
			return Integer.parseInt(ver.split("\\.")[1]);
		} else {
			int cut = ver.indexOf('.'); 
			if (cut == -1) {
				cut = ver.length();
			}

			return Integer.parseInt(ver.substring(0, cut));
		}
	}

	public static void openURL(final URI uri) {
		try {
			final Object invoke = Class.forName("java.awt.Desktop").getMethod("getDesktop", (Class<?>[])new Class[0]).invoke(null, new Object[0]);
			invoke.getClass().getMethod("browse", URI.class).invoke(invoke, uri);
		}
		catch (Throwable t) {
			System.out.println("Failed to open link in a web browser: " + uri.toString());
		}
	}
}
