/*******************************************************************************
 * Copyright (c) 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.builds.ui.view;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.mylyn.builds.core.IBuild;
import org.eclipse.mylyn.builds.core.IBuildPlan;
import org.eclipse.mylyn.commons.core.DateUtil;

/**
 * @author Steffen Pingel
 */
public class BuildDurationLabelProvider extends ColumnLabelProvider {

	@Override
	public String getText(Object element) {
		if (element instanceof IBuildPlan) {
			IBuildPlan plan = (IBuildPlan) element;
			if (plan.getLastBuild() != null) {
				return getText(plan.getLastBuild());
			}
		}
		if (element instanceof IBuild) {
			return getText((IBuild) element);
		}
		return ""; //$NON-NLS-1$
	}

	protected String getText(IBuild build) {
		return DateUtil.getFormattedDurationShort(build.getDuration(), true);
	}

}
