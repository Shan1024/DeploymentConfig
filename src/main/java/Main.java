import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by shan on 6/1/16.
 */
public class Main {

    private static Map<String, Map<String, String>> deploymentPropertiesMap;

    public static void main(String[] args) {

        deploymentPropertiesMap = new HashMap<String, Map<String, String>>();

        Properties deploymentProperties = new Properties();
        InputStream input = null;

        try {
            File file = new File("deployment.properties");

            input = new FileInputStream(file);

            deploymentProperties.load(input);
            System.out.println("deploymentProperties: " + deploymentProperties);
            System.out.println();

            for (Object key : deploymentProperties.keySet()) {
                System.out.println("key: " + key);
                String value = deploymentProperties.getProperty(key.toString());
                System.out.println("value: " + value);

                String[] temp = key.toString().split("/");
                String fileName = temp[0];
                String xpath = temp[1];

                System.out.println("fileName: " + fileName);
                System.out.println("xpath: " + xpath);

                if (deploymentPropertiesMap.containsKey(fileName)) {
                    Map<String, String> tempMap = deploymentPropertiesMap.get(fileName);
                    tempMap.put(xpath, value);
                    System.out.println(tempMap);
                } else {
                    Map<String, String> tempMap = new HashMap<String, String>();
                    tempMap.put(xpath, value);
                    deploymentPropertiesMap.put(fileName, tempMap);
                }
                System.out.println();
            }

            System.out.println("deploymentPropertiesMap: " + deploymentPropertiesMap);

            Map carbonYMLMap = readYAML();
            System.out.println("OldMap: " + carbonYMLMap);

            Map newMap = getDeploymentValues(carbonYMLMap, "carbon.yml");
            System.out.println("NewMap: " + newMap);

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private static Map getDeploymentValues(Map map, String fileName) {

        fileName = "[" + fileName.trim() + "]";

        if (deploymentPropertiesMap.containsKey(fileName)) {

            Map newValuesMap = deploymentPropertiesMap.get(fileName);

            for (Object key : newValuesMap.keySet()) {

                if (map.containsKey(key.toString())) {

                    Object value = map.get(key.toString());

                    if (value instanceof String) {
                        map.put(key.toString(), newValuesMap.get(key.toString()));
                    } else if (value instanceof Map) {
                        //                        replace((Map)value, deploymentPropertiesMap.get());
                    } else {
                        System.out.println("Unknown type");
                    }

                } else if (key.toString().indexOf('.') != -1) {
                    mapToValue(map, key.toString(), newValuesMap.get(key));
                } else {
                    System.out.println(key + " was not found in " + fileName);
                }

            }
        } else {
            System.out.println("No new config found for " + fileName + " in deployment.properties file");
        }

        return map;
    }

    private static void mapToValue(Map oldValues, String xpath, Object newValue) {

        int index = xpath.indexOf('.');

        if (index != -1) {

            String key = xpath.substring(0, index);

            System.out.println(key);

            String newKey = xpath.substring(index + 1);

            //            if (newKey.indexOf('.') != -1) {
            //                if (oldValues.containsKey(key)) {
            //                    Object value = oldValues.get(key);
            //
            //                    if (value instanceof String) {
            //                        System.out.println("Error: can traverse more. But end node found");
            //                    } else if (value instanceof Map) {
            //                        mapToValue((Map) value, newKey, value);
            //                    } else {
            //                        System.out.println("Unkown type");
            //                    }
            //                } else {
            //                    System.out.println(key + " was not found in the map : " + xpath);
            //                }
            //            } else {
            ////                oldValues.put(key,newValue);
            //                Object value = oldValues.get(key);
            //
            //                if (value instanceof String) {
            //                    System.out.println("Error: can traverse more. But end node found");
            //                } else if (value instanceof Map) {
            //                    mapToValue((Map) value, newKey, value);
            //                } else {
            //                    System.out.println("Unkown type");
            //                }
            //
            //            }

            if (oldValues.containsKey(key)) {

                Object value = oldValues.get(key);

                if (value instanceof String) {
                    System.out.println("Error: can traverse more. But end node found");
                } else if (value instanceof Map) {
                    mapToValue((Map) value, newKey, newValue);
                } else {
                    System.out.println("Unknown type");
                }

            } else {
                System.out.println(key + " not found");
            }

        } else {
            if (oldValues.containsKey(xpath)) {
                oldValues.put(xpath, newValue);
            } else {
                System.out.println(xpath + " not found");

            }
        }

    }

    private static Map readYAML() {

        Map map = null;

        String fileName = "carbon.yml";

        Yaml yaml = new Yaml();

        try {
            InputStream ios = new FileInputStream(new File(fileName));

            // Parse the YAML file and return the output as a series of Maps and Lists
            map = yaml.loadAs(ios, Map.class);

            System.out.println(map);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return map;
    }
}