package net.bitbylogic.apibylogic.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class Pair<K, V> {

    private K key;
    private V value;

}
