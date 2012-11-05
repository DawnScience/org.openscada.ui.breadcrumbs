/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Pawel Piech (Wind River) - adapted breadcrumb for use in Debug view (Bug 252677)
 *     Jens Reimann (TH4 SYSTEMS GmbH) - extracted to standalone bundle
 *******************************************************************************/
package org.openscada.ui.breadcrumbs;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;

/**
 * Breadcrumb base class. It creates the breadcrumb viewer and manages its
 * activation.
 * <p>
 * Clients must implement the abstract methods.
 * </p>
 * 
 * @since 3.5
 */
public abstract class AbstractBreadcrumb {

	private BreadcrumbViewer fBreadcrumbViewer;

	private boolean fHasFocus;

	private Composite fComposite;

	private Listener fDisplayFocusListener;
	private Listener fDisplayKeyListener;

	public AbstractBreadcrumb() {
	}

	/**
	 * The active element of the editor.
	 * 
	 * @return the active element of the editor, or <b>null</b> if none
	 */
	protected abstract Object getCurrentInput();

	/**
	 * Create and configure the viewer used to display the parent chain.
	 * 
	 * @param parent
	 *            the parent composite
	 * @return the viewer
	 */
	protected abstract BreadcrumbViewer createViewer(Composite parent);

	/**
	 * Open the element in a new editor if possible.
	 * 
	 * @param selection
	 *            element the element to open
	 * @return true if the element could be opened
	 */
	protected abstract boolean open(ISelection selection);

	/**
	 * The breadcrumb has been activated. Implementors must retarget the editor
	 * actions to the breadcrumb aware actions.
	 */
	protected abstract void activateBreadcrumb();

	/**
	 * The breadcrumb has been deactivated. Implementors must retarget the
	 * breadcrumb actions to the editor actions.
	 */
	protected abstract void deactivateBreadcrumb();

	/**
	 * Returns the selection provider for this breadcrumb.
	 * 
	 * @return the selection provider for this breadcrumb
	 */
	public ISelectionProvider getSelectionProvider() {
		return this.fBreadcrumbViewer;
	}

	/**
	 * Set the input of the breadcrumb to the given element
	 * 
	 * @param element
	 *            the input element can be <code>null</code>
	 */
	public void setInput(final Object element) {
		if (element == null || this.fBreadcrumbViewer == null
				|| this.fBreadcrumbViewer.getControl().isDisposed()) {
			return;
		}

		final Object input = this.fBreadcrumbViewer.getInput();
		if (input == element || element.equals(input)) {
			refresh();
			return;
		}

		this.fBreadcrumbViewer.setInput(element);
	}

	protected void refresh() {
		if (!this.fBreadcrumbViewer.getControl().isDisposed()) {
			this.fBreadcrumbViewer.refresh();
		}
	}

	/**
	 * Activates the breadcrumb. This sets the keyboard focus inside this
	 * breadcrumb and retargets the editor actions.
	 */
	public void activate() {
		if (this.fBreadcrumbViewer.getSelection().isEmpty()) {
			this.fBreadcrumbViewer.setSelection(new StructuredSelection(
					this.fBreadcrumbViewer.getInput()));
		}
		this.fBreadcrumbViewer.setFocus();
	}

	/**
	 * A breadcrumb is active if it either has the focus or another workbench
	 * part has the focus and the breadcrumb had the focus before the other
	 * workbench part was made active.
	 * 
	 * @return <code>true</code> if this breadcrumb is active
	 */
	public boolean isActive() {
		return true;
	}

