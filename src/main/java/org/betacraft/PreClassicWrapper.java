package org.betacraft;

import java.awt.Image;
import java.lang.Thread.State;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

import org.betacraft.launcher.Logger;

public class PreClassicWrapper extends Wrapper {

	public PreClassicWrapper(String user, String ver_prefix, String version, String sessionid, String mainFolder,
			int height, int width, String launchMethod, String server, String mppass, String USR,
			String VER, Image img, ArrayList addons) {
		super(user, ver_prefix, version, sessionid, mainFolder, height, width, launchMethod, server, mppass, null, USR, VER,
				img, addons);
	}

	@Override
	public void loadMainClass(URL[] url) {
		try {
			classLoader = null;
			URL[] old = url.clone();
			URL[] neww = new URL[old.length];

			int i;
			for (i = 0; i < old.length; i++) {
				neww[i] = old[i];
			}

			classLoader = new URLClassLoader(neww);
		} catch (Exception ex) {
			ex.printStackTrace();
			Logger.printException(ex);
		}

		try {
			// rd-
			mainClass = classLoader.loadClass("com.mojang.rubydung.RubyDung");
		} catch (ClassNotFoundException ex) {
			try {
				// mc-
				mainClass = classLoader.loadClass("com.mojang.minecraft.RubyDung");
			} catch (ClassNotFoundException ex1) {
				ex1.printStackTrace();
				Logger.printException(ex1);
			}
		}
		try {
			for (Class<Addon> c : ogaddons) {
				this.loadAddon((Addon) c.newInstance());
				System.err.println("- " + c);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			Logger.printException(ex);
		}
		try {
			mainClassInstance = mainClass.newInstance();
			if (!this.addonsPreAppletInit(this.addons)) return;
			System.err.println(mainClassInstance.getClass().getName());
			Thread t = new Thread() {
				public void run() {
					((Runnable)mainClassInstance).run();
				}
			};
			t.start();
			if (!this.addonsPostAppletInit(this.addons)) return;
			this.stop();

			return;
		} catch (Exception ex) {
			ex.printStackTrace();
			Logger.printException(ex);
		}
	}

	@Override
	public void play() {
		try {
			this.loadJars();
		} catch (Exception ex) {
			ex.printStackTrace();
			Logger.printException(ex);
		}
	}
}
