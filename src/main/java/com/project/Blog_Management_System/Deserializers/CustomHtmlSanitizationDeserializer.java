package com.project.Blog_Management_System.Deserializers;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;


public class CustomHtmlSanitizationDeserializer extends StdDeserializer<String> {

    private static final Safelist CUSTOM_POLICY = Safelist.relaxed()
            .removeTags("img")
            .addAttributes(":all", "class", "style");

    public CustomHtmlSanitizationDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        String value = p.getValueAsString();
        return (value == null) ? null : Jsoup.clean(value, CUSTOM_POLICY).trim();
    }

}
