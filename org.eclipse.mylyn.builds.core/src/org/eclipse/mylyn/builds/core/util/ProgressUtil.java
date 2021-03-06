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

package org.eclipse.mylyn.builds.core.util;

import java.util.EnumSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.mylyn.commons.core.IOperationMonitor;

/**
 * @author Steffen Pingel
 */
public class ProgressUtil {

	private static class NullOperationMonitor extends NullProgressMonitor implements IOperationMonitor {

		private EnumSet<OperationFlag> flags;

		public synchronized void addFlag(OperationFlag flag) {
			if (flags == null) {
				flags = EnumSet.of(flag);
			} else {
				flags.add(flag);
			}
		}

		public void clearBlocked() {
			// ignore			
		}

		public synchronized boolean hasFlag(OperationFlag flag) {
			if (flags != null) {
				return flags.contains(flag);
			}
			return false;
		}

		public IOperationMonitor newChild(int totalWork) {
			return this;
		}

		public IOperationMonitor newChild(int totalWork, int suppressFlags) {
			return this;
		}

		public synchronized void removeFlag(OperationFlag flag) {
			if (flags != null) {
				flags.remove(flag);
			}
		}

		public void setBlocked(IStatus reason) {
			// ignore			
		}

		public IOperationMonitor setWorkRemaining(int workRemaining) {
			return this;
		}

	}

	private static class OperationMonitor implements IOperationMonitor {

		private EnumSet<OperationFlag> flags;

		private final SubMonitor monitor;

		private final IOperationMonitor root;

		protected OperationMonitor(IOperationMonitor root, IProgressMonitor monitor) {
			this.root = root;
			this.monitor = SubMonitor.convert(monitor);
		}

		protected OperationMonitor(IOperationMonitor root, IProgressMonitor monitor, String taskName, int work) {
			this.root = root;
			this.monitor = SubMonitor.convert(monitor, taskName, work);
		}

		public synchronized void addFlag(OperationFlag flag) {
			if (root != null) {
				root.addFlag(flag);
			} else if (flags == null) {
				flags = EnumSet.of(flag);
			} else {
				flags.add(flag);
			}
		}

		public void beginTask(String name, int totalWork) {
			monitor.beginTask(name, totalWork);
		}

		public void clearBlocked() {
			monitor.clearBlocked();
		}

		public void done() {
			monitor.done();
		}

		@Override
		public boolean equals(Object obj) {
			return monitor.equals(obj);
		}

		public synchronized boolean hasFlag(OperationFlag flag) {
			if (root != null) {
				return root.hasFlag(flag);
			} else if (flags != null) {
				return flags.contains(flag);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return monitor.hashCode();
		}

		public void internalWorked(double work) {
			monitor.internalWorked(work);
		}

		public boolean isCanceled() {
			return monitor.isCanceled();
		}

		public IOperationMonitor newChild(int totalWork) {
			return new OperationMonitor((root == null) ? this : root, monitor.newChild(totalWork));
		}

		public IOperationMonitor newChild(int totalWork, int suppressFlags) {
			return new OperationMonitor((root == null) ? this : root, monitor.newChild(totalWork, suppressFlags));
		}

		public synchronized void removeFlag(OperationFlag flag) {
			if (root != null) {
				root.removeFlag(flag);
			} else if (flags != null) {
				flags.remove(flag);
			}
		}

		public void setBlocked(IStatus reason) {
			monitor.setBlocked(reason);
		}

		public void setCanceled(boolean b) {
			monitor.setCanceled(b);
		}

		public void setTaskName(String name) {
			monitor.setTaskName(name);
		}

		public IOperationMonitor setWorkRemaining(int workRemaining) {
			monitor.setWorkRemaining(workRemaining);
			return this;
		}

		public void subTask(String name) {
			monitor.subTask(name);
		}

		@Override
		public String toString() {
			return monitor.toString();
		}

		public void worked(int work) {
			monitor.worked(work);
		}

	}

	public static IOperationMonitor convert(IProgressMonitor monitor) {
		return convert(monitor, "", 0); //$NON-NLS-1$
	}

	public static IOperationMonitor convert(IProgressMonitor monitor, int work) {
		return convert(monitor, "", work); //$NON-NLS-1$
	}

	public static IOperationMonitor convert(IProgressMonitor monitor, String taskName, int work) {
		if (monitor instanceof IOperationMonitor) {
			return (IOperationMonitor) monitor;
		}
		if (monitor == null) {
			return new NullOperationMonitor();
		}
		return new OperationMonitor(null, monitor, taskName, work);
	}

}
