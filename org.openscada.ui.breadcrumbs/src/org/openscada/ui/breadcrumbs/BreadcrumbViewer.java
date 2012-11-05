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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreePathContentProvider;
import org.eclipse.jface.viewers.ITreePathLabelProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.ViewerLabel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
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
 * A breadcrumb viewer shows a the parent chain of its input element in a list.
 * Each breadcrumb item of that list can be expanded and a sibling of the
 * element presented by the breadcrumb item can be selected.
 * <p>
 * Content providers for breadcrumb viewers must implement the
 * <code>ITreePathContentProvider</code> interface.
 * </p>
 * <p>
 * Label providers for breadcrumb viewers must implement the
 * <code>ITreePathLabelProvider</code> interface.
 * </p>
 * 
 * @since 3.5
 */
public abstract class BreadcrumbViewer extends StructuredViewer {

	private static final boolean IS_GTK = "gtk".equals(SWT.getPlatform()); //$NON-NLS-1$

	private final int fStyle;
	private final Composite fContainer;
	private final ArrayList fBreadcrumbItems;
	private final ListenerList fMenuListeners;

	private Image fGradientBackground;
	private BreadcrumbItem fSelectedItem;

	/**
	 * Create a new <code>BreadcrumbViewer</code>.
	 * <p>
	 * Style is one of:
	 * <ul>
	 * <li>SWT.NONE</li>
	 * <li>SWT.VERTICAL</li>
	 * <li>SWT.HORIZONTAL</li>
	 * <li>SWT.BOTTOM</li>
	 * <li>SWT.RIGHT</li>
	 * </ul>
	 * 
	 * @param parent
	 *            the container for the viewer
	 * @param style
	 *            the style flag used for this viewer
	 */
	public BreadcrumbViewer(final Composite parent, final int style) {
		this.fStyle = style;
		this.fBreadcrumbItems = new ArrayList();
		this.fMenuListeners = new ListenerList();

		this.fContainer = new Composite(parent, SWT.NONE);
		final GridData layoutData = new GridData(SWT.FILL, SWT.TOP, true, false);
		this.fContainer.setLayoutData(layoutData);
		this.fContainer.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(final TraverseEvent e) {
				e.doit = true;
			}
		});
		this.fContainer.setBackgroundMode(SWT.INHERIT_DEFAULT);

		this.fContainer.addListener(SWT.Resize, new Listener() {
			@Override
			public void handleEvent(final Event event) {
				final int height = BreadcrumbViewer.this.fContainer
						.getClientArea().height;

				if (BreadcrumbViewer.this.fGradientBackground == null
						|| BreadcrumbViewer.this.fGradientBackground
								.getBounds().height != height) {
					final Image image = height == 0 ? null
							: createGradientImage(height, event.display);
					BreadcrumbViewer.this.fContainer.setBackgroundImage(image);

					if (BreadcrumbViewer.this.fGradientBackground != null) {
						BreadcrumbViewer.this.fGradientBackground.dispose();
					}
					BreadcrumbViewer.this.fGradientBackground = image;
				}
			}
		});

		hookControl(this.fContainer);

		int columns = 1000;
		if ((SWT.VERTICAL & style) != 0) {
			columns = 2;
		}

		final GridLayout gridLayout = new GridLayout(columns, false);
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		gridLayout.verticalSpacing = 0;
		gridLayout.horizontalSpacing = 0;
		this.fContainer.setLayout(gridLayout);

		this.fContainer.addListener(SWT.Resize, new Listener() {
			@Override
			public void handleEvent(final Event event) {
				updateSize();
				BreadcrumbViewer.this.fContainer.layout(true, true);
			}
		});
	}

	int getStyle() {
		return this.fStyle;
	}

	/**
	 * Configure the given drop down viewer. The given input is used for the
	 * viewers input. Clients must at least set the label and the content
	 * provider for the viewer.
	 * 
	 * @param viewer
	 *            the viewer to configure
	 * @param input
	 *            the input for the viewer
	 */
	protected abstract Control createDropDown(Composite parent,
			IBreadcrumbDropDownSite site, TreePath path);

	/*
	 * @see org.eclipse.jface.viewers.Viewer#getControl()
	 */
	@Override
	public Control getControl() {
		return this.fContainer;
	}

	/*
	 * @see org.eclipse.jface.viewers.StructuredViewer#reveal(java.lang.Object)
	 */
	@Override
	public void reveal(final Object element) {
		// all elements are always visible
	}

	/**
	 * Transfers the keyboard focus into the viewer.
	 */
	public void setFocus() {
		this.fContainer.setFocus();

		if (this.fSelectedItem != null) {
			this.fSelectedItem.setFocus(true);
		} else {
			if (this.fBreadcrumbItems.size() == 0) {
				return;
			}

			final BreadcrumbItem item = (BreadcrumbItem) this.fBreadcrumbItems
					.get(this.fBreadcrumbItems.size() - 1);
			item.setFocus(true);
		}
	}

	/**
	 * @return true if any of the items in the viewer is expanded
	 */
	public boolean isDropDownOpen() {
		for (int i = 0, size = this.fBreadcrumbItems.size(); i < size; i++) {
			final BreadcrumbItem item = (BreadcrumbItem) this.fBreadcrumbItems
					.get(i);
			if (item.isMenuShown()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * The shell used for the shown drop down or <code>null</code> if no drop
	 * down is shown at the moment.
	 * 
	 * @return the drop downs shell or <code>null</code>
	 */
	public Shell getDropDownShell() {
		for (int i = 0, size = this.fBreadcrumbItems.size(); i < size; i++) {
			final BreadcrumbItem item = (BreadcrumbItem) this.fBreadcrumbItems
					.get(i);
			if (item.isMenuShown()) {
				return item.getDropDownShell();
			}
		}

		return null;
	}

	/**
	 * Add the given listener to the set of listeners which will be informed
	 * when a context menu is requested for a breadcrumb item.
	 * 
	 * @param listener
	 *            the listener to add
	 */
	public void addMenuDetectListener(final MenuDetectListener listener) {
		this.fMenuListeners.add(listener);
	}

	/**
	 * Remove the given listener from the set of menu detect listeners. Does
	 * nothing if the listener is not element of the set.
	 * 
	 * @param listener
	 *            the listener to remove
	 */
	public void removeMenuDetectListener(final MenuDetectListener listener) {
		this.fMenuListeners.remove(listener);
	}

	/*
	 * @see
	 * org.eclipse.jface.viewers.StructuredViewer#assertContentProviderType(
	 * org.eclipse.jface.viewers.IContentProvider)
	 */
	@Override
	protected void assertContentProviderType(final IContentProvider provider) {
		super.assertContentProviderType(provider);
		Assert.isTrue(provider instanceof ITreePathContentProvider);
	}

	/*
	 * @see org.eclipse.jface.viewers.Viewer#inputChanged(java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	protected void inputChanged(final Object input, final Object oldInput) {
		if (this.fContainer.isDisposed()) {
			return;
		}

		disableRedraw();
		try {
			preservingSelection(new Runnable() {
				@Override
				public void run() {
					buildItemChain(input);
				}
			});
		} finally {
			enableRedraw();
		}
	}

	/*
	 * @see
	 * org.eclipse.jface.viewers.StructuredViewer#doFindInputItem(java.lang.
	 * Object)
	 */
	@Override
	protected Widget doFindInputItem(final Object element) {
		if (element == null) {
			return null;
		}

		if (element == getInput() || element.equals(getInput())) {
			return doFindItem(element);
		}

		return null;
	}

	/*
	 * @see
	 * org.eclipse.jface.viewers.StructuredViewer#doFindItem(java.lang.Object)
	 */
	@Override
	protected Widget doFindItem(final Object element) {
		if (element == null) {
			return null;
		}

		for (int i = 0, size = this.fBreadcrumbItems.size(); i < size; i++) {
			final BreadcrumbItem item = (BreadcrumbItem) this.fBreadcrumbItems
					.get(i);
			if (item.getData() == element || element.equals(item.getData())) {
				return item;
			}
		}

		return null;
	}

	/*
	 * @see
	 * org.eclipse.jface.viewers.StructuredViewer#doUpdateItem(org.eclipse.swt
	 * .widgets.Widget, java.lang.Object, boolean)
	 */
	@Override
	protected void doUpdateItem(final Widget widget, final Object element,
			final boolean fullMap) {
		myDoUpdateItem(widget, element, fullMap);
	}

	private boolean myDoUpdateItem(final Widget widget, final Object element,
			final boolean fullMap) {
		if (widget instanceof BreadcrumbItem) {
			final BreadcrumbItem item = (BreadcrumbItem) widget;

			// remember element we are showing
			if (fullMap) {
				associate(element, item);
			} else {
				final Object data = item.getData();
				if (data != null) {
					unmapElement(data, item);
				}
				item.setData(element);
				mapElement(element, item);
			}

			refreshItem(item);
		}
		return false;
	}

	/**
	 * This implementation of getSelection() returns an instance of
	 * ITreeSelection.
	 */
	@Override
	public ISelection getSelection() {
		final Control control = getControl();
		if (control == null || control.isDisposed()) {
			return TreeSelection.EMPTY;
		}
		if (this.fSelectedItem != null) {
			final TreePath path = getTreePathFromItem(this.fSelectedItem);
			if (path != null) {
				return new TreeSelection(new TreePath[] { path });
			}
		}
		return TreeSelection.EMPTY;
	}

	protected TreePath getTreePathFromItem(final BreadcrumbItem item) {
		final List elements = new ArrayList(this.fBreadcrumbItems.size());
		for (int i = 0; i < this.fBreadcrumbItems.size(); i++) {
			elements.add(((BreadcrumbItem) this.fBreadcrumbItems.get(i))
					.getData());
			if (this.fBreadcrumbItems.get(i).equals(item)) {
				return new TreePath(elements.toArray());
			}
		}
		return null;
	}

	/*
	 * @see org.eclipse.jface.viewers.StructuredViewer#getSelectionFromWidget()
	 */
	@Override
	protected List getSelectionFromWidget() {
		if (this.fSelectedItem == null) {
			return Collections.EMPTY_LIST;
		}

		if (this.fSelectedItem.getData() == null) {
			return Collections.EMPTY_LIST;
		}

		final ArrayList result = new ArrayList();
		result.add(this.fSelectedItem.getData());
		return result;
	}

	/*
	 * @see
	 * org.eclipse.jface.viewers.StructuredViewer#internalRefresh(java.lang.
	 * Object)
	 */
	@Override
	protected void internalRefresh(final Object element) {

		disableRedraw();
		try {
			boolean layoutChanged = false;

			final BreadcrumbItem item = (BreadcrumbItem) doFindItem(element);
			if (item == null || element != null && element.equals(getInput())) {
				for (int i = 0, size = this.fBreadcrumbItems.size(); i < size; i++) {
					final BreadcrumbItem item1 = (BreadcrumbItem) this.fBreadcrumbItems
							.get(i);
					layoutChanged = refreshItem(item1) || layoutChanged;
				}
			} else {
				layoutChanged = refreshItem(item) || layoutChanged;
			}

			if (layoutChanged) {
				updateSize();
				this.fContainer.layout(true, true);
			}
		} finally {
			enableRedraw();
		}
	}

	/*
	 * @see
	 * org.eclipse.jface.viewers.StructuredViewer#setSelectionToWidget(java.
	 * util.List, boolean)
	 */
	@Override
	protected void setSelectionToWidget(List l, final boolean reveal) {
		BreadcrumbItem focusItem = null;

		// Unselect the currently selected items, and remember the focused item.
		for (int i = 0, size = this.fBreadcrumbItems.size(); i < size; i++) {
			final BreadcrumbItem item = (BreadcrumbItem) this.fBreadcrumbItems
					.get(i);
			if (item.hasFocus()) {
				focusItem = item;
			}

			item.setSelected(false);
		}

		if (l == null) {
			l = Collections.EMPTY_LIST;
		}

		// Set the new selection to items.
		this.fSelectedItem = null;
		for (final Iterator iterator = l.iterator(); iterator.hasNext();) {
			final Object element = iterator.next();
			final BreadcrumbItem item = (BreadcrumbItem) doFindItem(element);
			if (item != null) {
				item.setSelected(true);
				this.fSelectedItem = item;
				if (item == focusItem) {
					focusItem = null;
				}
			}
		}

		// If there is a new selection, and it does not overlap the old
		// selection,
		// remove the focus marker from the old focus item.
		if (this.fSelectedItem != null && focusItem != null) {
			focusItem.setFocus(false);
		}
	}

	/**
	 * Set a single selection to the given item. <code>null</code> to deselect
	 * all.
	 * 
	 * @param item
	 *            the item to select or <code>null</code>
	 */
	void selectItem(final BreadcrumbItem item) {
		if (this.fSelectedItem != null) {
			this.fSelectedItem.setSelected(false);
		}

		this.fSelectedItem = item;
		setSelectionToWidget(getSelection(), false);
		setFocus();

		fireSelectionChanged(new SelectionChangedEvent(this, getSelection()));
	}

	/**
	 * Returns the item count.
	 * 
	 * @return number of items shown in the viewer
	 */
	int getItemCount() {
		return this.fBreadcrumbItems.size();
	}

	/**
	 * Returns the item for the given item index.
	 * 
	 * @param index
	 *            the index of the item
	 * @return the item ad the given <code>index</code>
	 */
	BreadcrumbItem getItem(final int index) {
		return (BreadcrumbItem) this.fBreadcrumbItems.get(index);
	}

	/**
	 * Returns the index of the given item.
	 * 
	 * @param item
	 *            the item to search
	 * @return the index of the item or -1 if not found
	 */
	int getIndexOfItem(final BreadcrumbItem item) {
		for (int i = 0, size = this.fBreadcrumbItems.size(); i < size; i++) {
			final BreadcrumbItem pItem = (BreadcrumbItem) this.fBreadcrumbItems
					.get(i);
			if (pItem == item) {
				return i;
			}
		}

		return -1;
	}

	/**
	 * Notifies all double click listeners.
	 */
	void fireDoubleClick() {
		fireDoubleClick(new DoubleClickEvent(this, getSelection()));
	}

	/**
	 * Notifies all open listeners.
	 */
	void fireOpen() {
		fireOpen(new OpenEvent(this, getSelection()));
	}

	/**
	 * The given element was selected from a drop down menu.
	 * 
	 * @param element
	 *            the selected element
	 */
	void fireMenuSelection(final ISelection selection) {
		fireOpen(new OpenEvent(this, selection));
	}

	/**
	 * A context menu has been requested for the selected breadcrumb item.
	 * 
	 * @param event
	 *            the event issued the menu detection
	 */
	void fireMenuDetect(final MenuDetectEvent event) {
		final Object[] listeners = this.fMenuListeners.getListeners();
		for (int i = 0; i < listeners.length; i++) {
			((MenuDetectListener) listeners[i]).menuDetected(event);
		}
	}

	/**
	 * Set selection to the next or previous element if possible.
	 * 
	 * @param next
	 *            <code>true</code> if the next element should be selected,
	 *            otherwise the previous one will be selected
	 */
	void doTraverse(final boolean next) {
		if (this.fSelectedItem == null) {
			return;
		}

		final int index = this.fBreadcrumbItems.indexOf(this.fSelectedItem);
		if (next) {
			if (index == this.fBreadcrumbItems.size() - 1) {
				final BreadcrumbItem current = (BreadcrumbItem) this.fBreadcrumbItems
						.get(index);

				current.openDropDownMenu();
				current.getDropDownShell().setFocus();
			} else {
				final BreadcrumbItem nextItem = (BreadcrumbItem) this.fBreadcrumbItems
						.get(index + 1);
				selectItem(nextItem);
			}
		} else {
			if (index == 0) {
				final BreadcrumbItem root = (BreadcrumbItem) this.fBreadcrumbItems
						.get(index);
				root.openDropDownMenu();
				root.getDropDownShell().setFocus();
			} else {
				selectItem((BreadcrumbItem) this.fBreadcrumbItems
						.get(index - 1));
			}
		}
	}

	/**
	 * Generates the parent chain of the given element.
	 * 
	 * @param element
	 *            element to build the parent chain for
	 * @return the first index of an item in fBreadcrumbItems which is not part
	 *         of the chain
	 */
	private void buildItemChain(final Object input) {
		if (this.fBreadcrumbItems.size() > 0) {
			final BreadcrumbItem last = (BreadcrumbItem) this.fBreadcrumbItems
					.get(this.fBreadcrumbItems.size() - 1);
			last.setIsLastItem(false);
		}

		int index = 0;
		boolean updateLayout = false;
		if (input != null) {
			final ITreePathContentProvider contentProvider = (ITreePathContentProvider) getContentProvider();
			TreePath path = new TreePath(new Object[0]);

			// Top level elements need to be retrieved using getElements(), rest
			// using getChildren().
			Object[] children = contentProvider.getElements(input);
			Object element = children != null && children.length != 0 ? children[0]
					: null;
			while (element != null) {
				path = path.createChildPath(element);

				// All but last item are hidden if the viewer is in a vertical
				// toolbar.
				children = contentProvider.getChildren(path);
				if ((getStyle() & SWT.VERTICAL) == 0 || children == null
						|| children.length == 0) {
					updateLayout = updateOrCreateItem(index++, path, element)
							|| updateLayout;
				}

				if (children != null && children.length != 0) {
					element = children[0];
				} else {
					break;
				}

			}
		}

		BreadcrumbItem last = null;
		if (index <= this.fBreadcrumbItems.size()) {
			last = (BreadcrumbItem) this.fBreadcrumbItems.get(index - 1);
			last.setIsLastItem(true);
		}

		while (index < this.fBreadcrumbItems.size()) {
			updateLayout = true;
			final BreadcrumbItem item = (BreadcrumbItem) this.fBreadcrumbItems
					.remove(this.fBreadcrumbItems.size() - 1);
			if (item.hasFocus() && last != null) {
				last.setFocus(true);
			}
			if (item == this.fSelectedItem) {
				selectItem(null);
			}
			if (item.getData() != null) {
				unmapElement(item.getData());
			}
			item.dispose();
		}

		if (updateLayout) {
			updateSize();
			this.fContainer.layout(true, true);
		}
	}

	/**
	 * @param item
	 *            Item to refresh.
	 * @return returns whether the item's size and layout needs to be updated.
	 */
	private boolean refreshItem(final BreadcrumbItem item) {
		boolean layoutChanged = false;

		final TreePath path = getTreePathFromItem(item);

		final ViewerLabel label = new ViewerLabel(item.getText(),
				item.getImage());
		((ITreePathLabelProvider) getLabelProvider()).updateLabel(label, path);

		if (label.hasNewText()) {
			item.setText(label.getText());
			layoutChanged = true;
		}
		if (label.hasNewImage()) {
			item.setImage(label.getImage());
			layoutChanged = true;
		}
		if (label.hasNewTooltipText()) {
			item.setToolTip(label.getTooltipText());
		}
		return layoutChanged;
	}

	/**
	 * Creates or updates a breadcrumb item.
	 * 
	 * @return whether breadcrumb layout needs to be updated due to this change
	 */
	private boolean updateOrCreateItem(final int index, final TreePath path,
			final Object element) {
		BreadcrumbItem item;
		if (this.fBreadcrumbItems.size() > index) {
			item = (BreadcrumbItem) this.fBreadcrumbItems.get(index);
			if (item.getData() != null) {
				unmapElement(item.getData());
			}
		} else {
			item = new BreadcrumbItem(this, this.fContainer);
			this.fBreadcrumbItems.add(item);
		}

		boolean updateLayout = false;

		if (equals(element, item.getData())) {
			item.setPath(path);
			updateLayout = myDoUpdateItem(item, element, false);
		} else {
			item.setData(element);
			item.setPath(path);
			mapElement(element, item);
			updateLayout = refreshItem(item);
		}

		return updateLayout;
	}

	/**
	 * Update the size of the items such that all items are visible, if
	 * possible.
	 * 
	 * @return <code>true</code> if any item has changed, <code>false</code>
	 *         otherwise
	 */
	private boolean updateSize() {
		final int width = this.fContainer.getClientArea().width;

		int currentWidth = getCurrentWidth();

		boolean requiresLayout = false;

		if (currentWidth > width) {
			int index = 0;
			while (currentWidth > width
					&& index < this.fBreadcrumbItems.size() - 1) {
				final BreadcrumbItem viewer = (BreadcrumbItem) this.fBreadcrumbItems
						.get(index);
				if (viewer.isShowText()) {
					viewer.setShowText(false);
					currentWidth = getCurrentWidth();
					requiresLayout = true;
				}

				index++;
			}

		} else if (currentWidth < width) {

			int index = this.fBreadcrumbItems.size() - 1;
			while (currentWidth < width && index >= 0) {

				final BreadcrumbItem viewer = (BreadcrumbItem) this.fBreadcrumbItems
						.get(index);
				if (!viewer.isShowText()) {
					viewer.setShowText(true);
					currentWidth = getCurrentWidth();
					if (currentWidth > width) {
						viewer.setShowText(false);
						index = 0;
					} else {
						requiresLayout = true;
					}
				}

				index--;
			}
		}

		return requiresLayout;
	}

	/**
	 * Returns the current width of all items in the list.
	 * 
	 * @return the width of all items in the list
	 */
	private int getCurrentWidth() {
		int result = 0;
		for (int i = 0, size = this.fBreadcrumbItems.size(); i < size; i++) {
			final BreadcrumbItem viewer = (BreadcrumbItem) this.fBreadcrumbItems
					.get(i);
			result += viewer.getWidth();
		}

		return result;
	}

	/**
	 * Enables redrawing of the breadcrumb.
	 */
	private void enableRedraw() {
		if (IS_GTK) {
			return;
		}

		this.fContainer.setRedraw(true);
	}

	/**
	 * Disables redrawing of the breadcrumb.
	 * 
	 * <p>
	 * <strong>A call to this method must be followed by a call to
	 * {@link #enableRedraw()}</strong>
	 * </p>
	 */
	private void disableRedraw() {
		if (IS_GTK) {
			return;
		}

		this.fContainer.setRedraw(false);
	}

	/**
	 * The image to use for the breadcrumb background as specified in
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=221477
	 * 
	 * @param height
	 *            the height of the image to create
	 * @param display
	 *            the current display
	 * @return the image for the breadcrumb background
	 */
	private Image createGradientImage(final int height, final Display display) {
		final int width = 50;

		final Image result = new Image(display, width, height);

		final GC gc = new GC(result);

		final Color colorC = createColor(SWT.COLOR_WIDGET_BACKGROUND,
				SWT.COLOR_LIST_BACKGROUND, 35, display);
		final Color colorD = createColor(SWT.COLOR_WIDGET_BACKGROUND,
				SWT.COLOR_LIST_BACKGROUND, 45, display);
		final Color colorE = createColor(SWT.COLOR_WIDGET_BACKGROUND,
				SWT.COLOR_LIST_BACKGROUND, 80, display);
		final Color colorF = createColor(SWT.COLOR_WIDGET_BACKGROUND,
				SWT.COLOR_LIST_BACKGROUND, 70, display);
		final Color colorG = createColor(SWT.COLOR_WIDGET_BACKGROUND,
				SWT.COLOR_WHITE, 45, display);
		final Color colorH = createColor(SWT.COLOR_WIDGET_NORMAL_SHADOW,
				SWT.COLOR_LIST_BACKGROUND, 35, display);

		try {
			drawLine(width, 0, colorC, gc);
			drawLine(width, 1, colorC, gc);

			gc.setForeground(colorD);
			gc.setBackground(colorE);
			gc.fillGradientRectangle(0, 2, width, 2 + 8, true);

			gc.setBackground(colorE);
			gc.fillRectangle(0, 2 + 9, width, height - 4);

			drawLine(width, height - 3, colorF, gc);
			drawLine(width, height - 2, colorG, gc);
			drawLine(width, height - 1, colorH, gc);

		} finally {
			gc.dispose();

			colorC.dispose();
			colorD.dispose();
			colorE.dispose();
			colorF.dispose();
			colorG.dispose();
			colorH.dispose();
		}

		return result;
	}

	private void drawLine(final int width, final int position,
			final Color color, final GC gc) {
		gc.setForeground(color);
		gc.drawLine(0, position, width, position);
	}

	private Color createColor(final int color1, final int color2,
			final int ratio, final Display display) {
		final RGB rgb1 = display.getSystemColor(color1).getRGB();
		final RGB rgb2 = display.getSystemColor(color2).getRGB();

		final RGB blend = blend(rgb2, rgb1, ratio);

		return new Color(display, blend);
	}

	/**
	 * Blends c1 and c2 based in the provided ratio.
	 * 
	 * @param c1
	 *            first color
	 * @param c2
	 *            second color
	 * @param ratio
	 *            percentage of the first color in the blend (0-100)
	 * @return the RGB value of the blended color
	 * @since 3.1
	 */
	public static RGB blend(final RGB c1, final RGB c2, final int ratio) {
		final int r = blend(c1.red, c2.red, ratio);
		final int g = blend(c1.green, c2.green, ratio);
		final int b = blend(c1.blue, c2.blue, ratio);
		return new RGB(r, g, b);
	}

	/**
	 * Blends two primary color components based on the provided ratio.
	 * 
	 * @param v1
	 *            first component
	 * @param v2
	 *            second component
	 * @param ratio
	 *            percentage of the first component in the blend
	 * @return
	 */
	private static int blend(final int v1, final int v2, final int ratio) {
		final int b = (ratio * v1 + (100 - ratio) * v2) / 100;
		return Math.min(255, b);
	}

	/*
	 * @see
	 * org.eclipse.jface.viewers.StructuredViewer#handleDispose(org.eclipse.
	 * swt.events.DisposeEvent)
	 * 
	 * @since 3.7
	 */
	@Override
	protected void handleDispose(final DisposeEvent event) {
		if (this.fGradientBackground != null) {
			this.fGradientBackground.dispose();
			this.fGradientBackground = null;
		}

		if (this.fBreadcrumbItems != null) {
			final Iterator iterator = this.fBreadcrumbItems.iterator();
			while (iterator.hasNext()) {
				final BreadcrumbItem item = (BreadcrumbItem) iterator.next();
				item.dispose();
			}
		}

		super.handleDispose(event);
	}

}
