package com.baselet.gwt.client.view;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.baselet.control.SharedUtils;
import com.baselet.control.basics.geom.Point;
import com.baselet.control.config.SharedConfig;
import com.baselet.control.constants.SharedConstants;
import com.baselet.control.enums.Direction;
import com.baselet.element.facet.common.GroupFacet;
import com.baselet.element.interfaces.GridElement;
import com.baselet.gwt.client.element.ElementFactoryGwt;
import com.baselet.gwt.client.keyboard.Shortcut;
import com.baselet.gwt.client.view.widgets.propertiespanel.PropertiesTextArea;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyDownEvent;


public class DrawPanelDiagram extends DrawPanel {
	private List<GridElement> currentPreviewElements; //previewed elements that will be displayed while dragging from palette into actual canvas
	public DrawPanelDiagram(MainView mainView, PropertiesTextArea propertiesPanel) {
		super(mainView, propertiesPanel);
	}

	@Override
	public void onDoubleClick(GridElement ge) {
		if (ge != null) {
			GridElement e = ElementFactoryGwt.create(ge, getDiagram());
			e.setProperty(GroupFacet.KEY, null);
			e.setLocationDifference(SharedConstants.DEFAULT_GRID_SIZE, SharedConstants.DEFAULT_GRID_SIZE);
			commandInvoker.addElements(this, Arrays.asList(e));
		}
	}

	public void InitializeDisplayingPreviewElements(List<GridElement> previewElements)
	{
		if (currentPreviewElements == null)
		{
			if (previewElements != null)
				this.addGridElements(previewElements);
			this.currentPreviewElements = previewElements;
		}
		this.redraw(false);
	}

	public void UpdateDisplayingPreviewElements(int diffX, int diffY, boolean firstDrag)
	{
		if (currentPreviewElements != null)
		{
			moveElements(diffX, diffY, firstDrag, currentPreviewElements);
		}
		this.redraw(false);
	}



	@Override
	void onMouseDown(GridElement element, boolean isControlKeyDown) {
			super.onMouseDown(element, isControlKeyDown);
			//CancelDragOfPalette(); //Should not be needed anymore since selecting elements only works with left click now
			if (!isControlKeyDown || selector.getSelectedElements().size() > 0)
			propertiesPanel.setEnabled(true);
			RemoveOldPreview();

	}

	public void CancelDragOfPalette()
	{
		if (otherDrawFocusPanel instanceof DrawPanelPalette)
		((DrawPanelPalette) otherDrawFocusPanel).CancelDragNoDuplicate();
	}

	@Override
	public void onShowMenu(Point point) {
		CancelDragOfPalette();
		super.onShowMenu(point);

	}





	public void RemoveOldPreview() {
		if (currentPreviewElements != null)
			commandInvoker.removeElements(this, this.currentPreviewElements);
		currentPreviewElements = null;
	}

	public boolean currentlyDisplayingPreview()
	{
		if (currentPreviewElements == null)
			return false;
		else
			return true;
	}

}
