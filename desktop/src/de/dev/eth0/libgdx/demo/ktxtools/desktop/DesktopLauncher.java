package de.dev.eth0.libgdx.demo.ktxtools.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import de.dev.eth0.libgdx.demo.ktxtools.KtxToolsDemo;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		new LwjglApplication(new KtxToolsDemo(), config);
	}
}
