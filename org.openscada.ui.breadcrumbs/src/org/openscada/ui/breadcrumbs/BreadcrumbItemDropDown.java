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
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.util.Geometry;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Widget;
import org.openscada.ui.breadcrumbs.internal.BreadcrumbsPlugin;

/**
 * The part of the breadcrumb item with the drop down menu.
 * 
 * @since 3.5
 */
class BreadcrumbItemDropDown implements IBreadcrumbDropDownSite {

	/**
	 * Tells whether this class is in debug mode.
	 */
	private static boolean DEBUG = BreadcrumbsPlugin.DEBUG
			&& "true".equalsIgnoreCase(Platform.getDebugOption("org.eclipse.debug.ui/debug/breadcrumb")); //$NON-NLS-1$//$NON-NLS-2$

	private static final boolean IS_MAC_WORKAROUND = "carbon".equals(SWT.getPlatform()); //$NON-NLS-1$

	/**
	 * An arrow image descriptor. The images color is related to the list fore-
	 * and background color. This makes the arrow visible even in high contrast
	 * mode. If <code>ltr</code> is true the arrow points to the right,
	 * otherwise it points to the left.
	 */
	private final class AccessibelArrowImage extends CompositeImageDescriptor {

		private final static int ARROW_SIZE = 5;

		private final boolean fLTR;

		public AccessibelArrowImage(final boolean ltr) {
			this.fLTR = ltr;
		}

		/*
		 * @see
		 * org.eclipse.jface.resource.CompositeImageDescriptor#drawCompositeImage
		 * (int, int)
		 */
		@Override
		protected void drawCompositeImage(final int width, final int height) {
			final Display display = BreadcrumbItemDropDown.this.fParentComposite
					.getDisplay();

			final Image image = new Image(display, ARROW_SIZE, ARROW_SIZE * 2);

			final GC gc = new GC(image);

			final Color triangle = createColor(SWT.COLOR_LIST_FOREGROUND,
					SWT.COLOR_LIST_BACKGROUND, 20, display);
			final Color aliasing = createColor(SWT.COLOR_LIST_FOREGROUND,
					SWT.COLOR_LIST_BACKGROUND, 30, display);
			gc.setBackground(triangle);

			if (this.fLTR) {
				gc.fillPolygon(new int[] { mirror(0), 0, mirror(ARROW_SIZE),
						ARROW_SIZE, mirror(0), ARROW_SIZE * 2 });
			} else {
				gc.fillPolygon(new int[] { ARROW_SIZE, 0, 0, ARROW_SIZE,
						ARROW_SIZE, ARROW_SIZE * 2 });
			}

			gc.setForeground(aliasing);
			gc.drawLine(mirror(0), 1, mirror(ARROW_SIZE - 1), ARROW_SIZE);
			gc.drawLine(mirror(ARROW_SIZE - 1), ARROW_SIZE, mirror(0),
					ARROW_SIZE * 2 - 1);

			gc.dispose();
			triangle.dispose();
			aliasing.dispose();

			final ImageData imageData = image.getImageData();
			for (int y = 1; y < ARROW_SIZE; y++) {
				for (int x = 0; x < y; x++) {
					imageData.setAlpha(mirror(x), y, 255);
				}
			}
			for (int y = 0; y < ARROW_SIZE; y++) {
				for (int x = 0; x <= y; x++) {
					imageData.setAlpha(mirror(x), ARROW_SIZE * 2 - y - 1, 255);
				}
			}

			final int offset = this.fLTR ? 0 : -1;
			drawImage(imageData, width / 2 - ARROW_SIZE / 2 + offset, height
					/ 2 - ARROW_SIZE - 1);

			image.dispose();
		}

		private int mirror(final int x) {
			if (this.fLTR) {
				return x;
			}

			return ARROW_SIZE - x - 1;
		}

		/*
		 * @see org.eclipse.jface.resource.CompositeImageDescriptor#getSize()
		 */
		@Override
		protected Point getSize() {
			return new Point(10, 16);
		}

