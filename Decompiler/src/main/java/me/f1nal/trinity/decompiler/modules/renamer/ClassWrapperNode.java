// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package me.f1nal.trinity.decompiler.modules.renamer;

import me.f1nal.trinity.decompiler.struct.StructClass;

import java.util.ArrayList;
import java.util.List;

public class ClassWrapperNode {
  private final StructClass classStruct;
  private final List<ClassWrapperNode> subclasses = new ArrayList<>();

  public ClassWrapperNode(StructClass cl) {
    this.classStruct = cl;
  }

  public void addSubclass(ClassWrapperNode node) {
    subclasses.add(node);
  }

  public StructClass getClassStruct() {
    return classStruct;
  }

  public List<ClassWrapperNode> getSubclasses() {
    return subclasses;
  }
}