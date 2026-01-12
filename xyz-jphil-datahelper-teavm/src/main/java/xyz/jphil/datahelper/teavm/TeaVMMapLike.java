package xyz.jphil.datahelper.teavm;

import xyz.jphil.datahelper.MapLike;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSMapLike;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * TeaVM implementation of MapLike that wraps JSMapLike&lt;JSObject&gt;.
 * Handles type conversions between JavaScript and Java types.
 *
 * <p>This implementation provides automatic type conversion for JavaScript primitives:
 * <ul>
 *   <li>JSNumber → Integer/Long/Double/Float (using type hints from getTyped())</li>
 *   <li>JS String → Java String</li>
 *   <li>JS Boolean → Java Boolean</li>
 *   <li>JS Object → TeaVMMapLike (nested)</li>
 *   <li>JS Array → TeaVMArrayLike</li>
 * </ul>
 */
public class TeaVMMapLike implements MapLike {
    private final JSMapLike<JSObject> jsMap;

    public TeaVMMapLike(JSMapLike<JSObject> jsMap) {
        this.jsMap = jsMap;
    }

    @Override
    public Object get(String key) {
        JSObject jsValue = jsMap.get(key);
        return convertFromJS(jsValue, null);
    }

    @Override
    public Object getTyped(String key, Class<?> expectedType) {
        JSObject jsValue = jsMap.get(key);
        return convertFromJS(jsValue, expectedType);  // Use type hint!
    }

    @Override
    public void set(String key, Object value) {
        JSObject jsValue = convertToJS(value);
        jsMap.set(key, jsValue);
    }

    @Override
    public boolean has(String key) {
        return !isUndefined(jsMap.get(key));
    }

    @Override
    public Iterable<String> keys() {
        String[] keysArray = getKeys(jsMap);
        
        return Arrays.asList(keysArray);
    }

    /**
     * Unwrap to get the underlying JSMapLike.
     * Useful for direct JavaScript interop.
     */
    public JSMapLike<JSObject> unwrap() {
        return jsMap;
    }

    // ========== Type Conversion: JS → Java ==========

    /**
     * Convert JavaScript value to Java object.
     *
     * @param jsValue the JavaScript value
     * @param expectedType optional type hint for proper conversion (especially for numbers)
     * @return converted Java object, or null if value is null/undefined
     */
    static Object convertFromJS(JSObject jsValue, Class<?> expectedType) {
        if (isNullOrUndefined(jsValue)) {
            return null;
        }

        // If we have a type hint, use it for proper conversion
        if (expectedType != null) {
            if (expectedType == Integer.class || expectedType == int.class) {
                return isNumber(jsValue) ? Integer.valueOf(numberToInt(jsValue)) : null;
            }
            if (expectedType == Long.class || expectedType == long.class) {
                return isNumber(jsValue) ? Long.valueOf(numberToLong(jsValue)) : null;
            }
            if (expectedType == Double.class || expectedType == double.class) {
                return isNumber(jsValue) ? Double.valueOf(numberToDouble(jsValue)) : null;
            }
            if (expectedType == Float.class || expectedType == float.class) {
                return isNumber(jsValue) ? Float.valueOf(numberToFloat(jsValue)) : null;
            }
            if (expectedType == Short.class || expectedType == short.class) {
                return isNumber(jsValue) ? Short.valueOf(numberToShort(jsValue)) : null;
            }
            if (expectedType == Byte.class || expectedType == byte.class) {
                return isNumber(jsValue) ? Byte.valueOf(numberToByte(jsValue)) : null;
            }
            if (expectedType == Boolean.class || expectedType == boolean.class) {
                return isBoolean(jsValue) ? Boolean.valueOf(booleanValue(jsValue)) : null;
            }
            if (expectedType == String.class) {
                return isString(jsValue) ? stringValue(jsValue) : null;
            }
        }

        // No type hint, or type not matched - infer type from JS value
        if (isString(jsValue)) {
            return stringValue(jsValue);
        }

        if (isNumber(jsValue)) {
            // Default: return as Double (can be converted later via convertType)
            return Double.valueOf(numberToDouble(jsValue));
        }

        if (isBoolean(jsValue)) {
            return Boolean.valueOf(booleanValue(jsValue));
        }

        if (isArray(jsValue)) {
            return new TeaVMArrayLike((JSArray<JSObject>) jsValue);
        }

        if (isObject(jsValue)) {
            return new TeaVMMapLike((JSMapLike<JSObject>) jsValue);
        }

        // Unknown type, return as JSObject
        return jsValue;
    }

