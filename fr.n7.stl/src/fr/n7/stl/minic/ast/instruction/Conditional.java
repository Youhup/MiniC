/**
 * 
 */
package fr.n7.stl.minic.ast.instruction;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import fr.n7.stl.minic.ast.Block;
import fr.n7.stl.minic.ast.SemanticsUndefinedException;
import fr.n7.stl.minic.ast.expression.Expression;
import fr.n7.stl.minic.ast.instruction.declaration.DeclarationInstruction;
import fr.n7.stl.minic.ast.instruction.declaration.FunctionDeclaration;
import fr.n7.stl.minic.ast.scope.Declaration;
import fr.n7.stl.minic.ast.scope.HierarchicalScope;
import fr.n7.stl.minic.ast.type.AtomicType;
import fr.n7.stl.tam.ast.Fragment;
import fr.n7.stl.tam.ast.Register;
import fr.n7.stl.tam.ast.TAMFactory;
import fr.n7.stl.util.Logger;

/**
 * Implementation of the Abstract Syntax Tree node for a conditional instruction.
 * @author Marc Pantel
 *
 */
public class Conditional implements Instruction {

	protected Expression condition;
	protected Block thenBranch;
	protected Block elseBranch;

	public Conditional(Expression _condition, Block _then, Block _else) {
		this.condition = _condition;
		this.thenBranch = _then;
		this.elseBranch = _else;
	}

	public Conditional(Expression _condition, Block _then) {
		this.condition = _condition;
		this.thenBranch = _then;
		this.elseBranch = null;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "if (" + this.condition + " )" + this.thenBranch + ((this.elseBranch != null)?(" else " + this.elseBranch):"");
	}
	
	/* (non-Javadoc)
	 * @see fr.n7.stl.block.ast.instruction.Instruction#collect(fr.n7.stl.block.ast.scope.Scope)
	 */
	@Override
	public boolean collectAndPartialResolve(HierarchicalScope<Declaration> _scope) {
		boolean ok = true;
    	ok &= this.condition.collectAndPartialResolve(_scope);
    		
        ok &= this.thenBranch.collectAndPartialResolve(_scope);
		if (this.elseBranch != null) {
			ok &= this.elseBranch.collectAndPartialResolve(_scope);
		} 
        return ok;
	}
	
	/* (non-Javadoc)
	 * @see fr.n7.stl.block.ast.instruction.Instruction#collect(fr.n7.stl.block.ast.scope.Scope)
	 */
	@Override
	public boolean collectAndPartialResolve(HierarchicalScope<Declaration> _scope, FunctionDeclaration _container) {
		throw new SemanticsUndefinedException( "Semantics collect is undefined in Conditional.");
	}
	
	/* (non-Javadoc)
	 * @see fr.n7.stl.block.ast.instruction.Instruction#resolve(fr.n7.stl.block.ast.scope.Scope)
	 */
	@Override
	public boolean completeResolve(HierarchicalScope<Declaration> _scope) {
		boolean ok = true;
    	ok &= this.condition.completeResolve(_scope);
    		
        ok &= this.thenBranch.completeResolve(_scope);
		if (this.elseBranch != null) {
			ok &= this.elseBranch.completeResolve(_scope);
		} 
        return ok;
	}

	/* (non-Javadoc)
	 * @see fr.n7.stl.block.ast.Instruction#checkType()
	 */
	@Override
	public boolean checkType() {
		boolean ok = true;
    	ok &= this.condition.getType().equals(AtomicType.BooleanType);
		if (!ok) {
			Logger.error("La condition n'est pas de type boolean");
		}
    		
        ok &= this.thenBranch.checkType();
		if (this.elseBranch != null) {
			ok &= this.elseBranch.checkType();
		} 
        return ok;
	}

	/* (non-Javadoc)
	 * @see fr.n7.stl.block.ast.Instruction#allocateMemory(fr.n7.stl.tam.ast.Register, int)
	 */
	@Override
	public int allocateMemory(Register _register, int _offset) {
		this.thenBranch.allocateMemory(_register, _offset);
		if (this.elseBranch != null) {
			this.elseBranch.allocateMemory(_register, _offset);	
		} 
		return _offset;
		
	}

	/* (non-Javadoc)
	 * @see fr.n7.stl.block.ast.Instruction#getCode(fr.n7.stl.tam.ast.TAMFactory)
	 */
	@Override
	public Fragment getCode(TAMFactory _factory) {
		String FinIfLabel = "fin_"  + _factory.createLabelNumber();
		String elseLabel = "else_" + _factory.createLabelNumber();

		Fragment fragment = _factory.createFragment();

		fragment.append(this.condition.getCode(_factory));

		if (this.elseBranch != null){
			fragment.add(_factory.createJumpIf(elseLabel, 0));

			Fragment thenFragment = this.thenBranch.getCode(_factory);
			thenFragment.add(_factory.createJump(FinIfLabel));
			fragment.append(thenFragment);

			Fragment elseFragment = this.elseBranch.getCode(_factory);
			elseFragment.addPrefix(elseLabel);
			fragment.append(elseFragment);
		}  else {
			fragment.add(_factory.createJumpIf(FinIfLabel, 0));

			Fragment thenFragment = this.thenBranch.getCode(_factory);
			fragment.append(thenFragment);
		}
		
		
		fragment.addSuffix(FinIfLabel);
		return fragment;
	}

}
