package com.baselet.gwt.client.view;

import java.util.*;

import com.baselet.control.SharedUtils;
import com.baselet.control.basics.geom.Point;
import com.baselet.control.enums.Direction;
import com.baselet.element.Selector;
import com.baselet.element.facet.common.GroupFacet;
import com.baselet.element.interfaces.Diagram;
import com.baselet.element.interfaces.GridElement;
import com.baselet.element.sticking.StickableMap;
import com.baselet.gwt.client.element.DiagramXmlParser;
import com.baselet.gwt.client.element.ElementFactoryGwt;
import com.baselet.gwt.client.view.palettes.Resources;
import com.baselet.gwt.client.view.widgets.propertiespanel.PropertiesTextArea;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.ui.ListBox;

public class DrawPanelPalette extends DrawPanel {


	private static final List<TextResource> PALETTELIST = Arrays.asList(
			Resources.INSTANCE.UML_Common_Elements(),
			Resources.INSTANCE.Custom_Drawings(),
			Resources.INSTANCE.Generic_Colors(),
			Resources.INSTANCE.Generic_Layers(),
			Resources.INSTANCE.Generic_Text_and_Alignment(),
			Resources.INSTANCE.UML_Activity(),
			Resources.INSTANCE.UML_Class(),
			Resources.INSTANCE.UML_Composite_Structure(),
			Resources.INSTANCE.UML_Package(),
			Resources.INSTANCE.UML_Sequence(),
			Resources.INSTANCE.UML_Sequence_All_in_one(),
			Resources.INSTANCE.UML_State_Machine(),
			Resources.INSTANCE.UML_Structure_and_Deployment(),
			Resources.INSTANCE.UML_Use_Case(),
			Resources.INSTANCE.Plots());
	private final Map<TextResource, Diagram> paletteCache = new HashMap<TextResource, Diagram>();

	private final ListBox paletteChooser;