    // ========== Type Conversion: Java → JS ==========

    /**
     * Convert Java object to JavaScript value.
     *
     * @param value the Java value
     * @return JavaScript representation
     */
    static JSObject convertToJS(Object value) {
        if (value == null) {
            return nullValue();
        }

        if (value instanceof String) {
            return stringToJS((String) value);
        }

        if (value instanceof Number) {
            return numberToJS((Number) value);
        }

        if (value instanceof Boolean) {
            return booleanToJS((Boolean) value);
        }

        if (value instanceof TeaVMArrayLike) {
            return ((TeaVMArrayLike) value).unwrap();
        }

        if (value instanceof TeaVMMapLike) {
            return ((TeaVMMapLike) value).unwrap();
        }

        if (value instanceof List) {
            // Convert Java List to JS Array
            List<?> list = (List<?>) value;
            JSArray<JSObject> jsArray = createArray();
            for (int i = 0; i < list.size(); i++) {
                arraySet(jsArray, i, convertToJS(list.get(i)));
            }
            return jsArray;
        }

        // Should not happen for DTOs
        throw new IllegalArgumentException("Cannot convert to JS: " + value.getClass());
    }

    // ========== JSBody Native Methods ==========

    @JSBody(params = {"obj"}, script = "return Object.keys(obj);")
    private static native String[] getKeys(JSMapLike<JSObject> obj);

    @JSBody(params = {"value"}, script = "return value === null || value === undefined;")
    static native boolean isNullOrUndefined(JSObject value);

    @JSBody(params = {"value"}, script = "return value === undefined;")
    static native boolean isUndefined(JSObject value);

    @JSBody(params = {"value"}, script = "return typeof value === 'string';")
    static native boolean isString(JSObject value);

    @JSBody(params = {"value"}, script = "return typeof value === 'number';")
    static native boolean isNumber(JSObject value);

    @JSBody(params = {"value"}, script = "return typeof value === 'boolean';")
    static native boolean isBoolean(JSObject value);

    @JSBody(params = {"value"}, script = "return Array.isArray(value);")
    static native boolean isArray(JSObject value);

    @JSBody(params = {"value"}, script = "return typeof value === 'object' && value !== null && !Array.isArray(value);")
    static native boolean isObject(JSObject value);

    @JSBody(params = {"value"}, script = "return value;")
    static native String stringValue(JSObject value);

    @JSBody(params = {"value"}, script = "return value | 0;")
    static native int numberToInt(JSObject value);

    @JSBody(params = {"value"}, script = "return value | 0;")
    static native long numberToLong(JSObject value);

    @JSBody(params = {"value"}, script = "return +value;")
    static native double numberToDouble(JSObject value);

    @JSBody(params = {"value"}, script = "return +value;")
    static native float numberToFloat(JSObject value);

    @JSBody(params = {"value"}, script = "return value | 0;")
    static native short numberToShort(JSObject value);

    @JSBody(params = {"value"}, script = "return value | 0;")
    static native byte numberToByte(JSObject value);

    @JSBody(params = {"value"}, script = "return value;")
    static native boolean booleanValue(JSObject value);

    @JSBody(params = {}, script = "return null;")
    static native JSObject nullValue();

    @JSBody(params = {"str"}, script = "return str;")
    static native JSObject stringToJS(String str);

    @JSBody(params = {"num"}, script = "return +num;")
    static native JSObject numberToJS(Number num);

    @JSBody(params = {"bool"}, script = "return bool;")
    static native JSObject booleanToJS(Boolean bool);

    @JSBody(params = {}, script = "return [];")
    static native JSArray<JSObject> createArray();

    @JSBody(params = {"array", "index", "value"}, script = "array[index] = value;")
    static native void arraySet(JSArray<JSObject> array, int index, JSObject value);

    /**
     * Wrap a JSMapLike as MapLike.
     */
    public static MapLike wrap(JSMapLike<JSObject> jsMap) {
        return new TeaVMMapLike(jsMap);
    }
}
