/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import static junit.framework.Assert.assertTrue;

import java.io.Serializable;
import java.util.List;

import org.apache.wicket.PageParameters;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

/**
 * 
 * @author Martin Davis OpenGeo
 * 
 */
public class WPSRequestBuilderTest extends GeoServerWicketTestSupport {

    @Test
    public void testJTSAreaWorkflow() throws Exception {
        login();

        // start the page
        tester.startPage(new WPSRequestBuilder());

        tester.assertComponent("form:requestBuilder:process", DropDownChoice.class);

        // look for JTS area
        DropDownChoice choice = (DropDownChoice) tester
                .getComponentFromLastRenderedPage("form:requestBuilder:process");
        int index = -1;
        final List choices = choice.getChoices();
        for (int i = 0; i < choices.size(); i++) {
            if (choices.get(i).equals("JTS:area")) {
                index = 0;
                break;
            }
        }

        // choose a process
        FormTester form = tester.newFormTester("form");
        form.select("requestBuilder:process", index);
        tester.executeAjaxEvent("form:requestBuilder:process", "onchange");

        // print(tester.getComponentFromLastRenderedPage("form"), true, true);

        // check process description
        tester.assertModelValue("form:requestBuilder:process", "JTS:area");
        Label label = (Label) tester
                .getComponentFromLastRenderedPage("form:requestBuilder:descriptionContainer:processDescription");
        assertTrue(label.getDefaultModelObjectAsString().contains("area"));

        tester.assertComponent(
                "form:requestBuilder:inputContainer:inputs:0:paramValue:editor:mime",
                DropDownChoice.class);
        tester.assertComponent(
                "form:requestBuilder:inputContainer:inputs:0:paramValue:editor:textarea",
                TextArea.class);

        // fill in the params
        form = tester.newFormTester("form");
        form.select("requestBuilder:inputContainer:inputs:0:paramValue:editor:mime", 2);
        form.setValue("requestBuilder:inputContainer:inputs:0:paramValue:editor:textarea",
                "POLYGON((0 0, 0 10, 10 10, 10 0, 0 0))");
        form.submit();
        tester.clickLink("form:execute", true);

        assertTrue(tester.getComponentFromLastRenderedPage("responseWindow")
                .getDefaultModelObjectAsString().contains("wps:Execute"));

        // unfortunately the wicket tester does not allow us to get work with the popup window
        // contents,
        // as that requires a true browser to execute the request
    }

    /**
     * Tests initializing page to specific process via name request parameter.
     * 
     * @throws Exception
     */
    @Test
    public void testNameRequest() throws Exception {
        login();

        // start the page
        tester.startPage(new WPSRequestBuilder(new PageParameters("name=JTS:area")));

        tester.assertComponent("form:requestBuilder:process", DropDownChoice.class);

        // check process description
        tester.assertModelValue("form:requestBuilder:process", "JTS:area");

        tester.assertComponent(
                "form:requestBuilder:inputContainer:inputs:0:paramValue:editor:textarea",
                TextArea.class);
    }

    public void testWrongValues() throws Exception {
        login();

        // start the page
        tester.startPage(new WPSRequestBuilder());

        tester.assertComponent("form:requestBuilder:process", DropDownChoice.class);

        // look for JTS area
        DropDownChoice choice = (DropDownChoice) tester
                .getComponentFromLastRenderedPage("form:requestBuilder:process");
        int index = -1;
        final List choices = choice.getChoices();
        for (int i = 0; i < choices.size(); i++) {
            if (choices.get(i).equals("JTS:area")) {
                index = 0;
                break;
            }
        }

        // choose a process
        FormTester form = tester.newFormTester("form");
        form.select("requestBuilder:process", index);
        tester.executeAjaxEvent("form:requestBuilder:process", "onchange");

        // print(tester.getComponentFromLastRenderedPage("form"), true, true);

        // check process description
        tester.assertModelValue("form:requestBuilder:process", "JTS:area");
        Label label = (Label) tester
                .getComponentFromLastRenderedPage("form:requestBuilder:descriptionContainer:processDescription");
        assertTrue(label.getDefaultModelObjectAsString().contains("area"));

        tester.assertComponent(
                "form:requestBuilder:inputContainer:inputs:0:paramValue:editor:mime",
                DropDownChoice.class);
        tester.assertComponent(
                "form:requestBuilder:inputContainer:inputs:0:paramValue:editor:textarea",
                TextArea.class);

        // fill in the params
        form = tester.newFormTester("form");
        form.select("requestBuilder:inputContainer:inputs:0:paramValue:editor:mime", 2);
        form.setValue("requestBuilder:inputContainer:inputs:0:paramValue:editor:textarea",
                "wrongwktstring");
        form.submit();
        tester.clickLink("form:execute", true);

        List<Serializable> msg = tester.getMessages(FeedbackMessage.ERROR);
        tester.assertErrorMessages(new String[] { "Invalid WKT string: wrongwktstring" });
    }

    @Test
    public void testMissingValues() throws Exception {
        login();

        // start the page
        tester.startPage(new WPSRequestBuilder());

        tester.assertComponent("form:requestBuilder:process", DropDownChoice.class);

        // look for Heatmap (a process that contains many different parameter types)
        DropDownChoice choice = (DropDownChoice) tester
                .getComponentFromLastRenderedPage("form:requestBuilder:process");
        int index = -1;
        final List choices = choice.getChoices();
        for (int i = 0; i < choices.size(); i++) {
            if (choices.get(i).equals("gs:Heatmap")) {
                index = i;
                break;
            }
        }

        // choose a process
        FormTester form = tester.newFormTester("form");
        form.select("requestBuilder:process", index);
        tester.executeAjaxEvent("form:requestBuilder:process", "onchange");

        // check process description
        tester.assertModelValue("form:requestBuilder:process", "gs:Heatmap");

        // try to execute without entering any value
        tester.clickLink("form:execute", true);

        List<Serializable> msg = tester.getMessages(FeedbackMessage.ERROR);
        tester.assertErrorMessages(new String[] { "Field 'layer' is required.",
                "Field 'radiusPixels' is required.", "Field 'paramValue' is required.",
                "Field 'outputWidth' is required.", "Field 'outputHeight' is required." });
    }
    
    @Test
    public void testInitValues() throws Exception{
        
    }

}
