package com.pahappa.sacco.util;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

//Formats every monetary value in the system consistently as
//"UGX XX,XXX.00" 
@FacesConverter("moneyConverter")
public class MoneyConverter implements Converter<BigDecimal> {

    private static final DecimalFormat FORMAT;
    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US); // comma grouping, dot decimal
        FORMAT = new DecimalFormat("#,##0.00", symbols);
    }

    @Override
    public BigDecimal getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            String cleaned = value.replace("UGX", "").replace(",", "").trim();
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            throw new ConverterException("'" + value + "' is not a valid amount.");
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, BigDecimal value) {
        if (value == null) {
            return "";
        }
        synchronized (FORMAT) { // DecimalFormat is not thread-safe; this converter instance is shared
            return "UGX " + FORMAT.format(value);
        }
    }
}
