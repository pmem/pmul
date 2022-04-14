/* 
 * Copyright (C) 2022 Intel Corporation
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * 
 */

package com.intel.pmem.pmul;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import jdk.incubator.foreign.MemoryLayout.PathElement;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;

class Generator {
	private static int uid = 0;

	static AccessorImpl accessorOf(MemoryLayout layout, PathElement... elements) {
		AccessorImpl ans = null;
		try {
			// generate Accessor source
			String pkg = "com.intel.pmem.pmul";
			Template classTemplate = new Template(classTemplateSource, "<>");
            //TODO varhandle generated twice
			String carrierStr = classTail(layout.varHandle(elements).varType());
			String className = "Accessor" + uid++;
			AccessorImpl.Info info = new AccessorImpl.Info(layout, elements);
			AccessorImpl.addInfo(className, info);
			classTemplate.setField("pkg", pkg);
			classTemplate.setField("classname", className);
			classTemplate.setField("carrier", carrierStr);
			String[] types = new String[] {"boolean", "byte", "short", "int", "long", "float", "double", "char", "MemoryAddress"};
			for (int i = 0; i < types.length; i++) {
				String type = types[i];
				Template setTemplate = new Template(setTemplateSource, "<>");
				setTemplate.setField("typelower", type);
				String setSource = setTemplate.render();
				classTemplate.setField("set" + (i + 1), setSource);
			}
			String src = classTemplate.render(); 
			// System.out.println("rendered source = \n" + src);
			// System.out.println(System.getProperty("jdk.module.path"));
			// System.out.println(System.getProperty("java.class.path"));

			// compile generated source
			JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
			StringSource ss = new StringSource(pkg + "." + className, src);
			ClassLoader loader = AccessorImpl.class.getClassLoader();
			URL url = loader.getResource("com/intel/pmem/pmul/AccessorImpl.class");
			String pathToPmemClasses = url.getPath().substring(0,url.getPath().lastIndexOf("/com"));
			Iterable<? extends JavaFileObject> fobjs = Arrays.asList(ss);
			List<String> options = Arrays.asList("-d", pathToPmemClasses, "--add-modules", "jdk.incubator.foreign");
			JavaCompiler.CompilationTask task = javac.getTask(null, null, null, options, null, fobjs);
			boolean result = task.call();
			assert result;
			// load class and instantiate Accessor
			String toDelete = pathToPmemClasses + "/" + pkg.replace('.', '/') + "/" + className + ".class";
			String toLoad = pkg + "." + className;
			@SuppressWarnings("unchecked") Class<AccessorImpl> cls = (Class<AccessorImpl>)Class.forName(toLoad, true, loader);
			new File(toDelete).delete();
			Constructor<AccessorImpl> ctor = cls.getConstructor();
			ans = ctor.newInstance();						
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(-1);
		}
		return ans;
	}

	private static final String classTail(Class<?> cls) {
		String name = cls.getCanonicalName();
		int p = name.lastIndexOf(".");
		return p == -1 ? name : name.substring(p + 1);
	};

	static class StringSource extends SimpleJavaFileObject {
		private String source;

		public StringSource(String name, String source) {
			super(URI.create("string:///" + name.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension), JavaFileObject.Kind.SOURCE);
			this.source = source;
		}

		@Override
		public CharSequence getCharContent(boolean ignoreErrors) {
			return source;
		}
	}

	static void debug(String msg, Object... args) {
		System.out.format(msg + "\n", args);
	}

