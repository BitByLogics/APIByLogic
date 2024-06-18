package net.bitbylogic.apibylogic.util.abstraction;

/**
 * Represents an object that can be enabled or disabled
 */
public interface Toggleable {

    /**
     * Check if the object is enabled or not
     *
     * @return true if the object is enabled
     */
    boolean isEnabled();

    /**
     * Enable the object
     *
     * @return true if the object was successfully enabled
     */
    boolean enable();

    /**
     *  Disable the object
     *
     * @return true if the object was successfully disabled
     */
    boolean disable();

}
