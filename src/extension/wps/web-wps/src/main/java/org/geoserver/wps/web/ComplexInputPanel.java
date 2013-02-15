/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.UrlValidator;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.wps.ppio.GMLPPIO.GML2;
import org.geoserver.wps.web.InputParameterValues.ParameterType;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.gml2.GMLConfiguration;
import org.geotools.util.logging.Logging;
import org.geotools.xml.Parser;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

/**
 * Allows the user to edit a complex input parameter providing a variety of different editors
 * depending on the parameter type
 * 
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public class ComplexInputPanel extends Panel {
    static final Logger LOGGER = Logging.getLogger(WPSRequestBuilderPanel.class);

    private DropDownChoice typeChoice;

    PropertyModel valueModel;

    List<String> mimeTypes;

    ModalWindow subprocesswindow;

    boolean required;

    public ComplexInputPanel(String id, final InputParameterValues pv, int valueIndex) {
        super(id);
        setOutputMarkupId(true);
        setDefaultModel(new PropertyModel(pv, "values[" + valueIndex + "]"));
        valueModel = new PropertyModel(getDefaultModel(), "value");
        mimeTypes = pv.getSupportedMime();
        required = pv.getParameter().minOccurs > 0;

        List<ParameterType> ptypes = pv.getSupportedTypes();
        ptypes.remove(ParameterType.LITERAL);
        typeChoice = new DropDownChoice("type", new PropertyModel(getDefaultModelObject(), "type"),
                ptypes);
        add(typeChoice);

        subprocesswindow = new ModalWindow("subprocessPopupWindow");
        subprocesswindow.setInitialWidth(700);
        subprocesswindow.setInitialHeight(500);
        add(subprocesswindow);
        subprocesswindow.setPageCreator(new ModalWindow.PageCreator() {

            public Page createPage() {
                return new SubProcessBuilder((ExecuteRequest) valueModel.getObject(),/*subprocesswindow
                        .getDefaultModelObject(),*/ subprocesswindow, pv.getParameter().type);
            }
        });

        updateEditor();

        typeChoice.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateEditor();
                target.addComponent(ComplexInputPanel.this);
            }

        });
    }

    void updateEditor() {
        // remove the old editor
        if (get("editor") != null) {
            remove("editor");
        }

        ParameterType pt = (ParameterType) typeChoice.getModelObject();
        if (pt == ParameterType.TEXT) {
            // an internal vector layer
            if (!(valueModel.getObject() instanceof String)) {
                valueModel.setObject("");
            }

            // data as plain text
            Fragment f = new Fragment("editor", "text", this);            
            DropDownChoice mimeChoice = new DropDownChoice("mime", new PropertyModel(
                    getDefaultModel(), "mime"), mimeTypes);
            f.add(mimeChoice);
            
            TextArea text = new TextArea("textarea", valueModel);
            //This will complain when the value is required and the text is left blank, but will use 
            //'textarea' instead of the name of the parameter. That can be improved...
            text.setRequired(required);
            text.add(getTextValidator(mimeChoice));
            f.add(text);
            add(f);
        } else if (pt == ParameterType.VECTOR_LAYER) {
            // an internal vector layer
            if (!(valueModel.getObject() instanceof VectorLayerConfiguration)) {
                valueModel.setObject(new VectorLayerConfiguration());
            }

            new PropertyModel(getDefaultModel(), "mime").setObject("text/xml");
            Fragment f = new Fragment("editor", "vectorLayer", this);
            DropDownChoice layer = new DropDownChoice("layer", new PropertyModel(valueModel,
                    "layerName"), getVectorLayerNames());
            layer.setRequired(required);
            f.add(layer);
            add(f);
        } else if (pt == ParameterType.RASTER_LAYER) {
            // an internal raster layer
            if (!(valueModel.getObject() instanceof RasterLayerConfiguration)) {
                valueModel.setObject(new RasterLayerConfiguration());
            }

            Fragment f = new Fragment("editor", "rasterLayer", this);
            final DropDownChoice layer = new DropDownChoice("layer", new PropertyModel(valueModel,
                    "layerName"), getRasterLayerNames());
            f.add(layer);
            layer.setRequired(required);
            add(f);

            // we need to update the raster own bounding box as wcs requests
            // mandate a spatial extent (why oh why???)
            layer.add(new AjaxFormComponentUpdatingBehavior("onchange") {

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    String name = layer.getDefaultModelObjectAsString();
                    LayerInfo li = GeoServerApplication.get().getCatalog().getLayerByName(name);
                    ReferencedEnvelope spatialDomain = li.getResource().getNativeBoundingBox();
                    ((RasterLayerConfiguration) valueModel.getObject())
                            .setSpatialDomain(spatialDomain);
                }
            });
        } else if (pt == ParameterType.REFERENCE) {
            // an external reference
            if (!(valueModel.getObject() instanceof ReferenceConfiguration)) {
                valueModel.setObject(new ReferenceConfiguration());
            }

            Fragment f = new Fragment("editor", "reference", this);
            final DropDownChoice method = new DropDownChoice("method", new PropertyModel(
                    valueModel, "method"), Arrays.asList(ReferenceConfiguration.Method.GET,
                    ReferenceConfiguration.Method.POST));
            f.add(method);

            DropDownChoice mimeChoice = new DropDownChoice("mime", new PropertyModel(
                    valueModel, "mime"), mimeTypes);            
            f.add(mimeChoice);

            TextField urlField = new TextField("url", new PropertyModel(valueModel, "url"));
            urlField.setRequired(required);
            urlField.add(new UrlValidator());
            f.add(urlField);
            final TextArea body = new TextArea("body", new PropertyModel(valueModel, "body"));
            add(body);

            final WebMarkupContainer bodyContainer = new WebMarkupContainer("bodyContainer");
            f.add(bodyContainer);
            bodyContainer.setOutputMarkupId(true);
            bodyContainer.add(body);
            bodyContainer.setVisible(false);

            method.add(new AjaxFormComponentUpdatingBehavior("onchange") {

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    boolean post = method.getModelObject() == ReferenceConfiguration.Method.POST;
                    bodyContainer.setVisible(post);
                    body.setRequired(post);
                    target.addComponent(ComplexInputPanel.this);
                }
            });

            add(f);
        } else if (pt == ParameterType.SUBPROCESS) {
            if (!(valueModel.getObject() instanceof ExecuteRequest)) {
                valueModel.setObject(new ExecuteRequest());
            }

            Fragment f = new Fragment("editor", "subprocess", this);
            f.add(new AjaxLink("edit") {

                @Override
                public void onClick(AjaxRequestTarget target) {
                    subprocesswindow.setDefaultModel(valueModel);
                    subprocesswindow.show(target);
                }
            });

            final TextArea xml = new TextArea("xml");
            if (((ExecuteRequest) valueModel.getObject()).processName != null) {
                try {
                    xml.setModelObject(getExecuteXML());
                } catch (Throwable t) {
                    xml.setModel(new Model(""));
                }
            } else {
                xml.setModel(new Model(""));
            }
            xml.setOutputMarkupId(true);
            f.add(xml);
            xml.setRequired(required);

            subprocesswindow.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

                public void onClose(AjaxRequestTarget target) {
                    // turn the GUI request into an actual WPS request                    
                    xml.setModelObject(getExecuteXML());
                    target.addComponent(xml);                 
                }

            });
            
