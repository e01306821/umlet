package com.baselet.gwt.client.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baselet.control.SharedUtils;
import com.baselet.control.basics.geom.Point;
import com.baselet.control.basics.geom.Rectangle;
import com.baselet.control.constants.SharedConstants;
import com.baselet.control.enums.Direction;
import com.baselet.diagram.draw.helper.theme.Theme;
import com.baselet.diagram.draw.helper.theme.ThemeFactory;
import com.baselet.element.GridElementUtils;
import com.baselet.element.NewGridElement;
import com.baselet.element.Selector;
import com.baselet.element.facet.common.GroupFacet;
import com.baselet.element.interfaces.Diagram;
import com.baselet.element.interfaces.GridElement;
import com.baselet.element.sticking.StickableMap;
import com.baselet.gwt.client.base.Converter;
import com.baselet.gwt.client.element.DiagramXmlParser;
import com.baselet.gwt.client.element.ElementFactoryGwt;
import com.baselet.gwt.client.logging.CustomLogger;
import com.baselet.gwt.client.logging.CustomLoggerFactory;
import com.baselet.gwt.client.view.palettes.Resources;
import com.baselet.gwt.client.view.widgets.propertiespanel.PropertiesTextArea;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.dom.client.Element;
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

	private final List<GridElement> draggedElements = new ArrayList<GridElement>();
	private boolean draggingDisabled; // to disable dragging when element was dragged to properties panel

	private GridElement lastDraggedGridElement;

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
		onThemeChange();
		redraw(true);
		ThemeFactory.addListener(this);
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
			// Set Location as top left of currently visible area
			DrawPanel.snapElementToVisibleTopLeft(e, otherDrawFocusPanel);
			commandInvoker.addElements(otherDrawFocusPanel, Arrays.asList(e), getGridSize());
		}
	}

	@Override
	void onMouseDown(GridElement element, boolean isControlKeyDown) {
		super.onMouseDown(element, isControlKeyDown);
		otherDrawFocusPanel.selector.deselectAll(); // prevents selecting elements in both the diagram panel and palette
		for (GridElement original : selector.getSelectedElements()) {
			draggedElements.add(ElementFactoryGwt.create(original, getDiagram()));
		}
		draggingDisabled = false;

	}

	@Override
	public void onMouseDragEnd(GridElement gridElement, Point lastPoint) {
		List<GridElement> previewElements = ((DrawPanelDiagram) otherDrawFocusPanel).getPreviewElements();

		if (lastPoint.getX() + -scrollPanel.getHorizontalScrollPosition() <= 0) { // mouse moved from palette to diagram -> insert elements to diagram
			List<GridElement> elementsToMove = new ArrayList<>();
			for (GridElement preview : previewElements) {
				elementsToMove.add(ElementFactoryGwt.create(preview, otherDrawFocusPanel.getDiagram()));
			}
			Selector.replaceGroupsWithNewGroups(elementsToMove, otherDrawFocusPanel.getSelector());
			resetPalette();
			commandInvoker.addElements(otherDrawFocusPanel, elementsToMove, otherDrawFocusPanel.getDiagram().getZoomLevel());

			// set focus to the diagram
			EventHandlingUtils.getStorageInstance().setActivePanel(otherDrawFocusPanel);
		}

		// clear preview object
		if (otherDrawFocusPanel instanceof DrawPanelDiagram) {
			((DrawPanelDiagram) otherDrawFocusPanel).removeOldPreview();
		}
		draggedElements.clear();

		super.onMouseDragEnd(gridElement, lastPoint);
	}

	private void resetPalette() {
		commandInvoker.removeSelectedElements(this);
		commandInvoker.addElements(this, draggedElements, getGridSize());
		draggedElements.clear();
		selector.deselectAll();
	}

	@Override
	public void onShowMenu(Point point) {
		super.onShowMenu(point);
	}

	private List<GridElement> gridElementCopyInOtherDiagram(List<GridElement> originals, int diffY) {
		int verticalScrollbarDiff = otherDrawFocusPanel.scrollPanel.getVerticalScrollPosition() - scrollPanel.getVerticalScrollPosition();
		int horizontalScrollbarDiff = otherDrawFocusPanel.scrollPanel.getHorizontalScrollPosition() - scrollPanel.getHorizontalScrollPosition();
		int oldZoomPalette = getGridSize();
		int oldZoomDiagram = otherDrawFocusPanel.getGridSize();

		List<GridElement> copies = new ArrayList<>();

		otherDrawFocusPanel.getDiagram().setZoomLevel(SharedConstants.DEFAULT_GRID_SIZE);
		zoomEntities(oldZoomPalette, SharedConstants.DEFAULT_GRID_SIZE, originals);

		for (GridElement original : originals) {
			GridElement copy = ElementFactoryGwt.create(original, otherDrawFocusPanel.getDiagram());
			copies.add(copy);
			Rectangle copyRect = copy.getRectangle();
			int newX = copyRect.getX() + horizontalScrollbarDiff;
			// Centering preview in other diagram
			int newY = copyRect.getY() + (paletteChooser.getOffsetHeight() + verticalScrollbarDiff) - lastDraggedGridElement.getRectangle().getY() - lastDraggedGridElement.getRectangle().getHeight() / 2;
			copy.setLocation(newX, newY);
		}
		otherDrawFocusPanel.getDiagram().setZoomLevel(oldZoomDiagram);
		zoomEntities(SharedConstants.DEFAULT_GRID_SIZE, oldZoomPalette, originals);

		// Elements are being zoomed again once they are added to the diagram. We therefore need to adjust the position
		// to display the dragged elements from the palette correctly by taking the future zoom level into account
		double factor = SharedConstants.DEFAULT_GRID_SIZE * 1.0d / otherDrawFocusPanel.getGridSize();
		for (GridElement copy : copies) {
			int newX = (int) (copy.getRectangle().getX() + otherDrawFocusPanel.getVisibleBounds().width * factor);
			int newY = (int) (copy.getRectangle().getY() + diffY * factor);
			copy.setLocation(newX, newY);
		}
		return copies;
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
		lastDraggedGridElement = draggedGridElement;
		if (!draggingDisabled) {
			if (diffX != 0 || diffY != 0) {
				cursorWasMovedDuringDrag = true;
			}
			if (firstDrag && draggedGridElement != null) { // if draggedGridElement == null the whole diagram is dragged and nothing has to be checked for sticking
				stickablesToMove.put(draggedGridElement, getStickablesToMoveWhenElementsMove(draggedGridElement, Collections.<GridElement> emptyList()));
			}
			if (isCtrlKeyDown && !selector.isLassoActive()) {
				selector.startSelection(dragStart);
			}
			if (selector.isLassoActive()) {
				selector.updateLasso(diffX, diffY);
			}
			else if (!resizeDirections.isEmpty()) {
				draggedGridElement.drag(resizeDirections, diffX, diffY, getRelativePoint(dragStart, draggedGridElement), isShiftKeyDown, firstDrag, stickablesToMove.get(draggedGridElement), false);
				List<GridElement> draggedGridElementAsList = new ArrayList<>();
				draggedGridElementAsList.add(draggedGridElement);
				handlePreviewDisplayInstantiated(dragStart, diffX, diffY, isShiftKeyDown, firstDrag, draggedGridElementAsList);
			} // if a single element is selected, drag it (and pass the dragStart, because it's important for Relations)
			else if (selector.getSelectedElements().size() == 1) {
				draggedGridElement.drag(Collections.<Direction> emptySet(), diffX, diffY, getRelativePoint(dragStart, draggedGridElement), isShiftKeyDown, firstDrag, stickablesToMove.get(draggedGridElement), false);
				handlePreviewDisplay(dragStart, diffX, diffY, isShiftKeyDown, firstDrag);
			}
			else if (!selector.isLassoActive()) { // if != 1 elements are selected, move them
				moveElements(diffX, diffY, firstDrag, selector.getSelectedElements());

				// Only handle preview if selector has selected elements
				if (!selector.getSelectedElements().isEmpty()) {
					handlePreviewDisplay(dragStart, diffX, diffY, isShiftKeyDown, firstDrag);
				}
			}

			// stop drag if element is dragged to properties panel
			if (dragStart.getY() + diffY - scrollPanel.getVerticalScrollPosition() > getVisibleBounds().height && !(dragStart.getX() + diffX + -scrollPanel.getHorizontalScrollPosition() <= 0)) {
				cancelDrag(dragStart, diffX, diffY, draggedGridElement);
			}
			redraw(false);
		}
	}

	public void cancelDragNoDuplicate() { // Needed to safely restore the palette after a drag was canceled by a right or middlemouse click in the diagram window
		if (!draggingDisabled && lastDraggedGridElement != null) {
			List<GridElement> lastDraggedGridElementAsList;
			lastDraggedGridElementAsList = new ArrayList();
			lastDraggedGridElementAsList.add(lastDraggedGridElement);
			removeGridElements(lastDraggedGridElementAsList);
			resetPalette();
		}
	}

	private void cancelDrag(Point dragStart, int diffX, int diffY, GridElement draggedGridElement) {
		onMouseDragEnd(draggedGridElement, new Point(dragStart.getX() + diffX, dragStart.getY() + diffY));
		draggingDisabled = true;
	}

	private void handlePreviewDisplayInstantiated(Point dragStart, int diffX, int diffY, boolean isShiftKeyDown, boolean firstDrag, List<GridElement> elementsToPreview) {
		if (otherDrawFocusPanel instanceof DrawPanelDiagram) {
			DrawPanelDiagram otherDrawDiagramFocusPanel = (DrawPanelDiagram) otherDrawFocusPanel;
			if (dragStart.getX() + diffX + -scrollPanel.getHorizontalScrollPosition() <= 0) {
				List<GridElement> elementsToMove = new ArrayList<>(gridElementCopyInOtherDiagram(elementsToPreview, dragStart.y + diffY));
				otherDrawDiagramFocusPanel.setDisplayingPreviewElementInstantiated(elementsToMove, SharedConstants.DEFAULT_GRID_SIZE);
			}
			else {
				// if cursor is dragged back, preview must be removed
				otherDrawDiagramFocusPanel.removeOldPreview();
			}
		}

	}

	private void handlePreviewDisplay(Point dragStart, int diffX, int diffY, boolean isShiftKeyDown, boolean firstDrag) {
		if (otherDrawFocusPanel instanceof DrawPanelDiagram) {
			DrawPanelDiagram otherDrawDiagramFocusPanel = (DrawPanelDiagram) otherDrawFocusPanel;
			if (dragStart.getX() + diffX + -scrollPanel.getHorizontalScrollPosition() <= 0) {
				if (!otherDrawDiagramFocusPanel.currentlyDisplayingPreview()) {
					List<GridElement> elementsToMove = new ArrayList<>(gridElementCopyInOtherDiagram(selector.getSelectedElements(), dragStart.y + diffY));
					otherDrawDiagramFocusPanel.initializeDisplayingPreviewElements(elementsToMove, SharedConstants.DEFAULT_GRID_SIZE);
				}
				else {
					otherDrawDiagramFocusPanel.updateDisplayingPreviewElements(diffX, diffY, firstDrag);
				}
			}
			else {
				// if cursor is dragged back, preview must be removed
				otherDrawDiagramFocusPanel.removeOldPreview();
			}
		}

	}

	@Override
	protected StickableMap getStickablesToMoveWhenElementsMove(GridElement draggedElement, List<GridElement> elements) {
		// Moves at the palette NEVER stick
		return StickableMap.EMPTY_MAP;
	}

	@Override
	public void onThemeChange() {
		super.onThemeChange();
		Element option = paletteChooser.getElement().getFirstChildElement();
		// We need to restyle all options in palette selector
		while (option != null && option.getTagName().equals("OPTION")) {
			option.getStyle().setBackgroundColor(Converter.convert(ThemeFactory.getCurrentTheme().getColor(Theme.ColorStyle.DEFAULT_DOCUMENT_BACKGROUND)).value());
			option = option.getNextSiblingElement();
		}
	}
}