	static final String classTemplateSource = 
"""
package <pkg>;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout.PathElement;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;

public class <classname> implements AccessorImpl {
	static final VarHandle varHandle;
	static final MethodHandle byteOffsetHandle;
	static final long elementSize;
	static final Class<?> carrier;

	static {
		AccessorImpl.Info info = AccessorImpl.getInfo("<classname>");
		MemoryLayout layout = info.layout;
		PathElement[] pathElements = info.pathElements;
		varHandle = layout.varHandle(pathElements);
		byteOffsetHandle = layout.byteOffsetHandle(pathElements);
		elementSize = layout.select(pathElements).byteSize();
        carrier = varHandle.varType();
	}

	public <classname>() {
	}

	public AllocationAddress getReference(Allocation allocation) {
		MemoryAddress value = (MemoryAddress)varHandle.get(allocation.segment());
        if (value.equals(MemoryAddress.NULL)) return null;
        return allocation.heap().reformAddress(value);
	}

	public AllocationAddress getReference(Allocation allocation, long index) {
		MemoryAddress value = (MemoryAddress)varHandle.get(allocation.segment(), index);
        if (value.equals(MemoryAddress.NULL)) return null;
        return allocation.heap().reformAddress(value);
	}

	public AllocationAddress getReference(Allocation allocation, long index1, long index2) {
		MemoryAddress value = (MemoryAddress)varHandle.get(allocation.segment(), index1, index2);
        if (value.equals(MemoryAddress.NULL)) return null;
        return allocation.heap().reformAddress(value);
	}

	public Object get(Allocation allocation) {
		return (<carrier>)varHandle.get(allocation.segment());
	}

	public Object get(Allocation allocation, long index) {
		return (<carrier>)varHandle.get(allocation.segment(), index);
	}

	public Object get(Allocation allocation, long index1, long index2) {
		return (<carrier>)varHandle.get(allocation.segment(), index1, index2);
	}

	public Object get(Object... args) {
		Allocation alloc = (Allocation)args[0];
		args[0] = alloc.segment();
        try {
		return (<carrier>)varHandle.toMethodHandle(VarHandle.AccessMode.GET).invokeWithArguments(args);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t.getMessage());
        }
	}

	public AllocationAddress getReference(Object... args) {
		Allocation allocation = (Allocation)args[0];
        MemoryAddress value = (MemoryAddress)get(args);
        return value.equals(MemoryAddress.NULL) ? null : allocation.heap().reformAddress(value);
	}

<set1>
<set2>
<set3>
<set4>
<set5>
<set6>
<set7>
<set8>
<set9>

	public void setReference(Allocation allocation, AllocationAddress value) {
		HighLevelHeap heap = allocation.heap();
		MemoryAddress address = value == null ? MemoryAddress.NULL : heap.transformAddress(value);
		heap.set((s) -> varHandle.set(s, address), byteOffsetHandle, elementSize, allocation.segment());
	}

	public void setReference(Allocation allocation, long index, AllocationAddress value) {
		HighLevelHeap heap = allocation.heap();
		MemoryAddress address = value == null ? MemoryAddress.NULL : heap.transformAddress(value);
		heap.set((s) -> varHandle.set(s, index, address), byteOffsetHandle, elementSize, allocation.segment(), index);
	}

	public void setReference(Allocation allocation, long index1, long index2, AllocationAddress value) {
		HighLevelHeap heap = allocation.heap();
		MemoryAddress address = value == null ? MemoryAddress.NULL : heap.transformAddress(value);
		heap.set((s) -> varHandle.set(s, index1, index2, address), byteOffsetHandle, elementSize, allocation.segment(), index1, index2);
	}

    public void set(Object... args) {
		// TODO: handle null Accessor set, map to 0
		Allocation allocation = (Allocation)args[0];
		HighLevelHeap heap = allocation.heap();
		heap.set((s) -> {
            try {
                args[0] = s;
                MethodHandle mh = varHandle.toMethodHandle(VarHandle.AccessMode.SET);
                mh.invokeWithArguments(args);
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable t) {
                throw new RuntimeException(t.getMessage());
            }
        }, byteOffsetHandle, elementSize, args);
	}

    public void setReference(Object... args) {
		// TODO: handle null Accessor set, map to 0
		AllocationAddress value = (AllocationAddress)args[args.length - 1];
        args[args.length - 1] = value == null ? MemoryAddress.NULL : value.heap().transformAddress(value);
        set(args);
	}

}
""";

	static final String setTemplateSource = 
"""
	public void set(Allocation allocation, <typelower> value){
		HighLevelHeap heap = allocation.heap();
		heap.set((s) -> varHandle.set(s, value), byteOffsetHandle, elementSize, allocation.segment());
	}

	public void set(Allocation allocation, long index, <typelower> value){
		HighLevelHeap heap = allocation.heap();
		heap.set((s) -> varHandle.set(s, index, value), byteOffsetHandle, elementSize, allocation.segment(), index);
	}

	public void set(Allocation allocation, long index1, long index2, <typelower> value) {
		HighLevelHeap heap = allocation.heap();
		heap.set((s) -> varHandle.set(s, index1, index2, value), byteOffsetHandle, elementSize, allocation.segment(), index1, index2);
	}

""";
}
