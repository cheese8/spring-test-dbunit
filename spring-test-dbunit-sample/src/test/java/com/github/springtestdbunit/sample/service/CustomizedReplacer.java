package com.github.springtestdbunit.sample.service;

import org.dbunit.Replacer;
import org.dbunit.StringReplaceDto;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.Replacements;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CustomizedReplacer implements Replacer {

    public CustomizedReplacer() {}

    public static final Map<String, Object> objectMap = new HashMap<String, Object>() {{
        put("[null]", null);
        put("[NULL]", null);
    }};

    public static final List<StringReplaceDto> stringReplaceList = new LinkedList<StringReplaceDto>() {{
        add(new StringReplaceDto("${", "}", false, new HashMap<String, String>(){{put("substring", "replacement");}}));
        add(new StringReplaceDto("!", "!", false, new HashMap<String, String>(){{put("substring", "replacement");}}));
    }};

    @Override
    public Object getValue(Object value) throws DataSetException {
        return Replacements.getValue(value);
    }
}