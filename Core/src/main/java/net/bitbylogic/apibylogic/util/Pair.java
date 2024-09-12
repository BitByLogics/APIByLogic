package net.bitbylogic.apibylogic.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Pair<K, V> {

    private K key;
    private V value;

    @Override
    public boolean equals(final Object o) {
        if (o == null) return false;
        if (!(o instanceof Pair<?, ?> pair)) return false;
        return pair.getKey().equals(key) && pair.getValue().equals(value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

}
