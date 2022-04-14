# 
# Copyright (C) 2022 Intel Corporation
#
# SPDX-License-Identifier: BSD-3-Clause
# 
#

CC = g++
JAVAC = $(JAVA_HOME)/bin/javac
JAVA = $(JAVA_HOME)/bin/java
JAR = $(JAVA_HOME)/bin/jar
JAVADOC = $(JAVA_HOME)/bin/javadoc

JNI_INCLUDES = $(JAVA_HOME)/include $(JAVA_HOME)/include/linux

CFLAGS = -O3 -DNDEBUG -fPIC -shared -D_FORTIFY_SOURCE=2 -z noexecstack -fstack-protector -Wformat -Wformat-security -Werror=format-security
JAVAFLAGS = -Xlint:unchecked -proc:none -XDenableSunApiLintControl --add-modules jdk.incubator.foreign,java.compiler --add-exports jdk.incubator.foreign/jdk.incubator.foreign.unsafe=com.intel.pmem.pmul --add-exports java.base/jdk.internal.misc=com.intel.pmem.pmul --add-reads com.intel.pmem.pmul=jdk.incubator.foreign,java.compiler --module-source-path com.intel.pmem.pmul=src/main/java --module com.intel.pmem.pmul
JAVADOCFLAGS = --add-modules jdk.incubator.foreign,java.compiler --add-exports jdk.incubator.foreign/jdk.incubator.foreign.unsafe=com.intel.pmem.pmul --add-exports java.base/jdk.internal.misc=com.intel.pmem.pmul --add-reads com.intel.pmem.pmul=jdk.incubator.foreign,java.compiler  #--module-source-path src/main/java --module pmem
LINK_FLAGS = -fPIC -O3 -DNDEBUG -shared -lpmem -lpmemobj -lmemkind -Wl,-rpath,/usr/local/lib:/usr/local/lib64 -Wl,-z,relro -Wl,-z,now -Wl,-z,noexecstack

CPP_SOURCE_DIR = src/main/cpp
JAVA_SOURCE_DIR = src/main/java/pmem
MODULE_NAME = com.intel.pmem.pmul
PACKAGE_NAME = com.intel.pmem.pmul

TARGET_DIR = target
CPP_BUILD_DIR = $(TARGET_DIR)/cppbuild
CLASSES_DIR = $(TARGET_DIR)/classes
EXAMPLES_DIR = src/examples/com/intel/pmem/pmul/examples
ALL_EXAMPLES_DIR = $(wildcard $(EXAMPLES_DIR)/*)

BASE_CLASSPATH = $(CLASSES_DIR):lib

ALL_CPP_SOURCES = $(wildcard $(CPP_SOURCE_DIR)/*.cpp)
ALL_JAVA_SOURCES = $(wildcard $(JAVA_SOURCE_DIR)/$(MODULE_NAME)/$(PACKAGE_NAME)/*.java)
ALL_OBJ = $(addprefix $(CPP_BUILD_DIR)/, $(notdir $(ALL_CPP_SOURCES:.cpp=.o)))

LIBRARIES = $(addprefix $(CPP_BUILD_DIR)/, libpmul.so)

all: sources 
sources: cpp java
cpp: $(LIBRARIES)
java: classes
docs: classes
	$(JAVADOC) $(JAVADOCFLAGS) -d docs com.intel.pmem.pmul -sourcepath $(JAVA_SOURCE_DIR) 
jar: sources
	$(JAR) cvf $(TARGET_DIR)/pmul.jar -C $(CLASSES_DIR)/$(MODULE_NAME) com/intel/pmem/ 
examples: sources
	$(foreach example_dir,$(ALL_EXAMPLES_DIR), $(JAVAC) --module-path target/classes/:lib  --add-modules com.intel.pmem.pmul,jdk.incubator.foreign -cp $(BASE_CLASSPATH)/$(PACKAGE_NAME):src/examples/ $(example_dir)/*.java;) 

clean: 
	$(foreach example_dir,$(ALL_EXAMPLES_DIR), rm -rf $(example_dir)/*.class;)
	rm -rf target
	rm -rf docs


$(LIBRARIES): | $(CPP_BUILD_DIR)
$(ALL_OBJ): | $(CPP_BUILD_DIR)

classes: | $(CLASSES_DIR) 
	$(JAVAC) $(JAVAFLAGS) -d $(CLASSES_DIR) -cp $(BASE_CLASSPATH) $(ALL_JAVA_SOURCES)

$(CPP_BUILD_DIR)/%.so: $(ALL_OBJ)
	$(CC) -Wl,-soname,$@ -o $@ $(ALL_OBJ) $(LINK_FLAGS)

$(CPP_BUILD_DIR)/%.o: $(CPP_SOURCE_DIR)/%.cpp
ifndef JAVA_HOME
	$(error JAVA_HOME not set)
endif
	$(CC) $(CFLAGS) $(addprefix -I, $(JNI_INCLUDES)) -o $@ -c $<

$(CPP_BUILD_DIR):
	mkdir -p $(CPP_BUILD_DIR)

$(CLASSES_DIR):
	mkdir -p $(CLASSES_DIR)

