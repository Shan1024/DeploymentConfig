package org.wso2.carbon.kernel.core.util.configfiletypes;

/**
 * This class is used to return the new config values to the component developer.
 */
public abstract class AbstractConfigFileFormat {

    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String newValue) {
        this.value = newValue;
    }

}
