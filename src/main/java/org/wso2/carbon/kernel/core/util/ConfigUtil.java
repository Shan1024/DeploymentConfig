package org.wso2.carbon.kernel.core.util;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wso2.carbon.kernel.core.util.configfiletypes.AbstractConfigFileType;
import org.wso2.carbon.kernel.core.util.configfiletypes.Properties;
import org.wso2.carbon.kernel.core.util.configfiletypes.YAML;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * This util class provide the ability to override configurations in various components using a single
 * <b>deployment.properties</b> file.
 */
public final class ConfigUtil {

    private static Logger logger = Logger.getLogger(ConfigUtil.class.getName());
    private static final String DEPLOYMENT_PROPERTIES_FILE_NAME = "deployment.properties";
    private static final String ROOT_ELEMENT = "configurations";
    private static final String FILE_REGEX = "\\[.+\\.(yml|properties)\\]";
    private static final String PLACEHOLDER_REGEX = "\\$\\s*(sys|env|sec)\\s*->(.+)";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(PLACEHOLDER_REGEX);
    private static final Map<String, Map<String, String>> deploymentPropertiesMap = readDeploymentFile();

    private enum ConfigFileFormat {
        YML, XML, PROPERTIES
    }

    private ConfigUtil() {

    }

    /**
     * This method reads the provided {@link File} and returns an object of type {@link AbstractConfigFileType}
     * which was given as a input. That object has the new configurations as a String which you can get by
     * {@link AbstractConfigFileType#getValue()} method.
     *
     * @param <T>   The class representing the configuration file type
     * @param file  Input config file
     * @param klass Configuration file type which is a subclass of {@link AbstractConfigFileType}.
     * @return The new configurations in the given format as a String.
     * @see AbstractConfigFileType
     */
    public static <T extends AbstractConfigFileType> T getConfig(File file, Class<T> klass) {

        T newConfigs = null;
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            newConfigs = getConfig(fileInputStream, file.getName(), klass);
        } catch (FileNotFoundException e) {
            logger.log(Level.INFO, "File not found at " + file.getAbsolutePath() + " ; " + e);
        }
        return newConfigs;
    }

    /**
     * This method reads the provided {@link FileInputStream} and returns an object of type
     * {@link AbstractConfigFileType} which was given as a input. That object has the new configurations as a String
     * which you can get by {@link AbstractConfigFileType#getValue()} method.
     *
     * @param <T>         The class representing the configuration file type
     * @param inputStream FileInputStream of the config file
     * @param klass       Configuration file type which is a subclass of {@link AbstractConfigFileType}.
     * @return The new configurations in the given format as a String.
     * @see AbstractConfigFileType
     */
    public static <T extends AbstractConfigFileType> T getConfig(FileInputStream inputStream, String fileName,
            Class<T> klass) {

        String xmlString = "";
        //This variable stores the original file format
        ConfigFileFormat configFileFormat = ConfigFileFormat.XML;
        String convertedString = "";

        /*If an exception throws when processing, don't apply the new configurations and don't convert back to
        original format.*/
        try {
            //properties file can be directly load from the input stream
            if (klass.isAssignableFrom(Properties.class)) {
                //Convert the properties file to XML format
                xmlString = convertPropertiesToXml(inputStream);
                configFileFormat = ConfigFileFormat.PROPERTIES;
            } else {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF8"));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                bufferedReader.close();

                //Convert the file to XML format
                if (klass.isAssignableFrom(org.wso2.carbon.kernel.core.util.configfiletypes.XML.class)) {
                    xmlString = stringBuilder.toString();
                    configFileFormat = ConfigFileFormat.XML;
                } else if (klass.isAssignableFrom(YAML.class)) {
                    xmlString = convertToXml(stringBuilder.toString(), ConfigFileFormat.YML);
                    configFileFormat = ConfigFileFormat.YML;
                }
            }
            //Apply the new configs
            xmlString = applyNewConfigs(xmlString, fileName);

            //Convert xml back to original format
            convertedString = convertToOriginalFormat(xmlString, configFileFormat);

        } catch (UnsupportedEncodingException e) {
            logger.log(Level.SEVERE, "Cannot read FileInputStream:  " + e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IOException:  " + e);
        }

        AbstractConfigFileType baseObject;
        if (klass.isAssignableFrom(YAML.class)) {
            baseObject = new YAML();
        } else if (klass.isAssignableFrom(org.wso2.carbon.kernel.core.util.configfiletypes.XML.class)) {
            baseObject = new org.wso2.carbon.kernel.core.util.configfiletypes.XML();
        } else if (klass.isAssignableFrom(Properties.class)) {
            baseObject = new Properties();
        } else {
            throw new IllegalArgumentException("Unsupported type " + klass.getTypeName());
        }
        baseObject.setValue(convertedString);

        return klass.cast(baseObject);
    }

    private static String convertToOriginalFormat(String xmlString, ConfigFileFormat fileFormat) {
        switch (fileFormat) {
            case XML:
                return xmlString;
            case YML:
                return convertXmlToYaml(xmlString);
            case PROPERTIES:
                return convertXmlToProperties(xmlString);
            default:
                throw new IllegalArgumentException("Unsupported file format: " + fileFormat);
        }
    }

    /**
     * This method returns the configuration value associated with the given key in the <b>deployment.properties</b>
     * file.
     *
     * @param key Key of the configuration
     * @return The new configuration value if the key. If it does not have a new value, {@code null} will be returned.
     */
    public static String getConfig(String key) {
        String returnValue = null;
        int index = key.indexOf("/");
        if (index != -1) {
            String fileName = key.substring(0, index);
            String xpath = key.substring(index);

            //Add root element for yml, properties files
            if (fileName.matches(FILE_REGEX)) {
                xpath = "/" + ROOT_ELEMENT + xpath;
            }
            if (deploymentPropertiesMap.containsKey(fileName)) {
                Map<String, String> configMap = deploymentPropertiesMap.get(fileName);
                if (configMap.containsKey(xpath)) {
                    returnValue = configMap.get(xpath);
                }
            } else {
                logger.log(Level.INFO, xpath + " was not found");
            }
        }
        return returnValue;
    }

    private static String convertToXml(String data, ConfigFileFormat configFileFormat) {
        //No need to convert xml to xml
        String convertedConfig = data;
        switch (configFileFormat) {
            case YML:
                convertedConfig = convertYamlToXml(data);
                break;
            default:
                logger.log(Level.SEVERE, "Unsupported file format: " + configFileFormat);
                break;
        }
        return convertedConfig;
    }

    private static String convertYamlToXml(String data) {
        String jsonString = convertYamlToJson(data);
        return convertJsonToXml(jsonString);
    }

    private static String convertXmlToYaml(String xmlString) {
        String jsonString = convertXmlToJson(xmlString);
        return convertJsonToYaml(jsonString);
    }

    private static String convertYamlToJson(String yamlString) {
        String jsonString;
        Yaml yaml = new Yaml();
        Map map = (Map) yaml.load(yamlString);
        JSONObject jsonObject = new JSONObject(map);
        jsonString = jsonObject.toString();
        return jsonString;
    }

    private static String convertJsonToYaml(String jsonString) {
        Yaml yaml = new Yaml();
        Map map = yaml.loadAs(jsonString, Map.class);
        //Remove root element
        if (map.containsKey(ROOT_ELEMENT)) {
            map = (Map) map.get(ROOT_ELEMENT);
        }
        return yaml.dumpAsMap(map);
    }

    private static String convertJsonToXml(String jsonString) {
        String xmlString = "";
        try {
            JSONObject json = new JSONObject(jsonString);
            xmlString = XML.toString(json);
        } catch (JSONException e) {
            logger.log(Level.SEVERE, "Exception occurred while converting JSON to XML");
        }
        //Need to add a root element
        xmlString = createXmlElement(ROOT_ELEMENT, xmlString);
        return xmlString;
    }

    private static String convertXmlToJson(String xmlString) {
        String jsonString = "";
        try {
            JSONObject xmlJSONObj = XML.toJSONObject(xmlString);
            jsonString = xmlJSONObj.toString();
        } catch (JSONException e) {
            logger.log(Level.SEVERE, "Exception occurred while converting XML to JSON: " + e);
        }
        return jsonString;
    }

    private static String convertPropertiesToXml(InputStream inputStream) {
        String xmlString = "";
        java.util.Properties deploymentProperties = new java.util.Properties();

        try {
            deploymentProperties.load(inputStream);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(xmlString);
            for (Map.Entry<Object, Object> entry : deploymentProperties.entrySet()) {
                stringBuilder.append(createXmlElement(entry.getKey().toString(), entry.getValue().toString()));
            }
            xmlString = stringBuilder.toString();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Exception occurred while converting Properties to XML: " + e);
        }
        //Need to add a root element
        xmlString = createXmlElement(ROOT_ELEMENT, xmlString);
        return prettyFormatXmlString(xmlString);
    }

    private static String convertXmlToProperties(String xmlString) {
        String jsonString = convertXmlToJson(xmlString);
        Yaml yaml = new Yaml();
        Map map = yaml.loadAs(jsonString, Map.class);
        //Remove root element
        if (map.containsKey(ROOT_ELEMENT)) {
            map = (Map) map.get(ROOT_ELEMENT);
        }
        StringBuilder stringBuilder = new StringBuilder();
        String tempString;
        for (Object entryObject : map.entrySet()) {
            Map.Entry entry = (Map.Entry) entryObject;
            tempString = entry.getKey() + " = " + entry.getValue();
            stringBuilder.append(tempString).append("\n");
        }
        return stringBuilder.toString();
    }

    private static String createXmlElement(String tagName, String text) {
        return "<" + tagName + ">" + text + "</" + tagName + ">";
    }

    private static String applyNewConfigs(String xmlString, String fileName) {

        String formattedFileName = "[" + fileName + "]";
        String updatedString = xmlString;

        if (deploymentPropertiesMap.containsKey(formattedFileName)) {
            Map<String, String> newConfigs = deploymentPropertiesMap.get(formattedFileName);
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();

            try {
                DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
                Document doc = docBuilder.parse(new InputSource(new StringReader(xmlString)));
                XPath xPath = XPathFactory.newInstance().newXPath();

                newConfigs.keySet().forEach(xPathKey -> {
                    try {
                        NodeList nodeList = (NodeList) xPath.compile(xPathKey).evaluate(doc, XPathConstants.NODESET);
                        if (nodeList.item(0) != null) {
                            Node firstNode = nodeList.item(0);
                            firstNode.getFirstChild().setNodeValue(newConfigs.get(xPathKey));
                        } else {
                            //If key in deployment.properties not found in the config file
                            logger.log(Level.SEVERE, xPathKey + " was not found in " + fileName);
                            throw new RuntimeException(xPathKey + " was not found in " + fileName);
                        }
                    } catch (XPathExpressionException e) {
                        logger.log(Level.SEVERE, "Exception occurred when applying xpath: " + e);
                    }
                });
                updatedString = convertXMLtoString(doc);
            } catch (ParserConfigurationException | IOException | SAXException e) {
                logger.log(Level.SEVERE, "Exception occurred when building document: " + e);
            }
        } else {
            logger.log(Level.INFO, "New configurations for " + formattedFileName + " was not found in "
                    + DEPLOYMENT_PROPERTIES_FILE_NAME);
        }
        return updatedString;
    }

    private static String prettyFormatXmlString(String input) {
        Source xmlInput = new StreamSource(new StringReader(input));
        return convertXmlSourceToString(xmlInput);
    }

    private static String convertXMLtoString(Document doc) {
        DOMSource domSource = new DOMSource(doc);
        return convertXmlSourceToString(domSource);
    }

    private static String convertXmlSourceToString(Source source) {
        String xmlString = "";
        try {
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", 4);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(source, xmlOutput);
            xmlString = xmlOutput.getWriter().toString();
        } catch (TransformerException e) {
            logger.log(Level.SEVERE, "Exception occurred while converting doc to string: " + e);
        }
        return xmlString;
    }

    private static Map<String, Map<String, String>> readDeploymentFile() {

        Map<String, Map<String, String>> tempPropertiesMap = new HashMap<>();
        java.util.Properties deploymentProperties = new java.util.Properties();
        InputStream input = null;

        try {
            File file = new File(DEPLOYMENT_PROPERTIES_FILE_NAME);

            if (file.exists()) {
                input = new FileInputStream(file);
                deploymentProperties.load(input);

                deploymentProperties.keySet().forEach(key -> {
                    String keyString = key.toString();
                    int index = keyString.indexOf("/");
                    String fileName = keyString.substring(0, index);
                    String xpath = keyString.substring(index);
                    String value = deploymentProperties.getProperty(keyString);

                    //Process value if it contains a $sys, $env, $sec placeholder
                    if (value.matches(PLACEHOLDER_REGEX)) {
                        value = processValue(value);
                    }

                    //Add root element for yml, properties files
                    if (fileName.matches(FILE_REGEX)) {
                        xpath = "/" + ROOT_ELEMENT + xpath;
                    }

                    if (tempPropertiesMap.containsKey(fileName)) {
                        Map<String, String> tempMap = tempPropertiesMap.get(fileName);
                        tempMap.put(xpath, value);
                    } else {
                        Map<String, String> tempMap = new HashMap<>();
                        tempMap.put(xpath, value);
                        tempPropertiesMap.put(fileName, tempMap);
                    }
                });
            } else {
                logger.log(Level.INFO,
                        DEPLOYMENT_PROPERTIES_FILE_NAME + " file not found at " + file.getAbsolutePath());
            }
        } catch (IOException ioException) {
            logger.log(Level.SEVERE, "Error occurred during reading the " + DEPLOYMENT_PROPERTIES_FILE_NAME +
                    " file. Error: " + ioException.toString());
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ioException2) {
                    logger.log(Level.SEVERE, "Error occurred while closing the InputStream: " + ioException2);
                }
            }
        }
        return tempPropertiesMap;
    }

    private static String processValue(String placeholder) {

        String newValue = placeholder;
        if (placeholder != null) {
            Matcher matcher = PLACEHOLDER_PATTERN.matcher(placeholder);
            if (matcher.find()) {
                String key = matcher.group(1).trim();
                String value = matcher.group(2).trim();
                switch (key) {
                    case "env":
                        newValue = System.getenv(value);
                        if (newValue == null) {
                            throw new RuntimeException("Environment Variable " + value + " not found. Processing "
                                    + DEPLOYMENT_PROPERTIES_FILE_NAME + " failed.");
                        }
                        break;
                    case "sys":
                        newValue = System.getProperty(value);
                        if (newValue == null) {
                            throw new RuntimeException("System property " + value + " not found. Processing "
                                    + DEPLOYMENT_PROPERTIES_FILE_NAME + " failed.");
                        }
                        break;
                    case "sec":
                        //todo
                        break;
                    default:
                        throw new RuntimeException("Unidentified placeholder key: " + key);
                }
            }
        }
        return newValue;
    }
}
