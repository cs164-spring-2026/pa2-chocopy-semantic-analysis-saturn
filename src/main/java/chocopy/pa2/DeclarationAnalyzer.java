package chocopy.pa2;

import chocopy.common.analysis.AbstractNodeAnalyzer;
import chocopy.common.analysis.SymbolTable;
import chocopy.common.analysis.types.ClassValueType;
import chocopy.common.analysis.types.FuncType;
import chocopy.common.analysis.types.Type;
import chocopy.common.analysis.types.ValueType;
import chocopy.common.astnodes.ClassDef;
import chocopy.common.astnodes.Declaration;
import chocopy.common.astnodes.Errors;
import chocopy.common.astnodes.FuncDef;
import chocopy.common.astnodes.GlobalDecl;
import chocopy.common.astnodes.Identifier;
import chocopy.common.astnodes.NonLocalDecl;
import chocopy.common.astnodes.Program;
import chocopy.common.astnodes.TypedVar;
import chocopy.common.astnodes.VarDef;

import java.util.ArrayList;
import java.util.List;

/**
 * Analyzes declarations to create a top-level symbol table.
 */
public class DeclarationAnalyzer extends AbstractNodeAnalyzer<Type> {

    /** Current symbol table.  Changes with new declarative region. */
    private SymbolTable<Type> sym = new SymbolTable<>();
    /** Global symbol table. */
    private final SymbolTable<Type> globals = sym;
    /** Receiver for semantic error messages. */
    private final Errors errors;

    /** A new declaration analyzer sending errors to ERRORS0. */
    public DeclarationAnalyzer(Errors errors0) {
        errors = errors0;
        // pre-populate built-in names so they count as already declared
        sym.put("int",    new ClassValueType("int"));
        sym.put("str",    new ClassValueType("str"));
        sym.put("bool",   new ClassValueType("bool"));
        sym.put("object", new ClassValueType("object"));
        sym.put("print",  new FuncType(List.of(Type.OBJECT_TYPE), Type.NONE_TYPE));
        sym.put("input",  new FuncType(List.of(), Type.STR_TYPE));
        sym.put("len",    new FuncType(List.of(Type.OBJECT_TYPE), Type.INT_TYPE));
    }

    public SymbolTable<Type> getGlobals() {
        return globals;
    }

    @Override
    public Type analyze(Program program) {
        for (Declaration decl : program.declarations) {
            Identifier id = decl.getIdentifier();
            String name = id.name;

            Type type = decl.dispatch(this);

            if (type == null) {
                continue;
            }

            if (sym.declares(name)) {
                errors.semError(id,
                                "Duplicate declaration of identifier in same "
                                + "scope: %s",
                                name);
            } else {
                sym.put(name, type);
            }
        }

        return null;
    }

    @Override
    public Type analyze(VarDef varDef) {
        return ValueType.annotationToValueType(varDef.var.type);
    }

    @Override
    public Type analyze(FuncDef funcDef) {
        // build the FuncType from the param + return type annotations
        List<ValueType> paramTypes = new ArrayList<>();
        for (TypedVar param : funcDef.params) {
            paramTypes.add(ValueType.annotationToValueType(param.type));
        }
        ValueType retType = ValueType.annotationToValueType(funcDef.returnType);
        FuncType funcType = new FuncType(paramTypes, retType);

        // push a new scope for the function body
        SymbolTable<Type> savedSym = sym;
        sym = new SymbolTable<>(savedSym);

        // register each param, checking for duplicates among params
        for (TypedVar param : funcDef.params) {
            String paramName = param.identifier.name;
            ValueType paramType = ValueType.annotationToValueType(param.type);
            if (sym.declares(paramName)) {
                errors.semError(param.identifier,
                        "Duplicate declaration of identifier in same scope: %s", paramName);
            } else {
                sym.put(paramName, paramType);
            }
        }

        // process inner declarations (local vars, nested funcs, global/nonlocal)
        for (Declaration decl : funcDef.declarations) {
            Identifier id = decl.getIdentifier();
            String name = id.name;
            Type type = decl.dispatch(this);
            if (type == null) continue;
            if (sym.declares(name)) {
                errors.semError(id,
                        "Duplicate declaration of identifier in same scope: %s", name);
            } else {
                sym.put(name, type);
            }
        }

        // TODO: rule 6 - first param must match enclosing class type (when inside a class)
        // TODO: rule 9 - non-<None> return type requires explicit return on all paths

        // restore outer scope
        sym = savedSym;

        return funcType;
    }

    @Override
    public Type analyze(ClassDef classDef) {
        // push a new scope for the class body
        SymbolTable<Type> savedSym = sym;
        sym = new SymbolTable<>(savedSym);

        for (Declaration decl : classDef.declarations) {
            Identifier id = decl.getIdentifier();
            String name = id.name;
            Type type = decl.dispatch(this);
            if (type == null) continue;
            if (sym.declares(name)) {
                errors.semError(id,
                        "Duplicate declaration of identifier in same scope: %s", name);
            } else {
                sym.put(name, type);
            }
        }

        // TODO: rule 5 - attributes must not override inherited attributes/methods
        // TODO: rule 7 - method overrides must have matching signatures

        sym = savedSym;

        // return a placeholder so the class name gets registered in the outer scope
        return new ClassValueType(classDef.name.name);
    }

    @Override
    public Type analyze(GlobalDecl globalDecl) {
        // TODO: validate that the name exists in global scope (rule 3)
        // TODO: return the global's type so it counts as declared in this scope
        return null;
    }

    @Override
    public Type analyze(NonLocalDecl nonLocalDecl) {
        // TODO: validate that the name exists in an enclosing function scope (rule 3)
        // TODO: return the nonlocal's type so it counts as declared in this scope
        return null;
    }

}
