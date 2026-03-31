package chocopy.pa2;

import chocopy.common.analysis.AbstractNodeAnalyzer;
import chocopy.common.analysis.SymbolTable;
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
import chocopy.common.astnodes.VarDef;

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
        // TODO: build FuncType from params + return type, push a new scope,
        // recurse into funcDef.declarations for nested vars/funcs/global/nonlocal,
        // check rule 6 (first param must match enclosing class), rule 9 (return paths)
        return null;
    }

    @Override
    public Type analyze(ClassDef classDef) {
        // TODO: push a new class scope, recurse into classDef.declarations,
        // check rule 5 (no attribute override), rule 7 (method signature match)
        return null;
    }

    @Override
    public Type analyze(GlobalDecl globalDecl) {
        // TODO: record that this name refers to the global scope,
        // check rule 3 (must actually exist in global scope)
        return null;
    }

    @Override
    public Type analyze(NonLocalDecl nonLocalDecl) {
        // TODO: record that this name refers to an enclosing (non-global) scope,
        // check rule 3 (must exist in a valid enclosing function scope)
        return null;
    }

}
