/**
 * 
 */
package fr.n7.stl.minic.ast.instruction.declaration;

import java.util.Iterator;
import java.util.List;

import fr.n7.stl.minic.ast.Block;
import fr.n7.stl.minic.ast.SemanticsUndefinedException;
import fr.n7.stl.minic.ast.instruction.Instruction;
import fr.n7.stl.minic.ast.scope.Declaration;
import fr.n7.stl.minic.ast.scope.HierarchicalScope;
import fr.n7.stl.minic.ast.scope.SymbolTable;
import fr.n7.stl.minic.ast.type.Type;
import fr.n7.stl.tam.ast.Fragment;
import fr.n7.stl.tam.ast.Register;
import fr.n7.stl.tam.ast.TAMFactory;
import fr.n7.stl.util.Logger;

/**
 * Abstract Syntax Tree node for a function declaration.
 * @author Marc Pantel
 */
public class FunctionDeclaration implements DeclarationInstruction {

	/**
	 * Name of the function
	 */
	protected String name;
	
	/**
	 * AST node for the returned type of the function
	 */
	protected Type type;
	
	/**
	 * List of AST nodes for the formal parameters of the function
	 */
	protected List<ParameterDeclaration> parameters;
	
	/**
	 * @return the parameters
	 */
	public List<ParameterDeclaration> getParameters() {
		return parameters;
	}

	/**
	 * AST node for the body of the function
	 */
	protected Block body;

	/**
	 * Builds an AST node for a function declaration
	 * @param _name : Name of the function
	 * @param _type : AST node for the returned type of the function
	 * @param _parameters : List of AST nodes for the formal parameters of the function
	 * @param _body : AST node for the body of the function
	 */
	public FunctionDeclaration(String _name, Type _type, List<ParameterDeclaration> _parameters, Block _body) {
		this.name = _name;
		this.type = _type;
		this.parameters = _parameters;
		this.body = _body;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String _result = this.type + " " + this.name + "( ";
		Iterator<ParameterDeclaration> _iter = this.parameters.iterator();
		if (_iter.hasNext()) {
			_result += _iter.next();
			while (_iter.hasNext()) {
				_result += " ," + _iter.next();
			}
		}
		return _result + " )" + this.body;
	}

	/* (non-Javadoc)
	 * @see fr.n7.stl.block.ast.Declaration#getName()
	 */
	@Override
	public String getName() {
		return this.name;
	}
	
	/* (non-Javadoc)
	 * @see fr.n7.stl.block.ast.Declaration#getType()
	 */
	@Override
	public Type getType() {
		return this.type;
	}
	
	/* (non-Javadoc)
	 * @see fr.n7.stl.block.ast.instruction.Instruction#collect(fr.n7.stl.block.ast.scope.Scope)
	 */
	@Override
	public boolean collectAndPartialResolve(HierarchicalScope<Declaration> _scope) {
		boolean ok;
		if (_scope.accepts(this)) {
                _scope.register(this);
        } else {
            Logger.error("Function : " + this.name + " is already defined.");
            return false;
        }
		SymbolTable newScope = new SymbolTable(_scope);
		for (ParameterDeclaration parameter : this.parameters) {
			if (newScope.accepts(parameter)) {
                newScope.register(parameter);
			} else {
				Logger.error("Parametre : " + parameter.name + " is already defined.");
                return false;
			}
			
		} 
		return this.body.collectAndPartialResolve(newScope);

	}
	
	@Override
	public boolean collectAndPartialResolve(HierarchicalScope<Declaration> _scope, FunctionDeclaration _container) {
		throw new SemanticsUndefinedException( "Semantics collectAndPartialResolve is undefined in ConstantDeclaration.");

	}
	
	/* (non-Javadoc)
	 * @see fr.n7.stl.block.ast.instruction.Instruction#resolve(fr.n7.stl.block.ast.scope.Scope)
	 */
	@Override
	public boolean completeResolve(HierarchicalScope<Declaration> _scope) {
		SymbolTable newScope = new SymbolTable(_scope);
			for (ParameterDeclaration parameter : this.parameters) {
				if (newScope.accepts(parameter)) {
                	newScope.register(parameter);
				} else {
					Logger.error("Parametre : " + parameter.name + " is already defined.");
                	return false;
				}
		} 

		return this.body.completeResolve(newScope);
	}

	/* (non-Javadoc)
	 * @see fr.n7.stl.block.ast.instruction.Instruction#checkType()
	 */
	@Override
	public boolean checkType() {
		boolean ok = this.body.checkType();
		if (!ok) {
			Logger.error("Probleme de type dans le corps de la fonction");
		}
        return ok;
	}
	

	/* (non-Javadoc)
	 * @see fr.n7.stl.block.ast.instruction.Instruction#allocateMemory(fr.n7.stl.tam.ast.Register, int)
	 */
	@Override
	public int allocateMemory(Register _register, int _offset) {
    // Step 1: compute total size of all parameters
    int totalParamSize = 0;
    for (ParameterDeclaration parameter : this.parameters) {
        totalParamSize += parameter.getType().length();
    }

    // Step 2: assign negative offsets, relative to LB
    int paramOffset = -totalParamSize;
    for (ParameterDeclaration parameter : this.parameters) {
        parameter.offset = paramOffset;         // negative!
        paramOffset += parameter.getType().length();
    }

    // Step 3: locals start at LB+0
    this.body.allocateMemory(Register.LB, 0);
    return 0; // function body doesn't contribute to caller's frame
}
	/* (non-Javadoc)
	 * @see fr.n7.stl.block.ast.instruction.Instruction#getCode(fr.n7.stl.tam.ast.TAMFactory)
	 */
	@Override
	public Fragment getCode(TAMFactory _factory) {
		Fragment fragment = _factory.createFragment();
		//fragment.add(_factory.createJump("main"));
		Fragment bodyFragment = this.body.getCode(_factory);
		bodyFragment.addPrefix(this.name);
		fragment.append(bodyFragment);
		return fragment;
	}

}
