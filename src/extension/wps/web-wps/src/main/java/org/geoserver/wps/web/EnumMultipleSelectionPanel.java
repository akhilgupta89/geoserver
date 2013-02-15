/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.CheckGroupSelector;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.wps.web.InputParameterValues.ParameterType;
import org.geoserver.wps.web.InputParameterValues.ParameterValue;

public class EnumMultipleSelectionPanel extends Panel {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public EnumMultipleSelectionPanel(String id, final InputParameterValues pv) {
        super(id);
        final List<Enum> enums = Arrays.asList(((Class<Enum>) pv.getParameter().type)
                .getEnumConstants());
        ListMultipleChoice siteChoice = new ListMultipleChoice("enums", enums);
        siteChoice.setModel(new Model() {
            ArrayList<Serializable> list = new ArrayList<Serializable>();
            @Override
            public Serializable getObject() {
                list.clear();
                for (ParameterValue value : pv.values) {
                    list.add(value.value);
                }
                return list;
            }

            @Override
            public void setObject(Serializable object) {                                
                for (Serializable obj : list) {
                    ParameterValue value = new ParameterValue(ParameterType.LITERAL,
                            pv.getDefaultMime(), obj);
                    pv.values.add(value);
                }
            }

        });
        add(siteChoice);
    }
}
