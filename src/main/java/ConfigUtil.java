import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Created by shan on 6/2/16.
 */
public final class ConfigUtil {

    private static Logger logger = Logger.getLogger(ConfigUtil.class.getName());

    private static final String DEPLOYMENT_PROPERTIES_FILE_NAME = "deployment.properties";
    private static final Map<String, Map<String, String>> deploymentPropertiesMap = readDeploymentFile();

    //Regex (\[.+\]){1}(\/.+)(\..+)*=(.+)
    private ConfigUtil() {

    }

    public static Map getConfigs(Map currentValuesMap, String fileName) {

        logger.info("Retrieving config data for " + fileName);
        logger.info("Map : " + currentValuesMap);

        fileName = "[" + fileName.trim() + "]";

        if (deploymentPropertiesMap.containsKey(fileName)) {

            logger.info("Config data available for " + fileName + " in " + DEPLOYMENT_PROPERTIES_FILE_NAME);

            Map newValuesMap = deploymentPropertiesMap.get(fileName);
            logger.info("Config data for " + fileName + ": " + newValuesMap);

            for (Object key : newValuesMap.keySet()) {

                if (currentValuesMap.containsKey(key.toString())) {

                    Object currentValue = currentValuesMap.get(key.toString());

                    if (currentValue instanceof String) {
                        currentValuesMap.put(key.toString(), newValuesMap.get(key.toString()));
                    } else {
                        System.out.println("Unknown type");
                    }

                } else if (key.toString().indexOf('.') != -1) {
                    mapToValue(currentValuesMap, key.toString(), newValuesMap.get(key));
                } else {
                    System.out.println(key + " was not found in " + fileName);
                }

            }
        } else {
            System.out.println("No new config found for " + fileName + " in deployment.properties file");
        }

        //temp
        Yaml yaml = new Yaml();
        String output = yaml.dumpAsMap(currentValuesMap);
        System.out.println(output);

        try {
            File newFile = new File("carbon_new.yml");
            FileWriter fileWriter = new FileWriter(newFile);
            fileWriter.write(output);
            fileWriter.flush();

            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //end temp

        return currentValuesMap;
    }

    //    public static String getValue(String key, String defaultValue) {
    //        String newValue = null;
    //
    //        if (newValue == null) {
    //            return defaultValue;
    //        } else {
    //            return newValue;
    //        }
    //    }

    private static Map<String, Map<String, String>> readDeploymentFile() {

        Map<String, Map<String, String>> tempPropertiesMap = new HashMap<String, Map<String, String>>();

        Properties deploymentProperties = new Properties();
        InputStream input = null;

        try {
            File file = new File(DEPLOYMENT_PROPERTIES_FILE_NAME);

            input = new FileInputStream(file);

            deploymentProperties.load(input);
            //            System.out.println("deploymentProperties: " + deploymentProperties);
            //            System.out.println();

            for (Object key : deploymentProperties.keySet()) {
                //                System.out.println("key: " + key);
                String value = deploymentProperties.getProperty(key.toString());
                //                System.out.println("value: " + value);

                String[] temp = key.toString().split("/");
                String fileName = temp[0];
                String xpath = temp[1];

                //                System.out.println("fileName: " + fileName);
                //                System.out.println("xpath: " + xpath);

                if (tempPropertiesMap.containsKey(fileName)) {
                    Map<String, String> tempMap = tempPropertiesMap.get(fileName);
                    tempMap.put(xpath, value);
                    //                    System.out.println(tempMap);
                } else {
                    Map<String, String> tempMap = new HashMap<String, String>();
                    tempMap.put(xpath, value);
                    tempPropertiesMap.put(fileName, tempMap);
                }
                //                System.out.println();
            }

            //            System.out.println("deploymentPropertiesMap: " + tempPropertiesMap);

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
        return tempPropertiesMap;
    }

    private static void mapToValue(Map oldValues, String xpath, Object newValue) {

        int index = xpath.indexOf('.');

        if (index != -1) {

            String key = xpath.substring(0, index);

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

}
