package xyz.jphil.datahelper.teavm;

import xyz.jphil.datahelper.ArrayLike;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSArray;

/**
 * TeaVM implementation of ArrayLike that wraps JSArray&lt;JSObject&gt;.
 * Handles type conversions between JavaScript and Java types.
 *
 * <p>This implementation reuses the conversion logic from TeaVMMapLike
 * to ensure consistent type handling across nested structures.</p>
 */
public class TeaVMArrayLike implements ArrayLike {
    private final JSArray<JSObject> jsArray;

    public TeaVMArrayLike(JSArray<JSObject> jsArray) {
        this.jsArray = jsArray;
    }

    @Override
    public Object get(int index) {
        JSObject jsValue = jsArray.get(index);
        return TeaVMMapLike.convertFromJS(jsValue, null);  // Reuse conversion
    }

    @Override
    public Object getTyped(int index, Class<?> expectedType) {
        JSObject jsValue = jsArray.get(index);
        return TeaVMMapLike.convertFromJS(jsValue, expectedType);  // Use type hint
    }

    @Override
    public void set(int index, Object value) {
        JSObject jsValue = TeaVMMapLike.convertToJS(value);
        jsArray.set(index, jsValue);
    }

    @Override
    public int size() {
        return jsArray.getLength();
    }

    /**
     * Unwrap to get the underlying JSArray.
     * Useful for direct JavaScript interop.
     */
    public JSArray<JSObject> unwrap() {
        return jsArray;
    }

    /**
     * Wrap a JSArray as ArrayLike.
     */
    public static ArrayLike wrap(JSArray<JSObject> jsArray) {
        return new TeaVMArrayLike(jsArray);
    }
}
