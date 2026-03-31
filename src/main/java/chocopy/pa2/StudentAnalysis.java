package chocopy.pa2;

import chocopy.common.analysis.SymbolTable;
import chocopy.common.analysis.types.Type;
import chocopy.common.astnodes.Program;

/** Top-level class for performing semantic analysis. */
public class StudentAnalysis {

    /** Perform semantic analysis on PROGRAM, adding error messages and
     *  type annotations. Provide debugging output iff DEBUG. Returns modified
     *  tree. */
    public static Program process(Program program, boolean debug) {
        if (program.hasErrors()) {
            return program;
        }

        // Pass 1: build class inheritance graph and validate rule 4.
        ClassHierarchyBuilder hierarchy = new ClassHierarchyBuilder(program.errors);
        hierarchy.build(program);

        DeclarationAnalyzer declarationAnalyzer =
            new DeclarationAnalyzer(program.errors);
        program.dispatch(declarationAnalyzer);
        SymbolTable<Type> globalSym =
            declarationAnalyzer.getGlobals();

        if (!program.hasErrors()) {
            TypeChecker typeChecker =
                new TypeChecker(globalSym, program.errors);
            program.dispatch(typeChecker);
        }

        return program;
    }
}
