/*
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * This is the utility class that provides the capability to get the values from deployment.properties file.
 *
 * @since 5.1.0
 */

@SuppressWarnings("unchecked")
public final class ConfigUtil {

    private static Logger logger = Logger.getLogger(ConfigUtil.class.getName());

    private static final String DEPLOYMENT_PROPERTIES_FILE_NAME = "deployment.properties";
    private static final Map<String, Map<String, String>> deploymentPropertiesMap = readDeploymentFile();

    private ConfigUtil() {

    }

    /**
     * This method will update the values of a given map with the values found with the filename prefix in
     * deployment.properties file.
     *
     * @param currentValuesMap Map containing the current values of configs
     * @param fileName         Name of the config file
     */
    public static Map getConfigs(Map currentValuesMap, String fileName) {

        fileName = "[" + fileName.trim() + "]";

        if (deploymentPropertiesMap.containsKey(fileName)) {

            Map newValuesMap = deploymentPropertiesMap.get(fileName);

            for (Object key : newValuesMap.keySet()) {

                if (currentValuesMap.containsKey(key.toString())) {
                    String newValue = newValuesMap.get(key.toString()).toString();
                    currentValuesMap.put(key.toString(), newValue);
                } else if (key.toString().indexOf('.') != -1) {
                    updateValue(currentValuesMap, key.toString(), newValuesMap.get(key));
                } else {
                    logger.warning(key + " was not found in " + fileName);
                }

            }

        } else {
            logger.info("No new configs found for " + fileName + " in " + DEPLOYMENT_PROPERTIES_FILE_NAME + " file.");
        }

        return currentValuesMap;
    }

    /**
     * This method will return all the values that has filename prefix in deployment.properties file.
     *
     * @param fileName Name of the config file
     */
    public static Map getConfigs(String fileName) {

        Map configs;

        fileName = "[" + fileName.trim() + "]";

        if (deploymentPropertiesMap.containsKey(fileName)) {
            configs = deploymentPropertiesMap.get(fileName);
        } else {
            configs = new HashMap();
        }

        return configs;
    }

    public static String convertMapToYmlString(Map map) {
        String yamlString = "";
        Yaml yaml = new Yaml();
        yamlString = yaml.dumpAsMap(map);
        return yamlString;
    }

    /**
     * This method will return the new value of a key if the key is available in deployment.properties file. If it is
     * not found, this method returns the defaultValue given as a parameter.
     *
     * @param key          Key of the config property
     * @param defaultValue Default value to return in case if the key is not found
     */
    public static <T> T getValue(String key, T defaultValue) {

        //todo
        //Regex to identify correctly formatted key - ^(\[\w+\.\w+\]){1}(\/\w+)(\..+)*=(.+)$
        //Reges to identify formats with arrays - ^(\[\w+\.\w+\]){1}(\/\w+)(\.\w+(\[@.+=\d+\])*)*=(.+)$

        //basic Regex (\[.+\]){1}(\/.+)(\..+)*=(.+)

        T newValue = null;

        String[] temp = key.split("/");
        String fileName = temp[0];
        String xpath = temp[1];

        if (deploymentPropertiesMap.containsKey(fileName)) {
            Map valuesMap = deploymentPropertiesMap.get(fileName);
            if (valuesMap.containsKey(xpath)) {
                newValue = (T)valuesMap.get(xpath);
            }
        } else {
            logger.info("No new configs found for " + fileName + " in " + DEPLOYMENT_PROPERTIES_FILE_NAME + " file.");
        }

        if (newValue == null) {
            return defaultValue;
        } else {
            //Need to cast the values
//            if (canParseToInt(newValue)) {
//                newValue = Integer.parseInt(newValue.toString());
//            } else if (canParseToDouble(newValue)) {
//                newValue = Double.parseDouble(newValue.toString());
//            } else if (canParseToBoolean(newValue)) {
//                newValue = Boolean.parseBoolean(newValue.toString());
//            }

            return newValue;
        }
    }

    public static Map loadXMLAsMap(String xmlString) {

        Map map = null;

        try {
            JSONObject xmlJSONObj = XML.toJSONObject(xmlString);
            String jsonPrettyPrintString = xmlJSONObj.toString();
            System.out.println(jsonPrettyPrintString);

            Yaml yaml = new Yaml();

            //            Object yamlObj = yaml.load(jsonPrettyPrintString);
            //            System.out.println(yamlObj);

            map = yaml.loadAs(jsonPrettyPrintString, Map.class);

        } catch (JSONException e) {

        }

        return map;
    }