	/**
	 * Create breadcrumb content.
	 * 
	 * @param parent
	 *            the parent of the content
	 * @return the control containing the created content
	 */
	public Control createContent(final Composite parent) {
		Assert.isTrue(this.fComposite == null,
				"Content must only be created once."); //$NON-NLS-1$

		final boolean rtl = (parent.getShell().getStyle() & SWT.RIGHT_TO_LEFT) != 0;
		// boolean rtl = true;

		this.fComposite = new Composite(parent, rtl ? SWT.RIGHT_TO_LEFT
				: SWT.NONE);
		final GridData data = new GridData(SWT.FILL, SWT.TOP, true, false);
		this.fComposite.setLayoutData(data);
		final GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		gridLayout.verticalSpacing = 0;
		gridLayout.horizontalSpacing = 0;
		this.fComposite.setLayout(gridLayout);

		this.fDisplayFocusListener = new Listener() {
			@Override
			public void handleEvent(final Event event) {
				if (AbstractBreadcrumb.this.fComposite.isDisposed()) {
					return;
				}

				if (isBreadcrumbEvent(event)) {
					if (AbstractBreadcrumb.this.fHasFocus) {
						return;
					}

					focusGained();
				} else {
					if (!AbstractBreadcrumb.this.fHasFocus) {
						return;
					}

					focusLost();
				}
			}
		};
		Display.getCurrent().addFilter(SWT.FocusIn, this.fDisplayFocusListener);

		this.fBreadcrumbViewer = createViewer(this.fComposite);

		this.fBreadcrumbViewer
				.addDoubleClickListener(new IDoubleClickListener() {
					@Override
					public void doubleClick(final DoubleClickEvent event) {
						final Object element = ((IStructuredSelection) event
								.getSelection()).getFirstElement();
						if (element == null) {
							return;
						}

						final BreadcrumbItem item = (BreadcrumbItem) AbstractBreadcrumb.this.fBreadcrumbViewer
								.doFindItem(element);
						if (item == null) {
							return;
						}
						item.openDropDownMenu();
					}
				});

		this.fBreadcrumbViewer.addOpenListener(new IOpenListener() {
			@Override
			public void open(final OpenEvent event) {
				doOpen(event.getSelection());
			}
		});

		return this.fComposite;
	}

	/**
	 * Dispose all resources hold by this breadcrumb.
	 */
	public void dispose() {
		if (this.fDisplayFocusListener != null) {
			Display.getDefault().removeFilter(SWT.FocusIn,
					this.fDisplayFocusListener);
		}
		deinstallDisplayListeners();
	}

	/**
	 * Either reveal the selection in the editor or open the selection in a new
	 * editor. If both fail open the child pop up of the selected element.
	 * 
	 * @param selection
	 *            the selection to open
	 */
	private void doOpen(final ISelection selection) {
		if (open(selection)) {
			this.fBreadcrumbViewer.setInput(getCurrentInput());
		}
	}

	/**
	 * Focus has been transfered into the breadcrumb.
	 */
	private void focusGained() {
		if (this.fHasFocus) {
			focusLost();
		}

		this.fHasFocus = true;

		installDisplayListeners();

		activateBreadcrumb();
	}

	/**
	 * Focus has been revoked from the breadcrumb.
	 */
	private void focusLost() {
		this.fHasFocus = false;

		deinstallDisplayListeners();

		deactivateBreadcrumb();
	}

	/**
	 * Installs all display listeners.
	 */
	private void installDisplayListeners() {
		// Sanity check
		deinstallDisplayListeners();

		this.fDisplayKeyListener = new Listener() {
			@Override
			public void handleEvent(final Event event) {
				if (event.keyCode != SWT.ESC) {
					return;
				}

				if (!isBreadcrumbEvent(event)) {
					return;
				}
			}
		};
		Display.getDefault().addFilter(SWT.KeyDown, this.fDisplayKeyListener);
	}

	/**
	 * Removes all previously installed display listeners.
	 */
	private void deinstallDisplayListeners() {
		if (this.fDisplayKeyListener != null) {
			Display.getDefault().removeFilter(SWT.KeyDown,
					this.fDisplayKeyListener);
			this.fDisplayKeyListener = null;
		}
	}

	/**
	 * Tells whether the given event was issued inside the breadcrumb viewer's
	 * control.
	 * 
	 * @param event
	 *            the event to inspect
	 * @return <code>true</code> if event was generated by a breadcrumb child
	 */
	private boolean isBreadcrumbEvent(final Event event) {
		if (this.fBreadcrumbViewer == null) {
			return false;
		}

		final Widget item = event.widget;
		if (!(item instanceof Control)) {
			return false;
		}

		final Shell dropDownShell = this.fBreadcrumbViewer.getDropDownShell();
		if (dropDownShell != null && isChild((Control) item, dropDownShell)) {
			return true;
		}

		return isChild((Control) item, this.fBreadcrumbViewer.getControl());
	}

	private boolean isChild(final Control child, final Control parent) {
		if (child == null) {
			return false;
		}

		if (child == parent) {
			return true;
		}

		return isChild(child.getParent(), parent);
	}
}
