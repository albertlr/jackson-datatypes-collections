package com.fasterxml.jackson.datatype.pcollections.deser;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.databind.type.MapType;

import org.pcollections.PMap;

public abstract class PCollectionsMapDeserializer<T extends PMap<Object, Object>>
    extends StdDeserializer<T>
{
    protected final MapType _mapType;
    
    /**
     * Key deserializer used, if not null. If null, String from JSON
     * content is used as is.
     */
    protected KeyDeserializer _keyDeserializer;

    /**
     * Value deserializer.
     */
    protected ValueDeserializer<?> _valueDeserializer;

    /**
     * If value instances have polymorphic type information, this
     * is the type deserializer that can handle it
     */
    protected final TypeDeserializer _typeDeserializerForValue;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    protected PCollectionsMapDeserializer(MapType type, KeyDeserializer keyDeser,
            TypeDeserializer typeDeser, ValueDeserializer<?> deser)
    {
        super(type);
        _mapType = type;
        _keyDeserializer = keyDeser;
        _typeDeserializerForValue = typeDeser;
        _valueDeserializer = deser;
    }

    @Override // since 2.12
    public LogicalType logicalType() {
        return LogicalType.Map;
    }

    /**
     * Overridable fluent factory method used for creating contextual
     * instances.
     */
    public abstract PCollectionsMapDeserializer<T> withResolved(KeyDeserializer keyDeser,
            TypeDeserializer typeDeser, ValueDeserializer<?> valueDeser);
    
    /*
    /**********************************************************
    /* Validation, post-processing
    /**********************************************************
     */

    /**
     * Method called to finalize setup of this deserializer,
     * after deserializer itself has been registered. This
     * is needed to handle recursive and transitive dependencies.
     */
    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property)
    {
        KeyDeserializer keyDeser = _keyDeserializer;
        ValueDeserializer<?> deser = _valueDeserializer;
        TypeDeserializer typeDeser = _typeDeserializerForValue;
        // Do we need any contextualization?
        if ((keyDeser != null) && (deser != null) && (typeDeser == null)) { // nope
            return this;
        }
        if (keyDeser == null) {
            keyDeser = ctxt.findKeyDeserializer(_mapType.getKeyType(), property);
        }
        if (deser == null) {
            deser = ctxt.findContextualValueDeserializer(_mapType.getContentType(), property);
        }
        if (typeDeser != null) {
            typeDeser = typeDeser.forProperty(property);
        }
        return withResolved(keyDeser, typeDeser, deser);
    }

    /*
    /**********************************************************
    /* Deserialization interface
    /**********************************************************
     */

    /**
     * Base implementation that does not assume specific type
     * inclusion mechanism. Sub-classes are expected to override
     * this method if they are to handle type information.
     */
    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws JacksonException
    {
        // note: call "...FromObject" because expected output structure
        // for value is JSON Object (regardless of contortions used for type id)
        return typeDeserializer.deserializeTypedFromObject(p, ctxt);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        // Ok: must point to START_OBJECT or PROPERTY_NAME
        JsonToken t = p.currentToken();
        if (t == JsonToken.START_OBJECT) { // If START_OBJECT, move to next; may also be END_OBJECT
            t = p.nextToken();
        }
        if (t != JsonToken.PROPERTY_NAME && t != JsonToken.END_OBJECT) {
            return (T) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
        }
        return _deserializeEntries(p, ctxt);
    }

    protected abstract T createEmptyMap();

    protected T _deserializeEntries(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        final KeyDeserializer keyDes = _keyDeserializer;
        final ValueDeserializer<?> valueDes = _valueDeserializer;
        final TypeDeserializer typeDeser = _typeDeserializerForValue;

        T map = createEmptyMap();
        for (; p.currentToken() == JsonToken.PROPERTY_NAME; p.nextToken()) {
            // Must point to field name now
            String fieldName = p.currentName();
            Object key = (keyDes == null) ? fieldName : keyDes.deserializeKey(fieldName, ctxt);
            // And then the value...
            JsonToken t = p.nextToken();
            // 28-Nov-2010, tatu: Should probably support "ignorable properties" in future...
            Object value;
            if (t == JsonToken.VALUE_NULL) {
                map = _handleNull(ctxt, key, _valueDeserializer, map);
                continue;
            }
            if (typeDeser == null) {
                value = valueDes.deserialize(p, ctxt);
            } else {
                value = valueDes.deserializeWithType(p, ctxt, typeDeser);
            }
            @SuppressWarnings("unchecked")
            T newMap = (T) map.plus(key, value);
            map = newMap;
        }
        return map;
    }

    /**
     * Overridable helper method called when a JSON null value is encountered.
     * Since PCollections Maps typically do not allow null values, special handling
     * is needed; default is to simply ignore and skip such values, but alternative
     * could be to throw an exception.
     */
    protected T _handleNull(DeserializationContext ctxt, Object key,
            ValueDeserializer<?> valueDeser, T map)
        throws JacksonException
    {
        // TODO: allow reporting problem via a feature, in future?

        // Actually, first, see if there's an alternative to Java null
        Object nvl = valueDeser.getNullValue(ctxt);
        if (nvl != null) {
            @SuppressWarnings("unchecked")
            T newMap = (T) map.plus(key, nvl);
            return newMap;
        } else {
            return map;
        }
    }
}
