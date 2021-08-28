/**
 * Copyright 2021 Viridian Software Ltd.
 */
package org.mini2Dx.miniscript.core.util;

import java.util.Set;
import java.util.function.Consumer;

public class ToSetConsumer<T> implements Consumer<T> {
	private Set<T> set;

	@Override
	public void accept(T t) {
		set.add(t);
	}

	public void setSet(Set<T> list) {
		this.set = list;
	}
}