		private Color createColor(final int color1, final int color2,
				final int ratio, final Display display) {
			final RGB rgb1 = display.getSystemColor(color1).getRGB();
			final RGB rgb2 = display.getSystemColor(color2).getRGB();

			final RGB blend = BreadcrumbViewer.blend(rgb2, rgb1, ratio);

			return new Color(display, blend);
		}
	}

	// Workaround for bug 258196: set the minimum size to 500 because on Linux
	// the size is not adjusted correctly in a virtual tree.
	private static final int DROP_DOWN_MIN_WIDTH = 500;
	private static final int DROP_DOWN_MAX_WIDTH = 501;

	private static final int DROP_DOWN_DEFAULT_MIN_HEIGHT = 300;
	private static final int DROP_DOWN_DEFAULT_MAX_HEIGHT = 500;

	private static final String DIALOG_SETTINGS = "BreadcrumbItemDropDown"; //$NON-NLS-1$
	private static final String DIALOG_HEIGHT = "height"; //$NON-NLS-1$

	private final BreadcrumbItem fParent;
	private final Composite fParentComposite;
	private final ToolBar fToolBar;

	private boolean fMenuIsShown;
	private boolean fEnabled;
	private Shell fShell;
	private boolean fIsResizingProgrammatically;

	public BreadcrumbItemDropDown(final BreadcrumbItem parent,
			final Composite composite) {
		this.fParent = parent;
		this.fParentComposite = composite;
		this.fMenuIsShown = false;
		this.fEnabled = true;

		this.fToolBar = new ToolBar(composite, SWT.FLAT);
		this.fToolBar.setLayoutData(new GridData(SWT.END, SWT.CENTER, false,
				false));
		this.fToolBar.getAccessible().addAccessibleListener(
				new AccessibleAdapter() {
					@Override
					public void getName(final AccessibleEvent e) {
						e.result = BreadcrumbMessages.BreadcrumbItemDropDown_showDropDownMenu_action_toolTip;
					}
				});
		final ToolBarManager manager = new ToolBarManager(this.fToolBar);

		final Action showDropDownMenuAction = new Action(null, SWT.NONE) {
			@Override
			public void run() {
				Shell shell = BreadcrumbItemDropDown.this.fParent
						.getDropDownShell();
				if (shell != null) {
					return;
				}

				shell = BreadcrumbItemDropDown.this.fParent.getViewer()
						.getDropDownShell();
				if (shell != null && !shell.isDisposed()) {
					shell.close();
				}

				showMenu();

				BreadcrumbItemDropDown.this.fShell.setFocus();
			}
		};

		showDropDownMenuAction.setImageDescriptor(new AccessibelArrowImage(
				isLeft()));
		showDropDownMenuAction
				.setToolTipText(BreadcrumbMessages.BreadcrumbItemDropDown_showDropDownMenu_action_toolTip);
		manager.add(showDropDownMenuAction);

		manager.update(true);
		if (IS_MAC_WORKAROUND) {
			manager.getControl().addMouseListener(new MouseAdapter() {
				// see also BreadcrumbItemDetails#addElementListener(Control)
				@Override
				public void mouseDown(final MouseEvent e) {
					showDropDownMenuAction.run();
				}
			});
		}
	}

	/**
	 * Return the width of this element.
	 * 
	 * @return the width of this element
	 */
	public int getWidth() {
		return this.fToolBar.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
	}

	/**
	 * Set whether the drop down menu is available.
	 * 
	 * @param enabled
	 *            true if available
	 */
	public void setEnabled(final boolean enabled) {
		this.fEnabled = enabled;

		this.fToolBar.setVisible(enabled);
	}

	/**
	 * Tells whether the menu is shown.
	 * 
	 * @return true if the menu is open
	 */
	public boolean isMenuShown() {
		return this.fMenuIsShown;
	}

	/**
	 * Returns the shell used for the drop down menu if it is shown.
	 * 
	 * @return the drop down shell or <code>null</code>
	 */
	public Shell getDropDownShell() {
		if (!isMenuShown()) {
			return null;
		}

		return this.fShell;
	}

