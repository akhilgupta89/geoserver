package org.geoserver.wps.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.wps.web.InputParameterValues.ParameterType;
import org.geoserver.wps.web.InputParameterValues.ParameterValue;
import org.geotools.data.Parameter;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class LiteralValueTextField extends TextField{
    
    static final String VALUES_SEPARATOR = ",";

    public LiteralValueTextField(String id, InputParameterValues pv) {
        super(id);
        setModel(new LiteralValueModel(pv));
        Parameter<?> p = pv.getParameter();        
        setRequired(p.minOccurs > 0);
        setLabel(new Model<String>(p.key));
        Object defaultValue = p.getDefaultValue();
        if (defaultValue != null){
            setModelObject(defaultValue);
        }
        else{
            setModelObject(p.sample);
        }
        IValidator validator = getValidatorForLiteralValue(p);
        add(validator);
    }

    private IValidator<String> getValidatorForLiteralValue(Parameter p) {
        final Class<?> type = p.type;
        final double min = p.metadata.containsKey(Parameter.MIN) ? ((Number) p.metadata
                .get(Parameter.MIN)).doubleValue() : Double.NEGATIVE_INFINITY;
        final double max = p.metadata.containsKey(Parameter.MAX) ? ((Number) p.metadata
                .get(Parameter.MAX)).doubleValue() : Double.POSITIVE_INFINITY;
        final boolean isSingleValue = p.maxOccurs == 1;
        return new IValidator<String>() {
            @Override
            public void validate(IValidatable<String> v) {
                String s = v.getValue();
                String[] values;
                if (!isSingleValue) {                    
                    values = s.split(VALUES_SEPARATOR);
                } else {
                    values = new String[] {s};
                }
                if (Number.class.isAssignableFrom(type)) {                    
                    try{
                        for (String singleValue : values){
                            validateNumber(singleValue, type, min, max);
                        }
                    }
                    catch(IllegalArgumentException e){                        
                        v.error(new ValidationError().setMessage(e.getMessage()));
                    }
                }
            }

        };

    }

    private void validateNumber(String s, Class<?> type, double min, double max) {
        final Number num;
        try {
            if (type.equals(Integer.class)) {
                num = new Integer(s);
            } else if (type.equals(Long.class)) {
                num = new Long(s);
            } else if (type.equals(Double.class)) {
                num = new Double(s);
            } else if (type.equals(float.class)) {
                num = new Float(s);
            } else if (type.equals(Short.class)) {
                num = new Short(s);
            } else if (type.equals(Byte.class)) {
                num = new Byte(s);
            } else {
                return;
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(s + " is not a valid value");            
        }
        double n = num.doubleValue();
        if (n < min || n > max) {
            throw new IllegalArgumentException(s + " is not within the valid range (" + Double.toString(min) + "-"
                    + Double.toString(max) + ")");
        }

    }
    
    /**
     * Sets the value of the field. This should be used instead of setModelObject(),
     * since this handles multiples values and creates the correspodning comma-separated
     * string.
     */
    public void setValue(List<ParameterValue> values){
        if (values == null || values.isEmpty()){
            return;
        }
        String s = "";
        if (values.size() == 1){
            Serializable value = values.get(0).value;
            if (value != null){
                s = values.get(0).value.toString();
            }
        }
        else{
            for (int i = 0; i < values.size(); i++) {
                Serializable value = values.get(i).value;
                if (value != null){
                    s += values.get(i).value.toString();
                }
                if (i < values.size() - 1){
                    s += ",";
                }
            }                                      
        }
        setModelObject(s);
    }
    
    /**
     * A model that handles string representation of literal value, including
     * comma separated value to indicate multiple values for parameters with
     * maxOccur > 0.     
     *
     */
    class LiteralValueModel extends Model {            
        
        private InputParameterValues pv;
        
        static final String VALUES_SEPARATOR = ",";
        
        public LiteralValueModel(InputParameterValues pv) {
            this.pv = pv;
        }
        @Override
        public Serializable getObject() {
            String s = "";
            for (int i = 0; i < pv.values.size(); i++) {
                Serializable value = pv.values.get(i).value;
                if (value != null){
                    s += pv.values.get(i).value.toString();
                }
                if (i < pv.values.size() - 1){
                    s += ",";
                }
            }     
            return s;
        }
        @Override
        public void setObject(Serializable object) {
            if (object == null){
                pv.values.clear();
                return;
            }
            Serializable[] objs;
            if (pv.getParameter().maxOccurs != 1) {
                String s = object.toString();
                objs = s.split(VALUES_SEPARATOR);
            } else {
                objs = new Serializable[] { object };
            }
            pv.values = new ArrayList<ParameterValue>();
            for (Serializable obj : objs) {
                ParameterValue value = new ParameterValue(ParameterType.LITERAL,
                        pv.getDefaultMime(), obj);
                pv.values.add(value);
            }
        }
    };
   

}


