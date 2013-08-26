package com.baselet.gwt.client.view;

import com.baselet.control.NewGridElementConstants;
import com.baselet.element.GridElement;
import com.baselet.gwt.client.element.ElementFactory;
import com.baselet.gwt.client.view.widgets.PropertiesTextArea;

public class DrawPanelDiagram extends DrawPanel {

	public DrawPanelDiagram(MainView mainView, PropertiesTextArea propertiesPanel) {
		super(mainView, propertiesPanel);
	}

	@Override
	public void onDoubleClick(GridElement ge) {
		if (ge != null) {
			GridElement e = ElementFactory.create(ge, getDiagram());
			e.setLocationDifference(NewGridElementConstants.DEFAULT_GRID_SIZE, NewGridElementConstants.DEFAULT_GRID_SIZE);
			commandInvoker.addElements(this, e);
		}
	}

}