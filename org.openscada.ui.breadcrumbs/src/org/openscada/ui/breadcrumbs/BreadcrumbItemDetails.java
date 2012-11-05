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

import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * The label and icon part of the breadcrumb item.
 * 
 * @since 3.5
 */
class BreadcrumbItemDetails {

	private final Label fElementImage;
	private final Label fElementText;
	private final Composite fDetailComposite;
	private final BreadcrumbItem fParent;
	private final Composite fTextComposite;
	private final Composite fImageComposite;

	private boolean fTextVisible;
	private boolean fSelected;
	private boolean fHasFocus;

	public BreadcrumbItemDetails(final BreadcrumbItem parent,
			final Composite parentContainer) {
		this.fParent = parent;
		this.fTextVisible = true;

		this.fDetailComposite = new Composite(parentContainer, SWT.NONE);
		this.fDetailComposite.setLayoutData(new GridData(SWT.BEGINNING,
				SWT.CENTER, false, false));
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 0;
		this.fDetailComposite.setLayout(layout);
		addElementListener(this.fDetailComposite);

		this.fImageComposite = new Composite(this.fDetailComposite, SWT.NONE);
		this.fImageComposite.setLayoutData(new GridData(SWT.BEGINNING,
				SWT.CENTER, false, false));
		layout = new GridLayout(1, false);
		layout.marginHeight = 1;
		layout.marginWidth = 2;
		this.fImageComposite.setLayout(layout);
		this.fImageComposite.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(final PaintEvent e) {
				if (BreadcrumbItemDetails.this.fHasFocus && !isTextVisible()) {
					e.gc.drawFocus(e.x, e.y, e.width, e.height);
				}
			}
		});
		installFocusComposite(this.fImageComposite);
		addElementListener(this.fImageComposite);

		this.fElementImage = new Label(this.fImageComposite, SWT.NONE);
		GridData layoutData = new GridData(SWT.BEGINNING, SWT.CENTER, false,
				false);
		this.fElementImage.setLayoutData(layoutData);
		addElementListener(this.fElementImage);

		this.fTextComposite = new Composite(this.fDetailComposite, SWT.NONE);
		this.fTextComposite.setLayoutData(new GridData(SWT.BEGINNING,
				SWT.CENTER, false, false));
		layout = new GridLayout(1, false);
		layout.marginHeight = 2;
		layout.marginWidth = 2;
		this.fTextComposite.setLayout(layout);
		addElementListener(this.fTextComposite);
		this.fTextComposite.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(final PaintEvent e) {
				if (BreadcrumbItemDetails.this.fHasFocus && isTextVisible()) {
					e.gc.drawFocus(e.x, e.y, e.width, e.height);
				}
			}
		});
		installFocusComposite(this.fTextComposite);
		addElementListener(this.fTextComposite);

		this.fElementText = new Label(this.fTextComposite, SWT.NONE);

		layoutData = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		this.fElementText.setLayoutData(layoutData);
		addElementListener(this.fElementText);

		this.fTextComposite.getAccessible().addAccessibleListener(
				new AccessibleAdapter() {
					@Override
					public void getName(final AccessibleEvent e) {
						e.result = BreadcrumbItemDetails.this.fElementText
								.getText();
					}
				});
		this.fImageComposite.getAccessible().addAccessibleListener(
				new AccessibleAdapter() {
					@Override
					public void getName(final AccessibleEvent e) {
						e.result = BreadcrumbItemDetails.this.fElementText
								.getText();
					}
				});

		this.fDetailComposite.setTabList(new Control[] { this.fTextComposite });
	}

	/**
	 * Returns whether this element has the keyboard focus.
	 * 
	 * @return true if this element has the keyboard focus.
	 */
	public boolean hasFocus() {
		return this.fHasFocus;
	}

	/**
	 * Sets the tool tip to the given text.
	 * 
	 * @param text
	 *            the tool tip
	 */
	public void setToolTip(final String text) {
		if (isTextVisible()) {
			this.fElementText.getParent().setToolTipText(text);
			this.fElementText.setToolTipText(text);

			this.fElementImage.setToolTipText(text);
		} else {
			this.fElementText.getParent().setToolTipText(null);
			this.fElementText.setToolTipText(null);

			this.fElementImage.setToolTipText(text);
		}
	}

	/**
	 * Sets the image to the given image.
	 * 
	 * @param image
	 *            the image to use
	 */
	public void setImage(final Image image) {
		if (image != this.fElementImage.getImage()) {
			this.fElementImage.setImage(image);
		}
	}

	/**
	 * Sets the text to the given text.
	 * 
	 * @param text
	 *            the text to use
	 */
	public void setText(String text) {
		if (text == null) {
			text = ""; //$NON-NLS-1$
		}
		if (!text.equals(this.fElementText.getText())) {
			this.fElementText.setText(text);
		}
	}

	/**
	 * Returns the width of this element.
	 * 
	 * @return current width of this element
	 */
	public int getWidth() {
		int result = 2;

		if (this.fElementImage.getImage() != null) {
			result += this.fElementImage.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		}

		if (this.fTextVisible && this.fElementText.getText().length() > 0) {
			result += this.fElementText.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		}

		return result;
	}

	public void setTextVisible(final boolean enabled) {
		if (this.fTextVisible == enabled) {
			return;
		}

		this.fTextVisible = enabled;

		final GridData data = (GridData) this.fTextComposite.getLayoutData();
		data.exclude = !enabled;
		this.fTextComposite.setVisible(enabled);

		if (this.fTextVisible) {
			this.fDetailComposite
					.setTabList(new Control[] { this.fTextComposite });
		} else {
			this.fDetailComposite
					.setTabList(new Control[] { this.fImageComposite });
		}

		if (this.fHasFocus) {
			if (isTextVisible()) {
				this.fTextComposite.setFocus();
			} else {
				this.fImageComposite.setFocus();
			}
		}
		updateSelection();
	}

	/**
	 * Tells whether this item shows a text or only an image.
	 * 
	 * @return <code>true</code> if it shows a text and an image, false if it
	 *         only shows the image
	 */
	public boolean isTextVisible() {
		return this.fTextVisible;
	}

	public void setSelected(final boolean selected) {
		if (selected == this.fSelected) {
			return;
		}

		this.fSelected = selected;

		updateSelection();
	}

	public void setFocus(final boolean enabled) {
		if (enabled == this.fHasFocus) {
			return;
		}

		this.fHasFocus = enabled;
		if (this.fHasFocus) {
			if (isTextVisible()) {
				this.fTextComposite.setFocus();
			} else {
				this.fImageComposite.setFocus();
			}
		}
		updateSelection();
	}

	private void updateSelection() {
		Color background;
		Color foreground;

		if (this.fSelected) {
			background = Display.getDefault().getSystemColor(
					SWT.COLOR_LIST_SELECTION);
			foreground = Display.getDefault().getSystemColor(
					SWT.COLOR_LIST_SELECTION_TEXT);
		} else {
			foreground = null;
			background = null;
		}

		if (isTextVisible()) {
			this.fTextComposite.setBackground(background);
			this.fElementText.setBackground(background);
			this.fElementText.setForeground(foreground);

			this.fImageComposite.setBackground(null);
			this.fElementImage.setBackground(null);
		} else {
			this.fImageComposite.setBackground(background);
			this.fElementImage.setBackground(background);

			this.fTextComposite.setBackground(null);
			this.fElementText.setBackground(null);
			this.fElementText.setForeground(null);
		}

		this.fTextComposite.redraw();
		this.fImageComposite.redraw();
	}

	/**
	 * Install focus and key listeners to the given composite.
	 * 
	 * @param composite
	 *            the composite which may get focus
	 */
	private void installFocusComposite(final Composite composite) {
		composite.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(final TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_TAB_NEXT
						|| e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
					int index = BreadcrumbItemDetails.this.fParent.getViewer()
							.getIndexOfItem(BreadcrumbItemDetails.this.fParent);
					if (e.detail == SWT.TRAVERSE_TAB_NEXT) {
						index++;
					} else {
						index--;
					}

					if (index > 0
							&& index < BreadcrumbItemDetails.this.fParent
									.getViewer().getItemCount()) {
						BreadcrumbItemDetails.this.fParent.getViewer()
								.selectItem(
										BreadcrumbItemDetails.this.fParent
												.getViewer().getItem(index));
					}

					e.doit = true;
				}
			}
		});
		composite.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(final KeyEvent e) {
				final BreadcrumbViewer viewer = BreadcrumbItemDetails.this.fParent
						.getViewer();

				switch (e.keyCode) {
				case SWT.ARROW_LEFT:
					if (BreadcrumbItemDetails.this.fSelected) {
						viewer.doTraverse(false);
						e.doit = false;
					} else {
						viewer.selectItem(BreadcrumbItemDetails.this.fParent);
					}
					break;
				case SWT.ARROW_RIGHT:
					if (BreadcrumbItemDetails.this.fSelected) {
						viewer.doTraverse(true);
						e.doit = false;
					} else {
						viewer.selectItem(BreadcrumbItemDetails.this.fParent);
					}
					break;
				case SWT.ARROW_DOWN:
				case SWT.ARROW_UP:
				case SWT.KEYPAD_ADD:
					if (!BreadcrumbItemDetails.this.fSelected) {
						viewer.selectItem(BreadcrumbItemDetails.this.fParent);
					}
					openDropDown();
					e.doit = false;
					break;
				case SWT.CR:
					if (!BreadcrumbItemDetails.this.fSelected) {
						viewer.selectItem(BreadcrumbItemDetails.this.fParent);
					}
					viewer.fireOpen();
					break;
				default:
					if (e.character == ' ') {
						if (!BreadcrumbItemDetails.this.fSelected) {
							viewer.selectItem(BreadcrumbItemDetails.this.fParent);
						}
						openDropDown();
						e.doit = false;
					}
					break;
				}
			}

			private void openDropDown() {
				Shell shell = BreadcrumbItemDetails.this.fParent
						.getDropDownShell();
				if (shell == null) {
					BreadcrumbItemDetails.this.fParent.openDropDownMenu();
					shell = BreadcrumbItemDetails.this.fParent
							.getDropDownShell();
				}
				shell.setFocus();
			}

			@Override
			public void keyReleased(final KeyEvent e) {
			}
		});

		composite.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(final FocusEvent e) {
				if (!BreadcrumbItemDetails.this.fHasFocus) {
					BreadcrumbItemDetails.this.fHasFocus = true;
					updateSelection();
				}
			}

			@Override
			public void focusLost(final FocusEvent e) {
				if (BreadcrumbItemDetails.this.fHasFocus) {
					BreadcrumbItemDetails.this.fHasFocus = false;
					updateSelection();
				}
			}
		});
	}

	/**
	 * Add mouse listeners to the given control.
	 * 
	 * @param control
	 *            the control to which may be clicked
	 */
	private void addElementListener(final Control control) {
		control.addMouseListener(new MouseListener() {
			@Override
			public void mouseDoubleClick(final MouseEvent e) {
			}

			@Override
			public void mouseDown(final MouseEvent e) {
				final BreadcrumbViewer viewer = BreadcrumbItemDetails.this.fParent
						.getViewer();
				final Shell shell = BreadcrumbItemDetails.this.fParent
						.getDropDownShell();
				viewer.selectItem(BreadcrumbItemDetails.this.fParent);
				if (shell == null && e.button == 1 && e.stateMask == 0) {
					BreadcrumbItemDetails.this.fParent.getViewer()
							.fireDoubleClick();
				}
			}

			@Override
			public void mouseUp(final MouseEvent e) {
			}
		});
		control.addMenuDetectListener(new MenuDetectListener() {
			@Override
			public void menuDetected(final MenuDetectEvent e) {
				final BreadcrumbViewer viewer = BreadcrumbItemDetails.this.fParent
						.getViewer();
				viewer.selectItem(BreadcrumbItemDetails.this.fParent);
				BreadcrumbItemDetails.this.fParent.getViewer()
						.fireMenuDetect(e);
			}
		});
	}
}
