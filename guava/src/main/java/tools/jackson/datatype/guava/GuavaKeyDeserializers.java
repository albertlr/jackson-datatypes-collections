package tools.jackson.datatype.guava;

import java.io.Serializable;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.KeyDeserializers;
import tools.jackson.datatype.guava.deser.RangeKeyDeserializer;
import com.google.common.collect.Range;

public class GuavaKeyDeserializers
    implements Serializable, KeyDeserializers
{
    static final long serialVersionUID = 1L;

    @Override
    public KeyDeserializer findKeyDeserializer(JavaType type, DeserializationConfig config,
            BeanDescription.Supplier beanDescRef)
        throws JacksonException
    {
        if (type.isTypeOrSubTypeOf(Range.class)) {
            return new RangeKeyDeserializer(type);
        }
        return null;
    }
}
