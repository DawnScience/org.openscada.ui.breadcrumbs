/*******************************************************************************
 * Copyright (c) 2009, 2012 Wind River Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *     IBM Corporation - ongoing bug fixes and enhancements
 *     Jens Reimann (TH4 SYSTEMS GmbH) - extracted to standalone bundle
 *******************************************************************************/
package org.openscada.ui.breadcrumbs;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.progress.UIJob;
import org.openscada.ui.breadcrumbs.internal.BreadcrumbsPlugin;

/**
 * A breadcrumb drop-down which shows a tree viewer. It implements mouse and key
 * listeners to handle selection and expansion behavior of the viewer. This
 * class needs to be extended to implement
 * {@link #createTreeViewer(Composite, int, TreePath)} to instantiate the
 * concrete {@link TreeViewer} object.
 * 
 * @since 3.5
 */
public abstract class TreeViewerDropDown {

	/**
	 * Tells whether this class is in debug mode.
	 */
	private static boolean DEBUG = BreadcrumbsPlugin.DEBUG
			&& "true".equalsIgnoreCase(Platform.getDebugOption("/org.openscada.ui.breadcrumbs/debug/breadcrumb")); //$NON-NLS-1$//$NON-NLS-2$

	/**
	 * Delay to control scrolling when the mouse pointer reaches the edge of the
	 * tree viewer.
	 */
	private static long MOUSE_MOVE_SCROLL_DELAY = 500;

	/**
	 * The breadcrumb site in which the viewer is created.
	 */
	private IBreadcrumbDropDownSite fDropDownSite;

	/**
	 * The tree viewer.
	 */
	private TreeViewer fDropDownViewer;

