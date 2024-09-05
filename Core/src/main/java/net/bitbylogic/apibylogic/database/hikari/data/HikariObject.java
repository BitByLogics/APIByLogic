package net.bitbylogic.apibylogic.database.hikari.data;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
public class HikariObject {

    @Setter(AccessLevel.PROTECTED)
    protected HikariTable owningTable;

    public void save() {
        if(owningTable == null) {
            return;
        }

        owningTable.save(this);
    }

    public void delete() {
        if(owningTable == null) {
            return;
        }

        owningTable.delete(this);
    }

}
