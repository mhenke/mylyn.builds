/*******************************************************************************
 * Copyright (c) 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *     Itema AS - Automatic refresh when a new repo has been added; bug 330910
 *******************************************************************************/

package org.eclipse.mylyn.internal.builds.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.mylyn.builds.internal.core.operations.BuildJob;
import org.eclipse.mylyn.builds.internal.core.operations.RefreshOperation;
import org.eclipse.mylyn.commons.core.IOperationMonitor;
import org.eclipse.mylyn.commons.core.IOperationMonitor.OperationFlag;

/**
 * @author Steffen Pingel
 * @author Torkild U. Resheim
 */
public class BuildRefresher implements IPropertyChangeListener {

	private static final long STARTUP_DELAY = 5 * 1000;

	private class RefreshJob extends BuildJob {

		public RefreshJob() {
			super("Background Builds Refresh");
			setUser(false);
		}

		@Override
		protected IStatus doExecute(IOperationMonitor progress) {
			RefreshOperation refreshOperation = BuildsUiInternal.getFactory().getRefreshOperation();
			refreshOperation.addFlag(OperationFlag.BACKGROUND);
			return refreshOperation.doExecute(progress);
		}
	};

	public BuildRefresher() {
	}

	private RefreshJob refreshJob;

	private long getInterval() {
		return BuildsUiPlugin.getDefault().getPreferenceStore().getLong(BuildsUiInternal.PREF_AUTO_REFRESH_INTERVAL);
	}

	public boolean isEnabled() {
		return BuildsUiPlugin.getDefault().getPreferenceStore().getBoolean(BuildsUiInternal.PREF_AUTO_REFRESH_ENABLED);
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(BuildsUiInternal.PREF_AUTO_REFRESH_ENABLED)
				|| event.getProperty().equals(BuildsUiInternal.PREF_AUTO_REFRESH_INTERVAL)) {
			reschedule(0L);
		}
	}

	public void start() {
		reschedule(STARTUP_DELAY);
	}

	/**
	 * Performs an immediate one-shot refresh of build server data regardless of the automatic refresh preference
	 * setting.
	 */
	void refresh() {
		if (refreshJob == null) {
			refreshJob = new RefreshJob();
			refreshJob.setSystem(true);
		}
		BuildsUiInternal.getModel().getScheduler().schedule(refreshJob, 0);
	}

	private synchronized void reschedule(long delay) {
		if (isEnabled()) {
			if (refreshJob == null) {
				refreshJob = new RefreshJob();
				refreshJob.setSystem(true);
				refreshJob.addJobChangeListener(new JobChangeAdapter() {
					@Override
					public void done(IJobChangeEvent event) {
						reschedule(getInterval());
					}
				});
			}
			BuildsUiInternal.getModel().getScheduler().schedule(refreshJob, delay);
		} else {
			if (refreshJob != null) {
				refreshJob.cancel();
			}
		}
	}

	public synchronized void stop() {
		if (refreshJob != null) {
			refreshJob.cancel();
		}
	}

}
