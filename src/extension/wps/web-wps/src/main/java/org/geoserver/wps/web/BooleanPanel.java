package org.geoserver.wps.web;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

public class BooleanPanel extends Panel{
    
    public BooleanPanel(String id, IModel<Boolean> model, Boolean defaultValue) {
        super(id, model);
        final List<Boolean> enums = Arrays.asList(new Boolean[]{Boolean.TRUE, Boolean.FALSE});
        DropDownChoice<Boolean> choice = new DropDownChoice<Boolean>("boolean", model, enums);
        choice.setModelObject(defaultValue);
        add(choice);
    }

}