//            subprocesswindow.setCloseButtonCallback(new ModalWindow.CloseButtonCallback() {
//                
//                @Override
//                public boolean onCloseButtonClicked(AjaxRequestTarget arg0) { 
//                    ((SubProcessBuilder)subprocesswindow.getPage()).setCanceled();                    
//                    return true;
//                }
//                
//            });

            add(f);
        } else {
            error("Unsupported parameter type");
        }
    }

    private IValidator getTextValidator(final DropDownChoice mimeChoice) {                
        return new IValidator<String>() {
            @Override
            //TODO: there must be a better way of getting mimetype strings...
            public void validate(IValidatable<String> v) {
                String s = v.getValue();    
                String type = (String) mimeChoice.getConvertedInput();
                if (type == null){
                    v.error(new ValidationError().setMessage("No mime type has been selected for the textbox content"));
                }
                else if (type.equals("application/wkt")){
                    try {
                        new WKTReader().read(s);
                    } catch (ParseException e) {
                        v.error(new ValidationError().setMessage("Invalid WKT string:" + s));
                    }
                }
                else if (type.equals("application/json")){
                    FeatureJSON json = new FeatureJSON();
                    try {
                        json.readFeatureCollection(new ByteArrayInputStream(s.getBytes()));
                    } catch (Exception e) {
                        v.error(new ValidationError().setMessage("Invalid JSON string:" + e.getMessage()));
                    }
                }
                else if (type.equals("text/xml; subtype=gml/2.1.2")){ 
                    GMLConfiguration gml = new GMLConfiguration();
                    Parser parser = new Parser(gml);
                    parser.setFailOnValidationError(false);
                    parser.setStrict(false);
                    try {
                        parser.validate(new ByteArrayInputStream(s.getBytes()));
                        List<?> errors = parser.getValidationErrors(); 
                        if (!errors.isEmpty()){
                            v.error(new ValidationError().setMessage("Invalid GML. " + errors.get(0)));    
                        }
                    } catch (Exception e) {
                        v.error(new ValidationError().setMessage("Invalid GML. " + e.getMessage()));
                    }
                }
                else if (type.equals("text/xml; subtype=gml/3.1.1")){ 
                    org.geotools.gml3.GMLConfiguration gml = new org.geotools.gml3.GMLConfiguration();
                    Parser parser = new Parser(gml);
                    parser.setFailOnValidationError(false);
                    parser.setStrict(false);
                    try {
                        parser.validate(new ByteArrayInputStream(s.getBytes()));
                        List<?> errors = parser.getValidationErrors(); 
                        if (!errors.isEmpty()){
                            v.error(new ValidationError().setMessage("Invalid GML. " + errors.get(0)));    
                        }
                    } catch (Exception e) {
                        v.error(new ValidationError().setMessage("Invalid GML. " + e.getMessage()));
                    }
                }                
                else if (type.equals("application/zip")){
                    URL url;
                    try {
                        url = new URL(s);
                    } catch (MalformedURLException e) {
                        v.error(new ValidationError().setMessage("Wrong URL:" + s));
                        return;
                    }
                    if (url.getProtocol().equals("file")){
                        if (!new File(url.getFile()).exists()){
                            v.error(new ValidationError().setMessage("The selected file does not exist:" + s));
                        }
                    }
                }
                
            }
        };
        
    }

    String getExecuteXML() {
        WPSExecuteTransformer tx = new WPSExecuteTransformer();
        tx.setIndentation(2);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            tx.transform(valueModel.getObject(), out);
        } catch (TransformerException e) {
            LOGGER.log(Level.SEVERE, "Error generating xml request", e);
            error(e);
        }
        String executeXml = out.toString();
        return executeXml;
    }

    List<String> getVectorLayerNames() {
        Catalog catalog = GeoServerApplication.get().getCatalog();

        List<String> result = new ArrayList<String>();
        for (LayerInfo li : catalog.getLayers()) {
            if (li.getResource() instanceof FeatureTypeInfo) {
                result.add(li.getResource().getPrefixedName());
            }
        }
        return result;
    }

    List<String> getRasterLayerNames() {
        Catalog catalog = GeoServerApplication.get().getCatalog();

        List<String> result = new ArrayList<String>();
        for (LayerInfo li : catalog.getLayers()) {
            if (li.getResource() instanceof CoverageInfo) {
                result.add(li.getResource().getPrefixedName());
            }
        }
        return result;
    }
    
}
