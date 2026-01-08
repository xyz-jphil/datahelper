package xyz.jphil.datahelper;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface DataHelper_I<E extends DataHelper_I<E>> {
    Map<String, Object> toMap();
    E fromValueProvider(Function<String, Object> valueProvider);
    E fromTypedValueProvider(BiFunction<String, Class<?>, Object> typedValueProvider);

    @SuppressWarnings("unchecked")
    default E fromMap(Map<String, Object> map) {
        return fromValueProvider(map::get);
    }
}
