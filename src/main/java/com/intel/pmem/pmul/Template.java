/* 
 * Copyright (C) 2022 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * 
 */

package com.intel.pmem.pmul;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

class Template {
	private final String source;
	private final String open;
	private final String close;
	private final Map<String, String> fields;

	public Template(String source, String delimiters) {
		this.fields = new HashMap<>();
		this.source = source;
		if (delimiters.length() == 0) {
			this.open = "";
			this.close = "";
		}
		else if (delimiters.length() == 2) {
			this.open = delimiters.substring(0, 1);
			this.close = delimiters.substring(1, 2);
		}
		else {
			throw new RuntimeException("delimiters must contain zero or 2 characters");
		}
	} 

	public void setField(String name, String value) {
		fields.put(open + name + close, value);
	}

	public <T> void setField(String name, Collection<T> items, String separator) {
		fields.put(open + name + close, listString(separator, "", items, null));
	}

	public String render() {
		String ans = source;
		for (Map.Entry<String, String> field : fields.entrySet()) {
			ans = ans.replaceAll(field.getKey(), field.getValue());
		}
		return ans;
	}

	public static <T> String listString(String delimiters, String emptyString, Collection<T> items, BiFunction<T, Integer, String> fn)
	{
		int len = delimiters.length();
		if (len != 1 && len != 3) throw new IllegalArgumentException("list delimiters must be 1 or 3 characters");
		if (items == null) throw new IllegalArgumentException("colleciton items must not be null");
		int nItems = items.size();
		if (nItems == 0) return emptyString;
		String open = len == 1 ? "" : delimiters.substring(0, 1);
		String sep = len == 1 ? delimiters : delimiters.substring(1,2);
		String close = len == 1 ? "" : delimiters.substring(2);
		StringBuilder buff = new StringBuilder();
		buff.append(open);
		int n = 0;
		for (T item : items)
		{
			buff.append(fn == null ? item.toString() : fn.apply(item, n));
			n++;
			if (n < nItems) buff.append(sep);
		}
		buff.append(close);
		return buff.toString();
	}
}
