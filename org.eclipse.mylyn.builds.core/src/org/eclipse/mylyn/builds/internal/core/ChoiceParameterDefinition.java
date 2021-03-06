/**
 * <copyright>
 * </copyright>
 *
 * $Id: ChoiceParameterDefinition.java,v 1.2 2010/08/28 09:21:40 spingel Exp $
 */
package org.eclipse.mylyn.builds.internal.core;

import java.util.Collection;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.util.EDataTypeUniqueEList;
import org.eclipse.mylyn.builds.core.IChoiceParameterDefinition;

/**
 * <!-- begin-user-doc --> A representation of the model object '<em><b>Choice Parameter Definition</b></em>'. <!--
 * end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link org.eclipse.mylyn.builds.internal.core.ChoiceParameterDefinition#getOptions <em>Options</em>}</li>
 * </ul>
 * </p>
 * 
 * @generated
 */
public class ChoiceParameterDefinition extends ParameterDefinition implements IChoiceParameterDefinition {
	/**
	 * The cached value of the '{@link #getOptions() <em>Options</em>}' attribute list. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @see #getOptions()
	 * @generated
	 * @ordered
	 */
	protected EList<String> options;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected ChoiceParameterDefinition() {
		super();
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return BuildPackage.Literals.CHOICE_PARAMETER_DEFINITION;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Options</em>' attribute list isn't clear, there really should be more of a description
	 * here...
	 * </p>
	 * <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public List<String> getOptions() {
		if (options == null) {
			options = new EDataTypeUniqueEList<String>(String.class, this,
					BuildPackage.CHOICE_PARAMETER_DEFINITION__OPTIONS);
		}
		return options;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
		case BuildPackage.CHOICE_PARAMETER_DEFINITION__OPTIONS:
			return getOptions();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
		case BuildPackage.CHOICE_PARAMETER_DEFINITION__OPTIONS:
			getOptions().clear();
			getOptions().addAll((Collection<? extends String>) newValue);
			return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
		case BuildPackage.CHOICE_PARAMETER_DEFINITION__OPTIONS:
			getOptions().clear();
			return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
		case BuildPackage.CHOICE_PARAMETER_DEFINITION__OPTIONS:
			return options != null && !options.isEmpty();
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public String toString() {
		if (eIsProxy())
			return super.toString();

		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (options: "); //$NON-NLS-1$
		result.append(options);
		result.append(')');
		return result.toString();
	}

} // ChoiceParameterDefinition