	/**
	 * Opens the drop down menu.
	 */
	public void showMenu() {
		if (DEBUG) {
			System.out.println("BreadcrumbItemDropDown.showMenu()"); //$NON-NLS-1$
		}

		if (!this.fEnabled || this.fMenuIsShown) {
			return;
		}

		this.fMenuIsShown = true;

		this.fShell = new Shell(this.fToolBar.getShell(), SWT.RESIZE | SWT.TOOL
				| SWT.ON_TOP);
		if (DEBUG) {
			System.out.println("	creating new shell"); //$NON-NLS-1$
		}

		this.fShell.addControlListener(new ControlAdapter() {
			/*
			 * @see
			 * org.eclipse.swt.events.ControlAdapter#controlResized(org.eclipse
			 * .swt.events.ControlEvent)
			 */
			@Override
			public void controlResized(final ControlEvent e) {
				if (BreadcrumbItemDropDown.this.fIsResizingProgrammatically) {
					return;
				}

				final Point size = BreadcrumbItemDropDown.this.fShell.getSize();
				getDialogSettings().put(DIALOG_HEIGHT, size.y);
			}
		});

		final GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		this.fShell.setLayout(layout);

		final Composite composite = new Composite(this.fShell, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		final GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		composite.setLayout(gridLayout);

		final TreePath path = this.fParent.getPath();

		final Control control = this.fParent.getViewer().createDropDown(
				composite, this, path);

		control.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		setShellBounds(this.fShell);
		this.fShell.setVisible(true);
		installCloser(this.fShell);
	}

	/**
	 * The closer closes the given shell when the focus is lost.
	 * 
	 * @param shell
	 *            the shell to install the closer to
	 */
	private void installCloser(final Shell shell) {
		final Listener focusListener = new Listener() {
			@Override
			public void handleEvent(final Event event) {
				final Widget focusElement = event.widget;
				final boolean isFocusBreadcrumbTreeFocusWidget = focusElement == shell
						|| focusElement instanceof Control
						&& ((Control) focusElement).getShell() == shell;
				final boolean isFocusWidgetParentShell = focusElement instanceof Control
						&& ((Control) focusElement).getShell().getParent() == shell;

				switch (event.type) {
				case SWT.FocusIn:
					if (DEBUG) {
						System.out
								.println("focusIn - is breadcrumb tree: " + isFocusBreadcrumbTreeFocusWidget); //$NON-NLS-1$
					}

					if (!isFocusBreadcrumbTreeFocusWidget
							&& !isFocusWidgetParentShell) {
						if (DEBUG) {
							System.out
									.println("==> closing shell since focus in other widget"); //$NON-NLS-1$
						}
						shell.close();
					}
					break;

				case SWT.FocusOut:
					if (DEBUG) {
						System.out
								.println("focusOut - is breadcrumb tree: " + isFocusBreadcrumbTreeFocusWidget); //$NON-NLS-1$
					}

					if (event.display.getActiveShell() == null) {
						if (DEBUG) {
							System.out
									.println("==> closing shell since event.display.getActiveShell() != shell"); //$NON-NLS-1$
						}
						shell.close();
					}
					break;

				default:
					Assert.isTrue(false);
				}
			}
		};

		final Display display = shell.getDisplay();
		display.addFilter(SWT.FocusIn, focusListener);
		display.addFilter(SWT.FocusOut, focusListener);

		final ControlListener controlListener = new ControlListener() {
			@Override
			public void controlMoved(final ControlEvent e) {
				if (!shell.isDisposed()) {
					shell.close();
				}
			}

			@Override
			public void controlResized(final ControlEvent e) {
				if (!shell.isDisposed()) {
					shell.close();
				}
			}
		};
		this.fToolBar.getShell().addControlListener(controlListener);

		shell.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				if (DEBUG) {
					System.out.println("==> shell disposed"); //$NON-NLS-1$
				}

				display.removeFilter(SWT.FocusIn, focusListener);
				display.removeFilter(SWT.FocusOut, focusListener);

				if (!BreadcrumbItemDropDown.this.fToolBar.isDisposed()) {
					BreadcrumbItemDropDown.this.fToolBar.getShell()
							.removeControlListener(controlListener);
				}
			}
		});
		shell.addShellListener(new ShellListener() {
			@Override
			public void shellActivated(final ShellEvent e) {
			}

			@Override
			public void shellClosed(final ShellEvent e) {
				if (DEBUG) {
					System.out.println("==> shellClosed"); //$NON-NLS-1$
				}

				if (!BreadcrumbItemDropDown.this.fMenuIsShown) {
					return;
				}

				BreadcrumbItemDropDown.this.fMenuIsShown = false;
			}

			@Override
			public void shellDeactivated(final ShellEvent e) {
			}

			@Override
			public void shellDeiconified(final ShellEvent e) {
			}

			@Override
			public void shellIconified(final ShellEvent e) {
			}
		});
	}

	private IDialogSettings getDialogSettings() {
		final IDialogSettings javaSettings = BreadcrumbsPlugin.getDefault()
				.getDialogSettings();
		IDialogSettings settings = javaSettings.getSection(DIALOG_SETTINGS);
		if (settings == null) {
			settings = javaSettings.addNewSection(DIALOG_SETTINGS);
		}
		return settings;
	}

	private int getMaxHeight() {
		try {
			return getDialogSettings().getInt(DIALOG_HEIGHT);
		} catch (final NumberFormatException e) {
			return DROP_DOWN_DEFAULT_MAX_HEIGHT;
		}
	}

	/**
	 * Calculates a useful size for the given shell.
	 * 
	 * @param shell
	 *            the shell to calculate the size for.
	 */
	private void setShellBounds(final Shell shell) {

		final Rectangle rect = this.fParentComposite.getBounds();
		final Rectangle toolbarBounds = this.fToolBar.getBounds();

		final Point size = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT, false);
		final int height = Math.max(Math.min(size.y, getMaxHeight()),
				DROP_DOWN_DEFAULT_MIN_HEIGHT);
		final int width = Math.max(Math.min(size.x, DROP_DOWN_MAX_WIDTH),
				DROP_DOWN_MIN_WIDTH);

		int imageBoundsX = 0;
		if (this.fParent.getImage() != null) {
			imageBoundsX = this.fParent.getImage().getImageData().width;
		}

		final Rectangle trim = this.fShell.computeTrim(0, 0, width, height);
		int x = toolbarBounds.x + toolbarBounds.width + 2 + trim.x
				- imageBoundsX;
		if (!isLeft()) {
			x += width;
		}

		int y = rect.y;
		if (isTop()) {
			y += rect.height;
		} else {
			y -= height;
		}

		Point pt = new Point(x, y);
		pt = this.fParentComposite.toDisplay(pt);

		final Rectangle monitor = getClosestMonitor(shell.getDisplay(), pt)
				.getClientArea();
		final int overlap = pt.x + width - (monitor.x + monitor.width);
		if (overlap > 0) {
			pt.x -= overlap;
		}
		if (pt.x < monitor.x) {
			pt.x = monitor.x;
		}

		shell.setLocation(pt);
		this.fIsResizingProgrammatically = true;
		try {
			shell.setSize(width, height);
		} finally {
			this.fIsResizingProgrammatically = false;
		}
	}

	/**
	 * Returns the monitor whose client area contains the given point. If no
	 * monitor contains the point, returns the monitor that is closest to the
	 * point.
	 * <p>
	 * Copied from
	 * <code>org.eclipse.jface.window.Window.getClosestMonitor(Display, Point)</code>
	 * </p>
	 * 
	 * @param display
	 *            the display showing the monitors
	 * @param point
	 *            point to find (display coordinates)
	 * @return the monitor closest to the given point
	 */
	private static Monitor getClosestMonitor(final Display display,
			final Point point) {
		int closest = Integer.MAX_VALUE;

		final Monitor[] monitors = display.getMonitors();
		Monitor result = monitors[0];

		for (int i = 0; i < monitors.length; i++) {
			final Monitor current = monitors[i];

			final Rectangle clientArea = current.getClientArea();

			if (clientArea.contains(point)) {
				return current;
			}

			final int distance = Geometry.distanceSquared(
					Geometry.centerPoint(clientArea), point);
			if (distance < closest) {
				closest = distance;
				result = current;
			}
		}

		return result;
	}

	/**
	 * Set the size of the given shell such that more content can be shown. The
	 * shell size does not exceed a user-configurable maximum.
	 * 
	 * @param shell
	 *            the shell to resize
	 */
	private void resizeShell(final Shell shell) {
		final Point size = shell.getSize();
		final int currentWidth = size.x;
		final int currentHeight = size.y;

		final int maxHeight = getMaxHeight();

		if (currentHeight >= maxHeight && currentWidth >= DROP_DOWN_MAX_WIDTH) {
			return;
		}

		final Point preferedSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT,
				true);

		int newWidth;
		if (currentWidth >= DROP_DOWN_MAX_WIDTH) {
			newWidth = currentWidth;
		} else {
			// Workaround for bug 319612: Do not resize width below the
			// DROP_DOWN_MIN_WIDTH. This can happen because the Shell.getSize()
			// is incorrectly small on Linux.
			newWidth = Math.min(
					Math.max(Math.max(preferedSize.x, currentWidth),
							DROP_DOWN_MIN_WIDTH), DROP_DOWN_MAX_WIDTH);
		}
		int newHeight;
		if (currentHeight >= maxHeight) {
			newHeight = currentHeight;
		} else {
			newHeight = Math.min(Math.max(preferedSize.y, currentHeight),
					maxHeight);
		}

		if (newHeight != currentHeight || newWidth != currentWidth) {
			shell.setRedraw(false);
			try {
				this.fIsResizingProgrammatically = true;
				shell.setSize(newWidth, newHeight);

				final Point location = shell.getLocation();
				Point newLocation = location;
				if (!isLeft()) {
					newLocation = new Point(newLocation.x
							- (newWidth - currentWidth), newLocation.y);
				}
				if (!isTop()) {
					newLocation = new Point(newLocation.x, newLocation.y
							- (newHeight - currentHeight));
				}
				if (!location.equals(newLocation)) {
					shell.setLocation(newLocation.x, newLocation.y);
				}
			} finally {
				this.fIsResizingProgrammatically = false;
				shell.setRedraw(true);
			}
		}
	}

	/**
	 * Tells whether this the breadcrumb is in LTR mode or RTL mode. Or whether
	 * the breadcrumb is on the right-side status coolbar, which has the same
	 * effect on layout.
	 * 
	 * @return <code>true</code> if the breadcrumb in left-to-right mode,
	 *         <code>false</code> otherwise
	 */
	private boolean isLeft() {
		return (this.fParentComposite.getStyle() & SWT.RIGHT_TO_LEFT) == 0
				&& (this.fParent.getViewer().getStyle() & SWT.RIGHT) == 0;
	}

	/**
	 * Tells whether this the breadcrumb is in LTR mode or RTL mode. Or whether
	 * the breadcrumb is on the right-side status coolbar, which has the same
	 * effect on layout.
	 * 
	 * @return <code>true</code> if the breadcrumb in left-to-right mode,
	 *         <code>false</code> otherwise
	 */
	private boolean isTop() {
		return (this.fParent.getViewer().getStyle() & SWT.BOTTOM) == 0;
	}

	@Override
	public void close() {
		if (this.fShell != null && !this.fShell.isDisposed()) {
			this.fShell.close();
		}
	}

	@Override
	public void notifySelection(final ISelection selection) {
		this.fParent.getViewer().fireMenuSelection(selection);
	}

	@Override
	public void updateSize() {
		if (this.fShell != null && !this.fShell.isDisposed()) {
			resizeShell(this.fShell);
		}
	}
}
