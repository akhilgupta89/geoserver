package org.geoserver.wps.web;

/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.PageCreator;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.web.wicket.CRSPanel;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SRSListPanel;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A class derived from CRSPanel that shows the CRS search modal window
 * using an IFrame. This avoids problems when the panel is part of
 * another modal window 
 *
 */
@SuppressWarnings("serial")
public class SubProcessCRSPanel extends CRSPanel{

    
    public SubProcessCRSPanel(String id, IModel model) {
        super(id, model);        
    }


    /*
     * helper for internally creating the panel. 
     */
    protected void initComponents() {
            
        popupWindow = new ModalWindow("popup");
        add( popupWindow );
        popupWindow.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            public void onClose(AjaxRequestTarget target) {
                Object obj = srsTextField.getModelObject();
                srsTextField.setModelObject(srsTextField.getModelObject());
                if (obj instanceof String){                                           
                    CoordinateReferenceSystem crs = fromSRS((String) obj);
                    wktLabel.setDefaultModelObject( crs.getName().toString() );
                    wktLink.setEnabled(true);
                    target.addComponent( wktLink );                    
                    popupWindow.close(target);                     
                    target.addComponent(srsTextField);                                                                          
                }
                
            }

        });
        
        srsTextField = new TextField( "srs", new Model() );
        add( srsTextField );
        srsTextField.setOutputMarkupId( true );
        
        srsTextField.add(new AjaxFormComponentUpdatingBehavior("onblur") {
            
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                convertInput();
                
                CoordinateReferenceSystem crs = (CoordinateReferenceSystem) getConvertedInput();
                if(crs != null) {
                    setModelObject(crs);
                    wktLabel.setDefaultModelObject(crs.getName().toString());
                    wktLink.setEnabled(true);
                } else {
                    wktLabel.setDefaultModelObject(null);
                    wktLink.setEnabled(false);
                }
                target.addComponent(wktLink);
                
                onSRSUpdated(toSRS(crs), target);
            }
        });
        
        findLink = new AjaxLink( "find" ) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                popupWindow.setPageCreator(srsPageCreator());//Content(srsListPanel());
                popupWindow.setTitle(new ParamResourceModel("selectSRS", SubProcessCRSPanel.this));
                popupWindow.show(target);
            }
        };
        add(findLink);
        
        wktLink = new GeoServerAjaxFormLink("wkt") {
            @Override
            public void onClick(AjaxRequestTarget target, Form form) {
                popupWindow.setInitialHeight( 375 );
                popupWindow.setInitialWidth( 525 );
                popupWindow.setContent(new WKTPanel( popupWindow.getContentId(), getCRS()));
                CoordinateReferenceSystem crs = (CoordinateReferenceSystem) SubProcessCRSPanel.this.getModelObject();
                if(crs != null)
                    popupWindow.setTitle(crs.getName().toString());
                popupWindow.show(target);
            }
        };
        wktLink.setEnabled(getModelObject() != null);
        add(wktLink);
        
        wktLabel = new Label( "wktLabel", new Model());
        wktLink.add( wktLabel );
        wktLabel.setOutputMarkupId( true );
    }


    @Override
    protected void onBeforeRender() {
        CoordinateReferenceSystem crs = (CoordinateReferenceSystem) getModelObject();
        if ( crs != null ) {
            srsTextField.setModelObject( toSRS(crs) );
            wktLabel.setDefaultModelObject( crs.getName().toString() );    
        } else {
            wktLabel.setDefaultModelObject(null);
            wktLink.setEnabled(false);
        }
        
        super.onBeforeRender();
    }
    
    @Override
    protected void convertInput() {
        String srs = srsTextField.getInput();
        CoordinateReferenceSystem crs = null;
        if ( srs != null && !"".equals(srs)) {
            if ( "UNKNOWN".equals( srs ) ) {
                //leave underlying crs unchanged
                if ( getModelObject() instanceof CoordinateReferenceSystem ) {
                    setConvertedInput(getModelObject());
                }
                return;
            }
            crs = fromSRS( srs );
        }
        setConvertedInput( crs );
    }
    
    
    /**
     * Subclasses can override to perform custom behaviors when the SRS is updated, which happens
     * either when the text field is left or when the find dialog returns
     * @param target 
     */
    protected void onSRSUpdated(String srs, AjaxRequestTarget target) {
        
    }
    
    /*
     * Builds the srs list panel component.
     */
    @SuppressWarnings("serial")
    protected SRSListPanel srsListPanel() {
        SRSListPanel srsList = new SRSListPanel("srsList"/*popupWindow.getContentId()*/) {
            
            @Override
            protected void onCodeClicked(AjaxRequestTarget target, String epsgCode) {                
                
                String srs =  "EPSG:" + epsgCode ;
                srsTextField.setModelObject( srs );
                target.addComponent( srsTextField );
                
                CoordinateReferenceSystem crs = fromSRS( srs );
                wktLabel.setDefaultModelObject( crs.getName().toString() );
                wktLink.setEnabled(true);
                target.addComponent( wktLink );
                onSRSUpdated(srs, target);
                popupWindow.close(target);                
               
            }
        };
        srsList.setCompactMode(true);
        return srsList;
    }
    
    
    protected PageCreator srsPageCreator() {
        PageCreator pageCreator = new PageCreator(){
            @Override
            public Page createPage() {
                return new CRSPage();
            }            
        };
        return pageCreator;
    }
    
    class CRSPage extends WebPage{        
        public CRSPage(){
            Form form = new Form("form");
            add(form);               
            form.add(srsListPanel());
        }        
    }
    
}