	/**
	 * Creates the viewer and installs the listeners.
	 * 
	 * @param composite
	 *            Parent control of the viewer.
	 * @param site
	 *            Breadcrumb site for the viewer.
	 * @param path
	 *            Path to the element for which the drop-down is being opened.
	 * @return The control created for the viewer.
	 */
	public Control createDropDown(final Composite composite,
			final IBreadcrumbDropDownSite site, final TreePath path) {

		this.fDropDownSite = site;
		this.fDropDownViewer = createTreeViewer(composite, SWT.SINGLE
				| SWT.H_SCROLL | SWT.V_SCROLL, path);

		this.fDropDownViewer.addOpenListener(new IOpenListener() {
			@Override
			public void open(final OpenEvent event) {
				if (DEBUG) {
					System.out
							.println("BreadcrumbItemDropDown.showMenu()$treeViewer>open"); //$NON-NLS-1$
				}

				openElement(event.getSelection());
			}
		});

		final Tree tree = this.fDropDownViewer.getTree();

		tree.addMouseListener(new MouseListener() {
			@Override
			public void mouseUp(final MouseEvent e) {
				if (DEBUG) {
					System.out
							.println("BreadcrumbItemDropDown.showMenu()$treeViewer>mouseUp"); //$NON-NLS-1$
				}

				if (e.button != 1) {
					return;
				}

				if ((OpenStrategy.getOpenMethod() & OpenStrategy.SINGLE_CLICK) != 0) {
					return;
				}

				TreeItem item = tree.getItem(new Point(e.x, e.y));
				if (item == null) {
					return;
				}

				final List pathElements = new LinkedList();
				while (item != null) {
					final Object data = item.getData();
					if (data == null) {
						return;
					}
					pathElements.add(0, data);
					item = item.getParentItem();
				}

				openElement(new TreeSelection(new TreePath(pathElements
						.toArray())));
			}

			@Override
			public void mouseDown(final MouseEvent e) {
			}

			@Override
			public void mouseDoubleClick(final MouseEvent e) {
			}
		});

		tree.addMouseMoveListener(new MouseMoveListener() {
			TreeItem fLastItem = null;
			long fLastScrollTime = 0;

			@Override
			public void mouseMove(final MouseEvent e) {
				if (tree.equals(e.getSource())) {
					final Object o = tree.getItem(new Point(e.x, e.y));
					if (this.fLastItem == null ^ o == null) {
						tree.setCursor(o == null ? null : tree.getDisplay()
								.getSystemCursor(SWT.CURSOR_HAND));
					}
					if (o instanceof TreeItem) {
						final TreeItem currentItem = (TreeItem) o;
						if (!o.equals(this.fLastItem)) {
							this.fLastItem = (TreeItem) o;
							tree.setSelection(new TreeItem[] { this.fLastItem });
						} else if (System.currentTimeMillis() > this.fLastScrollTime
								+ MOUSE_MOVE_SCROLL_DELAY) {
							if (e.y < tree.getItemHeight() / 4) {
								// Scroll up
								if (currentItem.getParentItem() == null) {
									final int index = tree
											.indexOf((TreeItem) o);
									if (index < 1) {
										return;
									}

									this.fLastItem = tree.getItem(index - 1);
									tree.setSelection(new TreeItem[] { this.fLastItem });
								} else {
									final Point p = tree.toDisplay(e.x, e.y);
									final Item item = TreeViewerDropDown.this.fDropDownViewer
											.scrollUp(p.x, p.y);
									this.fLastScrollTime = System
											.currentTimeMillis();
									if (item instanceof TreeItem) {
										this.fLastItem = (TreeItem) item;
										tree.setSelection(new TreeItem[] { this.fLastItem });
									}
								}
							} else if (e.y > tree.getBounds().height
									- tree.getItemHeight() / 4) {
								// Scroll down
								if (currentItem.getParentItem() == null) {
									final int index = tree
											.indexOf((TreeItem) o);
									if (index >= tree.getItemCount() - 1) {
										return;
									}

									this.fLastItem = tree.getItem(index + 1);
									tree.setSelection(new TreeItem[] { this.fLastItem });
								} else {
									final Point p = tree.toDisplay(e.x, e.y);
									final Item item = TreeViewerDropDown.this.fDropDownViewer
											.scrollDown(p.x, p.y);
									this.fLastScrollTime = System
											.currentTimeMillis();
									if (item instanceof TreeItem) {
										this.fLastItem = (TreeItem) item;
										tree.setSelection(new TreeItem[] { this.fLastItem });
									}
								}
							}
						}
					} else if (o == null) {
						this.fLastItem = null;
					}
				}
			}
		});

		tree.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(final KeyEvent e) {
				if (e.keyCode == SWT.ARROW_UP) {
					// No elements in the tree (bug 262961).
					if (tree.getItemCount() == 0) {
						TreeViewerDropDown.this.fDropDownSite.close();
						return;
					}

					final TreeItem[] selection = tree.getSelection();
					if (selection.length != 1) {
						return;
					}

					final int selectionIndex = tree.indexOf(selection[0]);
					if (selectionIndex != 0) {
						return;
					}

					TreeViewerDropDown.this.fDropDownSite.close();
				}
			}

			@Override
			public void keyReleased(final KeyEvent e) {
			}
		});

		this.fDropDownViewer.addTreeListener(new ITreeViewerListener() {
			@Override
			public void treeCollapsed(final TreeExpansionEvent event) {
			}

			@Override
			public void treeExpanded(final TreeExpansionEvent event) {
				tree.setRedraw(false);
				new UIJob(tree.getDisplay(), "") {
					{
						setSystem(true);
					}

					@Override
					public IStatus runInUIThread(final IProgressMonitor monitor) {
						if (!tree.isDisposed()) {
							try {
								TreeViewerDropDown.this.fDropDownSite
										.updateSize();
							} finally {
								tree.setRedraw(true);
							}
						}
						return Status.OK_STATUS;
					}
				}.schedule();
			}

		});

		return tree;
	}

	/**
	 * Creates and returns the tree viewer.
	 * 
	 * @param composite
	 *            Parent control of the viewer.
	 * @param style
	 *            Style flags to use in creating the tree viewer.
	 * @param path
	 *            Path to the element for which the drop-down is being opened.
	 * @return The newly created tree viewer.
	 */
	protected abstract TreeViewer createTreeViewer(Composite composite,
			int style, TreePath path);

	/**
	 * Called when the given element was selected in the viewer. It causes the
	 * breadcrumb viewer to fire an opened event. If the viewer loses focus as a
	 * result of the open operation, then the drop-down is closed. Otherwise the
	 * selected element is expanded.
	 * 
	 * @param selection
	 *            The selection to open.
	 */
	protected void openElement(final ISelection selection) {
		if (selection == null || !(selection instanceof ITreeSelection)
				|| selection.isEmpty()) {
			return;
		}

		// This might or might not open an editor
		this.fDropDownSite.notifySelection(selection);

		final Tree tree = this.fDropDownViewer.getTree();

		final boolean treeHasFocus = !tree.isDisposed()
				&& tree.isFocusControl();

		if (DEBUG) {
			System.out.println("    isDisposed: " + tree.isDisposed()); //$NON-NLS-1$
			System.out
					.println("    shell hasFocus: " + (!tree.isDisposed() && tree.isFocusControl())); //$NON-NLS-1$
			System.out.println("    tree hasFocus: " + treeHasFocus); //$NON-NLS-1$
		}

		if (tree.isDisposed()) {
			return;
		}

		if (!treeHasFocus) {
			this.fDropDownSite.close();
			return;
		}

		toggleExpansionState(((ITreeSelection) selection).getPaths()[0]);
	}

	private void toggleExpansionState(final TreePath path) {
		final Tree tree = this.fDropDownViewer.getTree();
		if (this.fDropDownViewer.getExpandedState(path)) {
			this.fDropDownViewer.collapseToLevel(path, 1);
		} else {
			tree.setRedraw(false);
			try {
				this.fDropDownViewer.expandToLevel(path, 1);
				this.fDropDownSite.updateSize();
			} finally {
				tree.setRedraw(true);
			}
		}
	}

}
