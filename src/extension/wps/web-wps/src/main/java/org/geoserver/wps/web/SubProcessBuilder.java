/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;

public class SubProcessBuilder extends WebPage {
    
    //boolean canceled;

    public SubProcessBuilder(final ExecuteRequest request, final ModalWindow window, Class<?> outputType) {
        Form form = new Form("form");
        add(form);

        final WPSRequestBuilderPanel builder = new WPSRequestBuilderPanel("builder", request, outputType);
        form.add(builder);

        form.add(new AjaxSubmitLink("apply") {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                if (request.processName == null){
                    builder.error("No process has been defined");
                }
                else{
                    window.close(target);
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                super.onError(target, form);
                target.addComponent(builder.getFeedbackPanel());
            }
        });

    }

//    public void setCanceled() {
//        canceled = true;        
//    }
//    
//    public void setAccepted() {
//        canceled = false;        
//    }
//    
//    public boolean isCanceled(){
//        return canceled;
//    }
    
}
