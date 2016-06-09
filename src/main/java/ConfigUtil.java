import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.yaml.snakeyaml.Yaml;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

public class ConfigUtil {

    private static Logger logger = Logger.getLogger(ConfigUtil.class.getName());
    private static final String DEPLOYMENT_PROPERTIES_FILE_NAME = "deployment.properties";
    private static final String ROOT_ELEMENT = "configurations";
    private static final Map<String, Map<String, String>> deploymentPropertiesMap = readDeploymentFile();
    private static final String FILE_REGEX = "\\[.+\\.(yml|properties)\\]";

    private ConfigUtil() {

    }

    public static String getConfig(File file, ConfigFileFormat configFileFormat) {

        String newConfigXmlString = "";
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            newConfigXmlString = getConfig(fileInputStream, file.getName(), configFileFormat);
        } catch (FileNotFoundException e) {
            logger.warning("Ex: " + e);
        }

        return newConfigXmlString;
    }

    public static String getConfig(FileInputStream inputStream, String fileName, ConfigFileFormat configFileFormat) {

        String newConfigXmlString = "";

        try {
            if (configFileFormat != ConfigFileFormat.PROPERTIES) {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                bufferedReader.close();
                //Convert the file to FileTypes.XML format
                newConfigXmlString = convertToXml(stringBuilder.toString(), configFileFormat);
            } else {
                //Convert the properties file to FileTypes.XML format
                newConfigXmlString = convertPropertiesToXml(inputStream);
            }

            //Apply the new configs
            newConfigXmlString = applyNewConfigs(newConfigXmlString, fileName);

        } catch (FileNotFoundException e) {
            logger.warning("File not found: " + e);
        } catch (IOException e) {
            logger.warning("IO Ex: " + e);
        }

        return newConfigXmlString;
    }

    //  \${(sys|sec|env)(:\w+)(\.\w+)*} - regex to identify placeholder values
    public static String getConfig(String key) {

        String returnValue = null;

        int index = key.indexOf("/");
        if (index != -1) {
            String fileName = key.substring(0, index);
            String xpath = key.substring(index);

            if (deploymentPropertiesMap.containsKey(fileName)) {

                Map<String, String> configMap = deploymentPropertiesMap.get(fileName);

                if (configMap.containsKey(xpath)) {
                    returnValue = configMap.get(xpath);
                }
            } else {
                logger.warning(xpath + " was not found");
            }
        }
        return returnValue;
    }

    //    public void test() {
    //                ObjectMapper wrapping = rootMapper();
    //
    //                String json = wrapping.writer().withRootName("something").writeValueAsString(new Bean());
    //                //        assertEquals("{\"something\":{\"a\":3}}", json);
    //                json = wrapping.writer().withRootName("").writeValueAsString(new Bean());
    //                //        assertEquals("{\"a\":3}", json);
    //
    ////        String TEST_XML_STRING = "<?xml version=\"1.0\" ?><test attrib=\"moretest\">Turn this to JSON</test>";
    ////
    ////        try {
    ////            JSONObject xmlJSONObj = FileTypes.XML.toJSONObject(TEST_XML_STRING);
    ////            String jsonPrettyPrintString = xmlJSONObj.toString(4);
    ////
    ////            System.out.println(jsonPrettyPrintString);
    ////        } catch (JSONException je) {
    ////            System.out.println(je.toString());
    ////        }
    //
    //    }
    //
    //    private ObjectMapper rootMapper() {
    //        ObjectMapper mapper = new ObjectMapper();
    //        mapper.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
    //        return mapper;
    //    }

    private static String convertToXml(String data, ConfigFileFormat configFileFormat) {
        //No need to convert xml to xml
        String convertedConfig = data;
        switch (configFileFormat) {
            case YML:
                convertedConfig = convertYamlToXml(data);
                break;
            default:
                logger.warning("Unsupported file format: " + configFileFormat);
                break;
        }
        return convertedConfig;
    }

    private static String convertYamlToXml(String data) {

        //Convert Yaml to Json
        String jsonString = convertYamlToJson(data);
        //Convert Json to Xml
        String xmlString = convertJsonToXML(jsonString);
        logger.info("xmlString: " + xmlString);
        return xmlString;
    }

    private static String convertYamlToJson(String yamlString) {

        String jsonString = "";
        Yaml yaml = new Yaml();
        Map<String, Object> map = (Map<String, Object>) yaml.load(yamlString);
        JSONObject jsonObject = new JSONObject(map);
        jsonString = jsonObject.toString();
        return jsonString;
    }

    private static String convertJsonToXML(String jsonString) {

        String xmlString = "";

        try {
            JSONObject json = new JSONObject(jsonString);
            xmlString = XML.toString(json);
        } catch (JSONException e) {
            logger.warning("Ex: " + e);
        }
        //Need to add a root element
        xmlString = createXmlElement(ROOT_ELEMENT, xmlString);
        return xmlString;
    }

    private static String convertPropertiesToXml(InputStream inputStream) {

        String xmlString = "";
        Properties deploymentProperties = new Properties();
        try {
            deploymentProperties.load(inputStream);
            for (Object key : deploymentProperties.keySet()) {
                xmlString = xmlString + createXmlElement(key.toString(), deploymentProperties.get(key).toString());
            }
        } catch (IOException e) {
            logger.warning("Ex: " + e);
        }
        //Need to add a root element
        xmlString = createXmlElement(ROOT_ELEMENT, xmlString);
        return prettyFormatXmlString(xmlString);
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

                newConfigs.keySet().forEach(key -> {
                    logger.info("key: " + key);
                    try {
                        NodeList nodeList = (NodeList) xPath.compile(key).evaluate(doc, XPathConstants.NODESET);
                        if (nodeList.item(0) != null) {
                            Node firstNode = nodeList.item(0);
                            firstNode.getFirstChild().setNodeValue(newConfigs.get(key));
                        }
                        System.out.println(nodeList);
                    } catch (XPathExpressionException e) {
                        logger.warning("Ex: " + e);
                    }
                });
                updatedString = convertXMLtoString(doc);
            } catch (ParserConfigurationException | IOException | SAXException e) {
                logger.warning("Ex: " + e);
            }

        } else {
            logger.info("New configurations for " + formattedFileName + " was not found in "
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

        } catch (Exception ex) {
            logger.warning("Exception occurred while converting doc to string: " + ex);
        }
        return xmlString;
    }

    private static Map<String, Map<String, String>> readDeploymentFile() {

        Map<String, Map<String, String>> tempPropertiesMap = new HashMap<>();

        Properties deploymentProperties = new Properties();
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
