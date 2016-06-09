package ConfigFileTypes;

/**
 * Created by shan on 6/9/16.
 */
public abstract class AbstractConfigFileFormat {

    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
