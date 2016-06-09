class ConfigUtil2 {

    private static class BaseObject {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    private static class YAML extends BaseObject {

    }

    private static class XML extends BaseObject {

    }

    public static <T extends BaseObject> T getConfig(String file, Class<T> t) {
        BaseObject b;
        if (t.isAssignableFrom(YAML.class)) {
            b = new YAML();
        } else if (t.isAssignableFrom(XML.class)) {
            b = new XML();
        } else {
            throw new IllegalArgumentException("Unsupported type " + t.getTypeName());
        }
        b.setValue("sdsdsdsd");

        return t.cast(b);
    }

    public static void main(String[] args) {
        YAML yml = getConfig("foo", YAML.class);

        XML xml = getConfig("foo", XML.class);

        String ymlValue = yml.getValue();
        System.out.println("ymlValue = " + ymlValue);
        String xmlValue = xml.getValue();
        System.out.println("xmlValue = " + xmlValue);
    }
}