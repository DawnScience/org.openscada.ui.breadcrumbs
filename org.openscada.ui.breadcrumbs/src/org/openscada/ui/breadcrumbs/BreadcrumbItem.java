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

import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Shell;

/**
 * An item in a breadcrumb viewer.
 * <p>
 * The item shows a label and an image. It also has the ability to expand, that
 * is to open a drop down menu.
 * </p>
 * <p>
 * The drop down allows to select any child of the items input element. The item
 * shows the label and icon of its data element, if any.
 * </p>
 * 
 * @since 3.5
 */
class BreadcrumbItem extends Item {

	private TreePath fPath;

	private final BreadcrumbViewer fParent;
	private final Composite fContainer;

	private final BreadcrumbItemDropDown fExpandBlock;
	private final BreadcrumbItemDetails fDetailsBlock;

	private boolean fIsLast;

	/**
	 * A new breadcrumb item which is shown inside the given viewer.
	 * 
	 * @param viewer
	 *            the items viewer
	 * @param parent
	 *            the container containing the item
	 */
	public BreadcrumbItem(final BreadcrumbViewer viewer, final Composite parent) {
		super(parent, SWT.NONE);

		this.fParent = viewer;

		this.fContainer = new Composite(parent, SWT.NONE);
		this.fContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false,
				false));
		final GridLayout layout = new GridLayout(2, false);
		layout.marginBottom = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 0;
		this.fContainer.setLayout(layout);

		this.fExpandBlock = new BreadcrumbItemDropDown(this, this.fContainer);
		this.fDetailsBlock = new BreadcrumbItemDetails(this, this.fContainer);
	}

	/**
	 * Returns this items viewer.
	 * 
	 * @return the viewer showing this item
	 */
	public BreadcrumbViewer getViewer() {
		return this.fParent;
	}

	/*
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	@Override
	public void dispose() {
		this.fContainer.dispose();
		super.dispose();
	}

	public TreePath getPath() {
		return this.fPath;
	}

	public void setPath(final TreePath path) {
		this.fPath = path;
	}

	/**
	 * Should this item show a text label.
	 * 
	 * @param enabled
	 *            true if it should
	 */
	void setShowText(final boolean enabled) {
		this.fDetailsBlock.setTextVisible(enabled);
	}

	/**
	 * Does this item show a text label?
	 * 
	 * @return true if it does.
	 */
	boolean isShowText() {
		return this.fDetailsBlock.isTextVisible();
	}

	/**
	 * Returns the width of this item.
	 * 
	 * @return the width of this item
	 */
	int getWidth() {
		return this.fDetailsBlock.getWidth() + this.fExpandBlock.getWidth() + 2;
	}

	/**
	 * Sets whether this item has to be marked as selected or not.
	 * 
	 * @param selected
	 *            true if marked as selected
	 */
	void setSelected(final boolean selected) {
		this.fDetailsBlock.setSelected(selected);
	}

	/**
	 * Sets whether this item has the keyboard focus.
	 * 
	 * @param state
	 *            <code>true</code> if it has focus, <code>false</code>
	 *            otherwise
	 */
	void setFocus(final boolean state) {
		this.fDetailsBlock.setFocus(state);
	}

	/**
	 * Returns whether this item has the keyboard focus.
	 * 
	 * @return <code>true</code> if this item has the keyboard focus
	 */
	boolean hasFocus() {
		return this.fDetailsBlock.hasFocus();
	}

	/**
	 * Set whether this is the last item in the breadcrumb item chain or not.
	 * 
	 * @param isLast
	 *            <code>true</code> if this is the last item, <code>false</code>
	 *            otherwise
	 */
	void setIsLastItem(final boolean isLast) {
		this.fIsLast = isLast;

		final GridData data = (GridData) this.fContainer.getLayoutData();
		data.grabExcessHorizontalSpace = isLast;
	}

	/**
	 * Expand this item, shows the drop down menu.
	 */
	void openDropDownMenu() {
		this.fExpandBlock.showMenu();
	}

	/**
	 * @return true if this item is expanded
	 */
	boolean isMenuShown() {
		return this.fExpandBlock.isMenuShown();
	}

	/**
	 * Returns the drop down shell.
	 * 
	 * @return the shell of the drop down if shown, <code>null</code> otherwise
	 */
	Shell getDropDownShell() {
		return this.fExpandBlock.getDropDownShell();
	}

	/**
	 * Returns the bounds of this item.
	 * 
	 * @return the bounds of this item
	 */
	public Rectangle getBounds() {
		return this.fContainer.getBounds();
	}

	/**
	 * Set the tool tip of the item to the given text.
	 * 
	 * @param text
	 *            the tool tip for the item
	 */
	public void setToolTip(final String text) {
		this.fDetailsBlock.setToolTip(text);
	}

	/*
	 * @see org.eclipse.swt.widgets.Item#setText(java.lang.String)
	 */
	@Override
	public void setText(final String string) {
		super.setText(string);
		this.fDetailsBlock.setText(string);

		// more or less space might be required for the label
		if (this.fIsLast) {
			this.fContainer.layout(true, true);
		}
	}

	/*
	 * @see
	 * org.eclipse.swt.widgets.Item#setImage(org.eclipse.swt.graphics.Image)
	 */
	@Override
	public void setImage(final Image image) {
		super.setImage(image);
		this.fDetailsBlock.setImage(image);
	}
}
