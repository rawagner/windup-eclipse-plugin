/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.

 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.windup.ui.internal.launch;

import static org.jboss.tools.windup.model.domain.WindupConstants.LAUNCH_COMPLETED;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.windup.core.services.WindupService;
import org.jboss.tools.windup.model.domain.ModelService;
import org.jboss.tools.windup.ui.internal.Messages;
import org.jboss.tools.windup.ui.internal.services.ConsoleService;
import org.jboss.tools.windup.ui.internal.services.MarkerService;
import org.jboss.tools.windup.windup.ConfigurationElement;

/**
 * The launch delegate for Windup.
 */
public class WindupLaunchDelegate implements ILaunchConfigurationDelegate {
	
	@Inject private ModelService modelService;
	@Inject private WindupService windupService;
	@Inject private IEventBroker broker;
	@Inject private MarkerService markerService;
	@Inject private ConsoleService consoleService;
	
	public void launch(ILaunchConfiguration config, String mode, ILaunch launch, IProgressMonitor monitor) {
		ConfigurationElement configuration = modelService.findConfiguration(config.getName());
		if (configuration == null || configuration.getInputs().isEmpty()) {
			Display.getDefault().asyncExec(() -> {
				MessageDialog.openInformation(Display.getDefault().getActiveShell(), 
						Messages.launchErrorTitle, Messages.launchErrorMessage);
			});
		}
		else {
			markerService.deleteAllWindupMarkers();
			Job job = new Job(NLS.bind(Messages.generate_windup_report_for, configuration.getName())) {
	            @Override
	            protected IStatus run(IProgressMonitor monitor) {
	            	IStatus status = null;
	            	try {
	            		consoleService.startCapturing();
	            		status = windupService.generateGraph(configuration, monitor);
	            	} 
	            	finally {
	            		consoleService.stopCapturing();
	            	}
	            	broker.post(LAUNCH_COMPLETED, configuration);
	                return status;
	            }
	        };
	        job.setUser(true);
	        job.schedule();
		}
	}
}
