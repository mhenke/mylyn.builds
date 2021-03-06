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

package org.eclipse.mylyn.builds.tests.mock;

import java.io.Reader;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.builds.core.BuildState;
import org.eclipse.mylyn.builds.core.BuildStatus;
import org.eclipse.mylyn.builds.core.IBuild;
import org.eclipse.mylyn.builds.core.IBuildPlan;
import org.eclipse.mylyn.builds.core.IBuildPlan;
import org.eclipse.mylyn.builds.core.IBuildPlan;
import org.eclipse.mylyn.builds.core.spi.BuildPlanRequest;
import org.eclipse.mylyn.builds.core.spi.GetBuildsRequest;
import org.eclipse.mylyn.builds.core.spi.BuildServerBehaviour;
import org.eclipse.mylyn.builds.core.spi.BuildServerConfiguration;
import org.eclipse.mylyn.builds.core.spi.RunBuildRequest;
import org.eclipse.mylyn.commons.core.IOperationMonitor;

/**
 * @author Steffen Pingel
 */
public class MockBuildServerBehavior extends BuildServerBehaviour {

	public MockBuildServerBehavior() {
	}

	@Override
	public List<IBuild> getBuilds(GetBuildsRequest request, IOperationMonitor monitor) throws CoreException {
		// ignore
		return null;
	}

	@Override
	public BuildServerConfiguration getConfiguration() {
		// ignore
		return null;
	}

	@Override
	public Reader getConsole(IBuild build, IOperationMonitor monitor) throws CoreException {
		// ignore
		return null;
	}

	@Override
	public List<IBuildPlan> getPlans(BuildPlanRequest request, IOperationMonitor monitor) throws CoreException {
		IBuildPlan failingPlan = createBuildPlan();
		failingPlan.setId("1");
		failingPlan.setName("Failing Build Plan");
		failingPlan.setState(BuildState.RUNNING);
		failingPlan.setStatus(BuildStatus.FAILED);
		failingPlan.setHealth(15);

		IBuildPlan childPlan1 = createBuildPlan();
		childPlan1.setId("1.1");
		childPlan1.setName("Stopped Child Build Plan");
		childPlan1.setState(BuildState.STOPPED);
		childPlan1.setStatus(BuildStatus.FAILED);
		//failingPlan.getChildren().add(childPlan1);

		IBuildPlan childPlan2 = createBuildPlan();
		childPlan2.setId("1.2");
		childPlan2.setName("Running Child Build Plan");
		childPlan2.setState(BuildState.STOPPED);
		childPlan2.setStatus(BuildStatus.FAILED);
		childPlan2.setHealth(55);
		//failingPlan.getChildren().add(childPlan2);

		IBuildPlan succeedingPlan = createBuildPlan();
		succeedingPlan.setId("2");
		succeedingPlan.setName("Succeeding Build Plan");
		succeedingPlan.setState(BuildState.STOPPED);
		succeedingPlan.setStatus(BuildStatus.SUCCESS);
		succeedingPlan.setInfo("12 tests passing");
		succeedingPlan.setHealth(89);

		return Arrays.asList((IBuildPlan) failingPlan, childPlan1, childPlan2, succeedingPlan);
	}

	@Override
	public BuildServerConfiguration refreshConfiguration(IOperationMonitor monitor) throws CoreException {
		// ignore
		return null;
	}

	@Override
	public void runBuild(RunBuildRequest request, IOperationMonitor monitor) throws CoreException {
	}

	@Override
	public IStatus validate(IOperationMonitor monitor) throws CoreException {
		return Status.OK_STATUS;
	}

}
