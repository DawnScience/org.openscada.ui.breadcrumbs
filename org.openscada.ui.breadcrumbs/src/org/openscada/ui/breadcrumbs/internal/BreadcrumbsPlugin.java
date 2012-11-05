/*******************************************************************************
 * Copyright (c) 2012 TH4 SYSTEMS GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jens Reimann (TH4 SYSTEMS GmbH) - initial API and implementation
 *******************************************************************************/
package org.openscada.ui.breadcrumbs.internal;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class BreadcrumbsPlugin extends AbstractUIPlugin {

	public static boolean DEBUG = false;

	private static BreadcrumbsPlugin instance;

	public BreadcrumbsPlugin() {
	}

	@Override
	public void start(final BundleContext context) throws Exception {
		instance = this;
		super.start(context);
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		instance = null;
		super.stop(context);
	}

	/**
	 * Returns the singleton instance of the debug plug-in.
	 */
	public static BreadcrumbsPlugin getDefault() {
		return instance;
	}
}
