package com.github.mostroverkhov.retrofit_property_streaming;

import com.github.mostroverkhov.retrofit_property_streaming.model.Prop;
import com.github.mostroverkhov.retrofit_property_streaming.model.PropType;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Created by Maksym Ostroverkhov on 13.06.2016.
 */
public class PropertySlicer<T> {

    private final Type propTarget;
    private final Gson gson;
    private final JsonReader jsonStream;

    private volatile boolean isRoot = true;

    private final PropertyContext propertyContext = new PropertyContext();

    public PropertySlicer(Type propTarget, Gson gson, JsonReader jsonStream) throws IOException {
        this.propTarget = propTarget;
        this.gson = gson;
        this.jsonStream = jsonStream;
    }

    /**
     * @return next property, or null if end of document is reached
     */
    public Prop<T> nextProp() {
        try {
            return getNextProp();
        } catch (IOException e) {
            throw new IllegalStateException("Error while parsing json stream ", e);
        }
    }

    private PropertyContext context() {
        return propertyContext;
    }

    private Prop<T> getNextProp() throws IOException {
        String name = null;

        JsonToken token = jsonStream.peek();
        if (token == JsonToken.NAME) {
            name = jsonStream.nextName();
            token = jsonStream.peek();
            /*proceed to next if root is object; if root is array, it is consumed by ARR_START delegate,
            * because we clients are interested in corresponding events*/
        } else if (token == JsonToken.BEGIN_OBJECT && isRoot) {
            jsonStream.beginObject();
            return nextProp();
            /*proceed to next - end_document, if root is object; otherwise this token is always
            consumed by OBJECT_OR_PRIMITIVE delegate*/
        } else if (token == JsonToken.END_OBJECT) {
            jsonStream.endObject();
            return nextProp();
        }

        isRoot = false;

        Delegate delegate;
        switch (token) {
            case BEGIN_ARRAY:
                delegate = Delegate.ARR_START;
                propertyContext.setArrayType();
                break;
            case END_ARRAY:
                delegate = Delegate.ARR_END;
                propertyContext.setPropType();
                break;
            case BEGIN_OBJECT:
            case NUMBER:
            case BOOLEAN:
            case STRING:
            case NULL:
                delegate = isTargetObject() ? Delegate.OBJECT_OR_PRIMITIVE : Delegate.ARR;
                break;
            case END_DOCUMENT:
                return null;
            default:
                throw new IllegalStateException("Unexpected token: " + token);
        }
        Prop<T> prop = delegate.resolve(name, propTarget, this);
        /*proceed to next prop if there is no field with given name in target type*/
        if (prop == null) {
            return getNextProp();
        } else {
            return prop;
        }
    }

    private boolean isTargetObject() {
        return !propertyContext.isArray();
    }

    private static class PropertyContext {

        private volatile boolean isArray;
        private volatile ContextState contextState = ContextState.empty();

        public void clearNameAndType() {
            contextState = ContextState.empty();
        }

        public void setItemNameAndType(String itemName, Type itemType) {
            contextState = ContextState.state(itemName, itemType);
        }

        public void setArrayType() {
            isArray = true;
        }

        public void setPropType() {
            isArray = false;
        }

        public boolean isArray() {
            return isArray;
        }

        public String getItemName() {
            return contextState.getItemName();
        }

        public Type getItemType() {
            return contextState.getItemType();
        }
    }

    private static class ContextState {

        private static final ContextState CONTEXT_STATE = new ContextState(null, null);

        private final String itemName;
        private final Type itemType;

        public static ContextState empty() {
            return CONTEXT_STATE;
        }

        public static ContextState state(String itemName, Type itemType) {
            return new ContextState(itemName, itemType);
        }

        private ContextState(String itemName, Type itemType) {
            this.itemName = itemName;
            this.itemType = itemType;
        }

        public String getItemName() {
            return itemName;
        }

        public Type getItemType() {
            return itemType;
        }
    }

    private Gson gson() {
        return gson;
    }

    private JsonReader jsonStream() {
        return jsonStream;
    }

    private enum Delegate {

        OBJECT_OR_PRIMITIVE {
            @Override
            <T> Prop<T> resolve(String propName, Type targetType, PropertySlicer<T> calculator) throws IOException {
                Gson gson = calculator.gson();
                JsonReader jsonStream = calculator.jsonStream();
                Type fieldType = getFieldTypeForTarget(targetType, propName);
                /*field is not present in target type*/
                if (fieldType == null) {
                    jsonStream.skipValue();
                    return null;
                } else {
                    return new Prop<>(propName, gson.fromJson(jsonStream, fieldType), PropType.PROPERTY);
                }
            }
        },

        ARR_START {
            @Override
            <T> Prop<T> resolve(String propName, Type targetType, PropertySlicer<T> calculator) throws IOException {

                JsonReader jsonStream = calculator.jsonStream();

                Type listGenericType;
                if (propName == null) {
                    listGenericType = getListGenericArgOrNull(targetType);

                /*property is array*/
                } else {
                    Class<?> rootRawType = Utils.getRawType(targetType);
                    try {
                        Type fieldType = rootRawType.getDeclaredField(propName).getGenericType();
                        listGenericType = getListGenericArgOrNull(fieldType);
                    } catch (NoSuchFieldException e) {
                        /*no field: skip array and return no property special value*/
                        jsonStream.skipValue();
                        return null;
                    }
                }

                if (listGenericType == null) {
                    throw new IllegalArgumentException("Target type is expected to be parametrized list: List<T>");
                }
                PropertyContext context = calculator.context();
                context.setItemNameAndType(propName, listGenericType);

                jsonStream.beginArray();
                return new Prop<>(propName, null, PropType.ARR_START);
            }
        },

        ARR_END {
            @Override
            <T> Prop<T> resolve(String propName, Type targetType, PropertySlicer<T> calculator) throws IOException {
                PropertyContext context = calculator.context();
                String name = context.getItemName();
                context.clearNameAndType();

                calculator.jsonStream().endArray();
                return new Prop<>(name, null, PropType.ARR_END);
            }
        },

        ARR {
            @Override
            <T> Prop<T> resolve(String propName, Type targetType, PropertySlicer<T> calculator) throws IOException {
                PropertyContext context = calculator.context();
                Type itemType = context.getItemType();
                String itemName = context.getItemName();
                if (itemName == null || itemType == null) {
                    throw new AssertionError("Internal error: itemName and itemType were not set for array item:ARR");
                }
                Object item = calculator.gson().fromJson(calculator.jsonStream(), itemType);

                return new Prop<>(itemName, item, PropType.ARR_PROPERTY);
            }
        };

        private static Type getListGenericArgOrNull(Type targetType) {
            if (targetType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) targetType;
                Type rawType = parameterizedType.getRawType();
                Type[] genericArgs = parameterizedType.getActualTypeArguments();
                if (rawType == List.class) {
                    return genericArgs[0];
                }
            }
            return null;
        }

        private static Type getFieldTypeForTarget(Type targetType, String name) {
            Class<?> rootRawType = Utils.getRawType(targetType);
            try {
                return rootRawType.getDeclaredField(name).getType();
            } catch (NoSuchFieldException e) {
                return null;
            }
        }

        /**
         * @param propName   property name for target type
         * @param targetType type for which properties are resolved
         * @return property, or null if target type does not contain field with name propName
         * @throws IOException
         */
        abstract <T> Prop<T> resolve(String propName,
                                     Type targetType,
                                     PropertySlicer<T> calculator) throws IOException;

    }
}