    //todo
//    private static String convertXMLToYAML(String xmlString) {
//
//        String yamlString = "";
//
//        try {
//            JSONObject xmlJSONObj = XML
//                    .toJSONObject("<?xml version=\"1.0\" ?><test attrib=\"moretest\">Turn this to JSON</test>");
//            String jsonPrettyPrintString = xmlJSONObj.toString();
//            System.out.println(jsonPrettyPrintString);
//
//            Yaml yaml = new Yaml();
//
//            Object yamlObj = yaml.load(jsonPrettyPrintString);
//            System.out.println(yamlObj);
//
//        } catch (JSONException e) {
//
//        }
//
//        return yamlString;
//    }

    //todo
    //    public static Map replaceAllPlaceholders(Map map) {
    //        return map;
    //    }

    private static void updateValue(Map currentValues, String xpath, Object newValue) {

        int index = xpath.indexOf('.');

        if (index != -1) {

            String firstKey = xpath.substring(0, index);
            String secondKey = xpath.substring(index + 1);

            if (currentValues.containsKey(firstKey)) {

                Object currentValue = currentValues.get(firstKey);

                //since we had a . in the path, we need to traverse more. So the value should be a map
                if (currentValue instanceof Map) {
                    updateValue((Map) currentValue, secondKey, newValue);
                } else {
                    logger.warning("Error: can traverse more. But end node found");
                }
            } else {
                logger.warning(firstKey + " not found.");
            }

        } else {

            if (currentValues.containsKey(xpath)) {

                Object currentValue = currentValues.get(xpath);

                if (currentValue instanceof Integer) {

                    if (canParseToInt(newValue)) {
                        int value = Integer.parseInt(newValue.toString());
                        currentValues.put(xpath, value);
                    } else {
                        currentValues.put(xpath, newValue);
                    }

                } else if (currentValue instanceof Double) {

                    if (canParseToDouble(newValue)) {
                        double value = Double.parseDouble(newValue.toString());
                        currentValues.put(xpath, value);
                    } else {
                        currentValues.put(xpath, newValue);
                    }

                } else if (currentValue instanceof Boolean) {

                    if (canParseToBoolean(newValue)) {
                        boolean value = Boolean.parseBoolean(newValue.toString());
                        currentValues.put(xpath, value);
                    } else {
                        currentValues.put(xpath, newValue);
                    }

                } else {
                    currentValues.put(xpath, newValue);
                }
            } else {
                logger.info("Final path,[" + xpath + "] was not found");
            }
        }
    }

    //    private static Object getValue(Map map, String xpath) {
    //        int index = xpath.indexOf('.');
    //
    //        if (index != -1) {
    //            String firstKey = xpath.substring(0, index);
    //            String secondKey = xpath.substring(index + 1);
    //
    //            if (map.containsKey(firstKey)) {
    //                Object currentValue = map.get(firstKey);
    //                if (currentValue instanceof Map) {
    //                    logger.info("Map found");
    //                    return getValue((Map) currentValue, secondKey);
    //                } else {
    //                    logger.warning("Error: can traverse more. But end node found");
    //                    return null;
    //                }
    //            } else {
    //                return null;
    //            }
    //        } else {
    //            if (map.containsKey(xpath)) {
    //                return map.get(xpath);
    //            } else {
    //                return null;
    //            }
    //        }
    //    }

    private static boolean canParseToInt(Object obj) {

        boolean canParse = true;
        try {
            Integer.parseInt(obj.toString());
        } catch (NumberFormatException exception) {
            canParse = false;
        }
        return canParse;
    }

    private static boolean canParseToDouble(Object obj) {

        boolean canParse = true;
        try {
            Double.parseDouble(obj.toString());
        } catch (NumberFormatException exception) {
            canParse = false;
        }
        return canParse;
    }

    private static boolean canParseToBoolean(Object obj) {
        return "true".equals(obj.toString().toLowerCase()) || "false".equals(obj.toString().toLowerCase());
    }

    private static Map<String, Map<String, String>> readDeploymentFile() {

        Map<String, Map<String, String>> tempPropertiesMap = new HashMap<String, Map<String, String>>();

        Properties deploymentProperties = new Properties();
        InputStream input = null;

        try {
            File file = new File(DEPLOYMENT_PROPERTIES_FILE_NAME);

            if (file.exists()) {

                input = new FileInputStream(file);

                deploymentProperties.load(input);

                for (Object key : deploymentProperties.keySet()) {

                    String value = deploymentProperties.getProperty(key.toString());
                    String[] temp = key.toString().split("/");
                    String fileName = temp[0];
                    String xpath = temp[1];

                    if (tempPropertiesMap.containsKey(fileName)) {
                        Map<String, String> tempMap = tempPropertiesMap.get(fileName);
                        tempMap.put(xpath, value);
                    } else {
                        Map<String, String> tempMap = new HashMap<String, String>();
                        tempMap.put(xpath, value);
                        tempPropertiesMap.put(fileName, tempMap);
                    }
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
