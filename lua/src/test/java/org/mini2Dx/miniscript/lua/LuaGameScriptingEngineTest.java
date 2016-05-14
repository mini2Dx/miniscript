/**
 * Copyright 2016 Thomas Cashman
 */
package org.mini2Dx.miniscript.lua;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Assert;
import org.mini2Dx.miniscript.core.AbstractGameScriptingEngineTest;
import org.mini2Dx.miniscript.core.GameScriptingEngine;

/**
 *
 */
public class LuaGameScriptingEngineTest extends AbstractGameScriptingEngineTest {

	@Override
	protected GameScriptingEngine createScriptingEngine() {
		return new LuaGameScriptingEngine();
	}

	@Override
	protected String getDefaultScript() {
		try {
			return new String(Files.readAllBytes(Paths.get(this.getClass().getResource("/default.lua").toURI())));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		Assert.fail("Could not read default script");
		return null;
	}
	
	@Override
	protected InputStream getDefaultScriptInputStream() {
		return LuaGameScriptingEngineTest.class.getResourceAsStream("/default.lua");
	}

	@Override
	protected String getWaitForCompletionScript() {
		try {
			return new String(Files.readAllBytes(Paths.get(this.getClass().getResource("/waitForCompletion.lua").toURI())));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		Assert.fail("Could not read waitForCompletion script");
		return null;
	}
}