	public DrawPanelPalette(MainView mainView, PropertiesTextArea propertiesPanel, final ListBox paletteChooser) {
		super(mainView, propertiesPanel);
		setDiagram(parsePalette(PALETTELIST.get(0)));
		this.paletteChooser = paletteChooser;
		for (TextResource r : PALETTELIST) {
			paletteChooser.addItem(r.getName().replaceAll("_", " "));
		}
		paletteChooser.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				setDiagram(parsePalette(PALETTELIST.get(paletteChooser.getSelectedIndex())));
				selector.deselectAll();
			}
		});
		paletteChooser.addMouseDownHandler(new MouseDownHandler() {
			@Override
			public void onMouseDown(MouseDownEvent event) {
				event.stopPropagation(); // avoid propagation of mouseclick to palette which can be under the opened listbox
			}
		});
	}

	private Diagram parsePalette(TextResource res) {
		Diagram diagram = paletteCache.get(res);
		if (diagram == null) {
			diagram = DiagramXmlParser.xmlToDiagram(res.getText());
			paletteCache.put(res, diagram);
		}
		return diagram;
	}

	@Override
	public void onDoubleClick(GridElement ge) {
		if (ge != null && !propertiesPanel.getPaletteShouldIgnoreMouseClicks()) {
			otherDrawFocusPanel.setFocus(true);
			GridElement e = ElementFactoryGwt.create(ge, otherDrawFocusPanel.getDiagram());
			e.setProperty(GroupFacet.KEY, null);
			commandInvoker.realignElementsToVisibleRect(otherDrawFocusPanel, Arrays.asList(e));
			commandInvoker.addElements(otherDrawFocusPanel, Arrays.asList(e));
		}
	}

	private final List<GridElement> draggedElements = new ArrayList<GridElement>();
	private boolean cursorWasMovedDuringDrag; //check if cursor was actually moved

	@Override
	void onMouseDown(GridElement element, boolean isControlKeyDown) {
		super.onMouseDown(element, isControlKeyDown);
		for (GridElement original : selector.getSelectedElements()) {
			draggedElements.add(ElementFactoryGwt.create(original, getDiagram()));
		}
		cursorWasMovedDuringDrag = false;
		propertiesPanel.setEnabled(false);
	}



	@Override
	public void onMouseDragEnd(GridElement gridElement, Point lastPoint) {
		//reset view if it was dragged out of bounds
		if (selector.getAllElements().size() == 0)
		{
			// todo: reset palette view to not be out-of-bounds
		}

		//reset dragged elements to origin position, if they were actually moved
		if (cursorWasMovedDuringDrag)
		{
			List<GridElement> elementsToMove = new ArrayList<GridElement>();
			for (GridElement original : selector.getSelectedElements()) {
				GridElement copy = gridElementCopyInOtherDiagram(original);
				elementsToMove.add(copy);
			}
			Selector.replaceGroupsWithNewGroups(elementsToMove, otherDrawFocusPanel.getSelector());
			commandInvoker.removeSelectedElements(this);
			commandInvoker.addElements(this, draggedElements);
			selector.deselectAll();
			if (lastPoint.getX() < 0 && resizeDirections.isEmpty()) { // mouse moved from palette to diagram -> insert elements to diagram

				commandInvoker.addElements(otherDrawFocusPanel, elementsToMove);
				if (otherDrawFocusPanel instanceof DrawPanelDiagram)
				{
					DrawPanelDiagram otherDrawDiagramFocusPanel = (DrawPanelDiagram) otherDrawFocusPanel;
					otherDrawDiagramFocusPanel.RemoveOldPreview();
				}
				propertiesPanel.setEnabled(true);
			}
		}

		draggedElements.clear();
		super.onMouseDragEnd(gridElement, lastPoint);

	}

	private GridElement gridElementCopyInOtherDiagram(GridElement original) {
		GridElement copy = ElementFactoryGwt.create(original, otherDrawFocusPanel.getDiagram());
		int verticalScrollbarDiff = otherDrawFocusPanel.scrollPanel.getVerticalScrollPosition() - scrollPanel.getVerticalScrollPosition();
		int horizontalScrollbarDiff = otherDrawFocusPanel.scrollPanel.getHorizontalScrollPosition() - scrollPanel.getHorizontalScrollPosition();
		copy.setLocationDifference(otherDrawFocusPanel.getVisibleBounds().width + horizontalScrollbarDiff, paletteChooser.getOffsetHeight() + verticalScrollbarDiff);

		copy.setRectangle(SharedUtils.realignToGrid(copy.getRectangle(), false)); // realign location to grid (width and height should not be changed)
		return copy;
	}


	@Override
	public void onMouseMoveDraggingScheduleDeferred(final Point dragStart, final int diffX, final int diffY, final GridElement draggedGridElement, final boolean isShiftKeyDown, final boolean isCtrlKeyDown, final boolean firstDrag) {
		Scheduler.get().scheduleFinally(new Scheduler.ScheduledCommand() { // scheduleDeferred is necessary for mobile (or low performance) browsers
			@Override
			public void execute() {
				onMouseMoveDragging(dragStart, diffX, diffY, draggedGridElement, isShiftKeyDown, isCtrlKeyDown, firstDrag);
			}
		});
	}

	@Override
	void onMouseMoveDragging(Point dragStart, int diffX, int diffY, GridElement draggedGridElement, boolean isShiftKeyDown, boolean isCtrlKeyDown, boolean firstDrag) {
		if (diffX != 0 || diffY != 0)
		{
			cursorWasMovedDuringDrag = true;
		}
		if (firstDrag && draggedGridElement != null) { // if draggedGridElement == null the whole diagram is dragged and nothing has to be checked for sticking
			stickablesToMove.put(draggedGridElement, getStickablesToMoveWhenElementsMove(draggedGridElement, Collections.<GridElement> emptyList()));
		}
		if (isCtrlKeyDown) {
			return; // TODO implement Lasso
		} else if (resizeDirections.isEmpty()) // dont do anything if resizing is active, elements should not be resizeable in the palette itself
		{
			if (selector.getSelectedElements().size() == 1) {
				draggedGridElement.drag(Collections.<Direction> emptySet(), diffX, diffY, getRelativePoint(dragStart, draggedGridElement), isShiftKeyDown, firstDrag, stickablesToMove.get(draggedGridElement), false);
			}
			else { // if != 1 elements are selected, move them
				moveElements(diffX, diffY, firstDrag, selector.getSelectedElements());
			}
			handlePreviewDisplay(dragStart, diffX, diffY, isShiftKeyDown, firstDrag);
		}
		redraw(false);
	}

	private void handlePreviewDisplay(Point dragStart, int diffX, int diffY, boolean isShiftKeyDown, boolean firstDrag) {
		if (otherDrawFocusPanel instanceof DrawPanelDiagram) {
			DrawPanelDiagram otherDrawDiagramFocusPanel = (DrawPanelDiagram) otherDrawFocusPanel;
			if (dragStart.getX()+diffX <= 0)
			{
				if (!otherDrawDiagramFocusPanel.currentlyDisplayingPreview())
				{
					List<GridElement> elementsToMove = new ArrayList<GridElement>();
					for (GridElement e:selector.getSelectedElements() ) {
						elementsToMove.add(gridElementCopyInOtherDiagram(e));
					}
					otherDrawDiagramFocusPanel.InitializeDisplayingPreviewElements (elementsToMove);
				} else {
					otherDrawDiagramFocusPanel.UpdateDisplayingPreviewElements(diffX, diffY, firstDrag);
				}

			} else {
				//if cursor is dragged back, preview must be removed
				otherDrawDiagramFocusPanel.RemoveOldPreview();
			}
		}


	}


	@Override
	protected StickableMap getStickablesToMoveWhenElementsMove(GridElement draggedElement, List<GridElement> elements) {
		// Moves at the palette NEVER stick
		return StickableMap.EMPTY_MAP;
	}

}
