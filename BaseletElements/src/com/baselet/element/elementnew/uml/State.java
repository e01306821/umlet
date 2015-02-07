package com.baselet.element.elementnew.uml;

import java.util.Arrays;
import java.util.List;

import com.baselet.control.enums.ElementId;
import com.baselet.diagram.draw.DrawHandler;
import com.baselet.element.NewGridElement;
import com.baselet.element.facet.Facet;
import com.baselet.element.facet.PropertiesParserState;
import com.baselet.element.facet.Settings;
import com.baselet.element.facet.common.SeparatorLineWithHalignChangeFacet;
import com.baselet.element.facet.specific.StateTypeFacet;
import com.baselet.element.facet.specific.SubStateSymbolFacet;
import com.baselet.element.settings.SettingsManualresizeCenter;

public class State extends NewGridElement {

	@Override
	public ElementId getId() {
		return ElementId.UMLState;
	}

	@Override
	protected void drawCommonContent(DrawHandler drawer, PropertiesParserState state) {
		// if not type is given, draw an action type as default
		if (!state.getFacetResponse(StateTypeFacet.class, false)) {
			StateTypeFacet.drawDefaultState(drawer, getRealSize());
		}
	}

	@Override
	protected Settings createSettings() {
		return new SettingsManualresizeCenter() {
			@Override
			public List<Facet> createFacets() {
				return Arrays.asList(SeparatorLineWithHalignChangeFacet.INSTANCE, StateTypeFacet.INSTANCE, SubStateSymbolFacet.INSTANCE);
			}
		};
	}
}
