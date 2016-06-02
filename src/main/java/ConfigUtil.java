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

                logger.info("Key: " + key);

                if (currentValuesMap.containsKey(key.toString())) {

                    String newValue = newValuesMap.get(key.toString()).toString();
                    currentValuesMap.put(key.toString(), newValue);

                    logger.info(key + " was found in the " + fileName + " file. Current value replaced with " +
                            newValue);

                } else if (key.toString().indexOf('.') != -1) {
                    logger.info("Processing [" + key + "]");
                    mapToValue(currentValuesMap, key.toString(), newValuesMap.get(key));
                } else {
                    logger.warning(key + " was not found in " + fileName);
                }

            }

        } else {
            logger.info("No new configs found for " + fileName + " in " + DEPLOYMENT_PROPERTIES_FILE_NAME + " file.");
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

    //Regex (\[.+\]){1}(\/.+)(\..+)*=(.+)
    public static String getValue(String key, String defaultValue) {
        String newValue = null;

        if (newValue == null) {
            return defaultValue;
        } else {
            return newValue;
        }
    }

    public static String convertMapToYmlString(Map map) {
        String yamlString = "";
        Yaml yaml = new Yaml();
        yamlString = yaml.dumpAsMap(map);
        return yamlString;
    }

    public static Map replaceAllPlaceholders(Map map) {
        return map;
    }

    private static void mapToValue(Map currentValues, String xpath, Object newValue) {

        logger.info("xpath: " + xpath);

        int index = xpath.indexOf('.');
        logger.info("Index: " + index);

        if (index != -1) {

            logger.info("More paths to travel");

            String firstKey = xpath.substring(0, index);
            logger.info("firstKey: " + firstKey);

            String secondKey = xpath.substring(index + 1);
            logger.info("secondKey: " + secondKey);

            if (currentValues.containsKey(firstKey)) {

                logger.info(firstKey + " was found");

                Object currentValue = currentValues.get(firstKey);
                logger.info("currentValue: " + currentValue);

                //since we had a . in the path, we need to traverse more. So the value should be a map
                if (currentValue instanceof Map) {
                    logger.info("Map found");
                    mapToValue((Map) currentValue, secondKey, newValue);
                } else {
                    logger.warning("Error: can traverse more. But end node found");
                }

            } else {
                logger.warning(firstKey + " not found.");
            }

        } else {
            logger.info("Final path found");

            if (currentValues.containsKey(xpath)) {
                logger.info("Final path,[" + xpath + "] was found. Replacing the value.");
                currentValues.put(xpath, newValue);
            } else {
                logger.info("Final path,[" + xpath + "] was not found");

            }
        }

    }

    private static Map<String, Map<String, String>> readDeploymentFile() {

        logger.info("readDeploymentFile() called");

        Map<String, Map<String, String>> tempPropertiesMap = new HashMap<String, Map<String, String>>();

        Properties deploymentProperties = new Properties();
        InputStream input = null;

        try {
            File file = new File(DEPLOYMENT_PROPERTIES_FILE_NAME);

            if (file.exists()) {

                input = new FileInputStream(file);

                deploymentProperties.load(input);

                logger.info("Properties loaded successfully from " + DEPLOYMENT_PROPERTIES_FILE_NAME);
                logger.info("Loaded Properties: \n" + deploymentProperties);
                logger.info("Creating properties map");

                for (Object key : deploymentProperties.keySet()) {
                    logger.info("key: " + key);

                    String value = deploymentProperties.getProperty(key.toString());
                    logger.info("value: " + value);

                    String[] temp = key.toString().split("/");
                    String fileName = temp[0];
                    String xpath = temp[1];

                    logger.info("fileName: " + fileName);
                    logger.info("xpath: " + xpath);

                    if (tempPropertiesMap.containsKey(fileName)) {
                        logger.info("Properties for current file already available in the map. Adding new entry to "
                                + "the map.");
                        Map<String, String> tempMap = tempPropertiesMap.get(fileName);
                        tempMap.put(xpath, value);
                    } else {
                        logger.info("Properties for current file not found in map. Creating a new sub map and "
                                + "entering value.");
                        Map<String, String> tempMap = new HashMap<String, String>();
                        tempMap.put(xpath, value);
                        tempPropertiesMap.put(fileName, tempMap);
                    }
                    logger.info("[" + key + "=" + value + "] successfully processed --------------------------------");
                }

            } else {
                logger.warning(DEPLOYMENT_PROPERTIES_FILE_NAME + " file not found at " + file.getAbsolutePath());
            }
        } catch (IOException ioException) {
            logger.warning("Error occurred during reading the " + DEPLOYMENT_PROPERTIES_FILE_NAME + " file. Error: "
                    + ioException.toString());
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ioException2) {
                    logger.warning("Error occurred while closing the InputStream. Error: " + ioException2.toString());
                }
            }
        }

        return tempPropertiesMap;
    }

}
