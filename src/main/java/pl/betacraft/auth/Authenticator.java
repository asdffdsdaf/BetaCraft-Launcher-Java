package pl.betacraft.auth;

import javax.swing.SwingUtilities;

import org.betacraft.launcher.Lang;
import org.betacraft.launcher.Launcher;
import org.betacraft.launcher.Window;

public class Authenticator {

	//public boolean refreshToken();

	public boolean authenticate() {
		return false;
	}

	public boolean invalidate() {
		return false;
	}

	public Credentials getCredentials() {
		return null;
	}

	void authSuccess() {
		 Window.nick_input.setText(Launcher.getNickname());
		 Window.nick_input.setEnabled(false);
		 Window.loginButton.setText(Lang.LOGOUT_BUTTON);
	};
}
