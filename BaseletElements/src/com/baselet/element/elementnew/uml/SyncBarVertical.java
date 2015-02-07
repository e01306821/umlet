package com.baselet.element.elementnew.uml;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.baselet.control.basics.geom.Dimension;
import com.baselet.control.basics.geom.Rectangle;
import com.baselet.control.constants.FacetConstants;
import com.baselet.control.enums.Direction;
import com.baselet.control.enums.ElementId;
import com.baselet.diagram.draw.DrawHandler;
import com.baselet.element.NewGridElement;
import com.baselet.element.facet.Facet;
import com.baselet.element.facet.PropertiesParserState;
import com.baselet.element.facet.Settings;
import com.baselet.element.settings.SettingsNoText;
import com.baselet.element.sticking.StickingPolygon;
import com.baselet.element.sticking.polygon.StickingPolygonGenerator;

public class SyncBarVertical extends NewGridElement {

	private final StickingPolygonGenerator syncBarStickingPolygonGenerator = new StickingPolygonGenerator() {
		@Override
		public StickingPolygon generateStickingBorder(Rectangle rect) {
			StickingPolygon p = new StickingPolygon(rect.x, rect.y);
			double lt = getDrawer().getStyle().getLineWidth();
			double halfWidth = getRealSize().getWidth() * 0.5;
			p.addRectangle(new Rectangle(halfWidth - lt * 0.5, 0.0, lt, (double) getRealSize().getHeight()));
			return p;
		}
	};

	@Override
	public ElementId getId() {
		return ElementId.UMLSyncBarVertical;
	}

	@Override
	protected void drawCommonContent(DrawHandler drawer, PropertiesParserState state) {
		if (drawer.getStyle().getLineWidth() == FacetConstants.LINE_WIDTH_DEFAULT) {
			drawer.setLineWidth(5);
		}
		Dimension s = getRealSize();
		drawer.drawLine(s.getWidth() * 0.5, 0, s.getWidth() * 0.5, s.getHeight());
		state.setStickingPolygonGenerator(syncBarStickingPolygonGenerator);
	}

	@Override
	protected Settings createSettings() {
		return new SettingsNoText() {
			@Override
			public List<Facet> createFacets() {
				return Collections.<Facet> emptyList();
			}
		};
	}

	@Override
	public Set<Direction> getResizeArea(int x, int y) {
		Set<Direction> returnSet = super.getResizeArea(x, y);
		returnSet.remove(Direction.LEFT);
		returnSet.remove(Direction.RIGHT);
		return returnSet;
	}
}
