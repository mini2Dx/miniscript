/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2016 Thomas Cashman
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.mini2Dx.miniscript.ruby;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Ignore;
import org.mini2Dx.miniscript.core.AbstractGameScriptingEngineTest;
import org.mini2Dx.miniscript.core.GameScriptingEngine;

/**
 * UATs for {@link RubyGameScriptingEngine}
 */
public class RubyGameScriptingEngineTest extends AbstractGameScriptingEngineTest {

	@Override
	protected GameScriptingEngine createScriptingEngine() {
		return new RubyGameScriptingEngine();
	}

	@Override
	protected String getDefaultScript() {
		try {
			return new String(Files.readAllBytes(Paths.get(this.getClass().getResource("/default.rb").toURI())));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		Assert.fail("Could not read default script");
		return null;
	}

	@Override
	protected String getDefaultScriptFilepath() {
		return "default.rb";
	}

	@Override
	protected String getInvokeWithScript() {
		try {
			return new String(Files.readAllBytes(Paths.get(this.getClass().getResource("/invokeWithinScript.rb").toURI())));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		Assert.fail("Could not read invokeWithinScript script");
		return null;
	}

	@Override
	protected String getInvokeWithinScriptFilepath() {
		return "invokeWithinScript.rb";
	}

	@Override
	protected String getNestedInvokeWithScript() {
		try {
			return new String(Files.readAllBytes(Paths.get(this.getClass().getResource("/nestedInvokeWithinScript.rb").toURI())));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		Assert.fail("Could not read nestedInvokeWithinScript script");
		return null;
	}

	@Override
	protected String getNestedInvokeWithinScriptFilepath() {
		return "nestedInvokeWithinScript.rb";
	}

	@Override
	protected InputStream getDefaultScriptInputStream() {
		return RubyGameScriptingEngineTest.class.getResourceAsStream("/default.rb");
	}

	@Override
	protected String getWaitForCompletionScript() {
		try {
			return new String(Files.readAllBytes(Paths.get(this.getClass().getResource("/waitForCompletion.rb").toURI())));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		Assert.fail("Could not read waitForCompletion script");
		return null;
	}
}
