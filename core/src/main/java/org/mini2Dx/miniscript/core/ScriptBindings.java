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
package org.mini2Dx.miniscript.core;

import java.util.*;

/**
 * A mapping of variable names to objects passed to a script
 */
public class ScriptBindings implements Map<String, Object> {
	public static final String SCRIPT_ID_VAR = "scriptId";
	public static final String SCRIPT_PARENT_ID_VAR = "scriptParentId";
	public static final String SCRIPT_INVOKE_VAR = "scripts";

	private final Map<String, Object> bindings = Collections.synchronizedMap(new HashMap<String, Object>());

	/**
	 * Creates a duplicate instance of this {@link ScriptBindings}
	 * 
	 * @return A new {@link ScriptBindings} instance containing all the same
	 *         bindings as this instance
	 */
	public ScriptBindings duplicate() {
		ScriptBindings result = new ScriptBindings();
		result.putAll(bindings);
		return result;
	}

	@Override
	public int size() {
		return bindings.size();
	}

	@Override
	public boolean isEmpty() {
		return bindings.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return bindings.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return bindings.containsValue(value);
	}

	@Override
	public Object get(Object key) {
		return bindings.get(key);
	}

	@Override
	public Object put(String key, Object value) {
		return bindings.put(key, value);
	}

	@Override
	public Object remove(Object key) {
		return bindings.remove(key);
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> m) {
		bindings.putAll(m);
	}

	@Override
	public void clear() {
		bindings.clear();
	}

	@Override
	public Set<String> keySet() {
		return bindings.keySet();
	}

	@Override
	public Collection<Object> values() {
		return bindings.values();
	}

	@Override
	public Set<java.util.Map.Entry<String, Object>> entrySet() {
		return bindings.entrySet();
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder("ScriptBindings [");
		for (String key : bindings.keySet()) {
			result.append(key);
			result.append("=");
			result.append(bindings.get(key));
			result.append(", ");
		}
		result.delete(result.length() - 2, result.length());
		result.append("]");
		return result.toString();
	}
}
