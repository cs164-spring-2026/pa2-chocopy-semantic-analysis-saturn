package chocopy.pa2;

import chocopy.common.analysis.types.ClassValueType;
import chocopy.common.analysis.types.Type;
import chocopy.common.analysis.types.ValueType;
import chocopy.common.astnodes.ClassDef;
import chocopy.common.astnodes.Declaration;
import chocopy.common.astnodes.Errors;
import chocopy.common.astnodes.Program;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Pass 1 - build the class hierarchy before anything else runs
// We need this so isSubtype/LUB work during declaration analysis and type checking
public class ClassHierarchyBuilder {

    // maps class name -> parent class name (null for object)
    private final Map<String, String> parentMap = new HashMap<>();
    private final Errors errors;

    // built-in classes you can't extend
    private static final Set<String> SPECIAL_CLASSES = new HashSet<>();
    static {
        SPECIAL_CLASSES.add("int");
        SPECIAL_CLASSES.add("str");
        SPECIAL_CLASSES.add("bool");
        SPECIAL_CLASSES.add("<None>");
        SPECIAL_CLASSES.add("<Empty>");
    }

    public ClassHierarchyBuilder(Errors errors) {
        this.errors = errors;
        // seed with built-in hierarchy
        parentMap.put("object", null);
        parentMap.put("int", "object");
        parentMap.put("bool", "int");   // bool < int < object
        parentMap.put("str", "object");
        parentMap.put("<None>", "object");
        parentMap.put("<Empty>", "object");
    }

    public void build(Program program) {
        // first collect all non-class names so we can give the right error message
        // (e.g. "must be a class" vs "not defined")
        Set<String> nonClassNames = new HashSet<>();
        for (Declaration decl : program.declarations) {
            if (!(decl instanceof ClassDef)) {
                nonClassNames.add(decl.getIdentifier().name);
            }
        }

        for (Declaration decl : program.declarations) {
            if (!(decl instanceof ClassDef)) continue;

            ClassDef classDef = (ClassDef) decl;
            String name = classDef.name.name;
            String superName = classDef.superClass.name;

            // check rule 4 - three different error cases
            if (SPECIAL_CLASSES.contains(superName)) {
                errors.semError(classDef.superClass,
                        "Cannot extend special class: %s", superName);
            } else if (nonClassNames.contains(superName)) {
                errors.semError(classDef.superClass,
                        "Super-class must be a class: %s", superName);
            } else if (!parentMap.containsKey(superName)) {
                // not defined yet - either undefined or forward reference
                errors.semError(classDef.superClass,
                        "Super-class not defined: %s", superName);
            }

            parentMap.put(name, superName);
        }
    }

    // is type a a subtype of type b?
    public boolean isSubtype(Type a, Type b) {
        if (a == null || b == null) return false;
        if (a.equals(b)) return true;

        // <None> is a subtype of every class and list type
        if (Type.NONE_TYPE.equals(a)) return b.isValueType();

        // <Empty> is a subtype of any list type
        if (Type.EMPTY_TYPE.equals(a)) return b.isListType();

        // lists are invariant so [T] <= [S] only if T == S (already handled above)
        if (a.isListType()) return false;
        if (b.isListType()) return false;

        if (!a.isValueType() || !b.isValueType()) return false;

        // walk up the parent chain of a to see if we hit b
        String cur = a.className();
        String target = b.className();
        while (cur != null) {
            if (cur.equals(target)) return true;
            cur = parentMap.get(cur);
        }
        return false;
    }

    // find the least upper bound of two types
    public ValueType leastUpperBound(Type a, Type b) {
        if (a == null || b == null) {
            return (a != null) ? (ValueType) a : (ValueType) b;
        }
        if (a.equals(b)) return (ValueType) a;

        // <None> is a subtype of everything so LUB(<None>, X) = X
        if (Type.NONE_TYPE.equals(a)) return (ValueType) b;
        if (Type.NONE_TYPE.equals(b)) return (ValueType) a;

        // <Empty> + list type -> that list type
        if (Type.EMPTY_TYPE.equals(a)) return b.isListType() ? (ValueType) b : Type.OBJECT_TYPE;
        if (Type.EMPTY_TYPE.equals(b)) return a.isListType() ? (ValueType) a : Type.OBJECT_TYPE;

        // two different list types -> object (lists are invariant)
        if (a.isListType() && b.isListType()) return Type.OBJECT_TYPE;
        if (a.isListType() || b.isListType()) return Type.OBJECT_TYPE;

        // both class types - walk b's ancestors and find first one that's also an ancestor of a
        List<String> aAncestors = getAncestors(a.className());
        String cur = b.className();
        while (cur != null) {
            if (aAncestors.contains(cur)) return new ClassValueType(cur);
            cur = parentMap.get(cur);
        }
        return Type.OBJECT_TYPE; // shouldn't happen
    }

    private List<String> getAncestors(String className) {
        List<String> result = new ArrayList<>();
        String cur = className;
        while (cur != null) {
            result.add(cur);
            cur = parentMap.get(cur);
        }
        return result;
    }

    public ClassValueType getClassType(String name) {
        return parentMap.containsKey(name) ? new ClassValueType(name) : null;
    }

    public boolean isDefinedClass(String name) {
        return parentMap.containsKey(name);
    }

    public String getSuperClassName(String className) {
        return parentMap.get(className);
    }
}
