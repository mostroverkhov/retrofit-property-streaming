package com.github.mostroverkhov.retrofit_property_streaming.model;

import java.util.Arrays;

/**
 * Created by Maksym Ostroverkhov on 11.06.2016.
 */
public class Prop<OwnerType> {
    private final PropType[] propTypes;
    private final String name;
    private final Object value;

    public Prop(String name, Object value, PropType... propTypes) {
        this.propTypes = propTypes;
        this.name = name;
        this.value = value;
    }

    public Prop(PropType... propTypes) {
        this(null, null, propTypes);
    }

    public PropType[] getPropTypes() {
        return propTypes;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    @SuppressWarnings("unchecked")
    public <T> T getTypedValue() {
        return (T) value;
    }

    public boolean isSimpleProperty() {
        return contains(propTypes, PropType.PROPERTY);
    }

    public boolean isArrayItem() {
        return contains(propTypes, PropType.ARR_PROPERTY);
    }

    public boolean isArrayItemStart() {
        return contains(propTypes, PropType.ARR_START);
    }

    public boolean isArrayItemEnd() {
        return contains(propTypes, PropType.ARR_END);
    }

    public boolean isDocumentEnd() {
        return contains(propTypes, PropType.DOC_END);
    }

    private boolean contains(Object[] arr, Object val) {

        for (Object o : arr) {
            if (val.equals(o)) {
                return true;
            }
        }
        return false;
    }

    public boolean isNull() {
        return value == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Prop<?> prop = (Prop<?>) o;

        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(propTypes, prop.propTypes)) return false;
        if (!name.equals(prop.name)) return false;
        return value != null ? value.equals(prop.value) : prop.value == null;

    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(propTypes);
        result = 31 * result + name.hashCode();
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Prop{");
        sb.append("propTypes=").append(Arrays.toString(propTypes));
        sb.append(", name='").append(name).append('\'');
        sb.append(", value=").append(value);
        sb.append('}');
        return sb.toString();
    }
}
