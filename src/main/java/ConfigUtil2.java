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
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
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

/**
 * Created by shan on 6/3/16.
 */
public class ConfigUtil2 {

    private static Logger logger = Logger.getLogger(ConfigUtil.class.getName());
    private static final String DEPLOYMENT_PROPERTIES_FILE_NAME = "deployment.properties";
    public static final String XML_ROOT = "configs";
    private static final Map<String, Map<String, String>> deploymentPropertiesMap = readDeploymentFile();

    private ConfigUtil2() {

    }

    public static String getConfig(File file, ConfigFileFormat configFileFormat) {

        String config = "";
        FileInputStream fileInputStream = null;

        try {
            fileInputStream = new FileInputStream(file);
            config = getConfig(fileInputStream, file.getName(), configFileFormat);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return config;
    }

    public static String getConfig(FileInputStream inputStream, String fileName, ConfigFileFormat configFileFormat) {

        String config = "";
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                out.append(line + "\n");
            }
            //            System.out.println(out.toString());   //Prints the string content read from input stream
            bufferedReader.close();

            config = parseFile(out.toString(), fileName, configFileFormat);
            config = applyNewConfigs(config, fileName);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return config;
    }

    public static <T> T getConfig(String key) {

        return (T) "";
    }

    private static String parseFile(String data, String fileName, ConfigFileFormat configFileFormat) {

        String convertedConfig = "";

        switch (configFileFormat) {
            case XML:
                convertedConfig = data;
                break;
            case YML:
                convertedConfig = parseYML(data, fileName);
                break;
            case PROPERTIES:
                convertedConfig = parseProperties(data);
                break;
            default:
                break;
        }
        return convertedConfig;
    }

    private static String parseYML(String data, String fileName) {
        String jsonString = convertToJson(data);
        String xmlString = convertToXML(jsonString);
        System.out.println("xmlString: " + xmlString);
        return xmlString;
    }

    private static String parseProperties(String data) {
        return data;
    }

    private static String applyNewConfigs(String xmlString, String fileName) {

        String formattedFileName = "[" + fileName + "]";
        String updatedString = xmlString;

        if (deploymentPropertiesMap.containsKey(formattedFileName)) {

            Map<String, String> newConfigs = deploymentPropertiesMap.get(formattedFileName);

            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = null;
            try {
                docBuilder = docBuilderFactory.newDocumentBuilder();
                Document doc = docBuilder.parse(new InputSource(new StringReader(xmlString)));

                XPath xPath = XPathFactory.newInstance().newXPath();

                for (String key : newConfigs.keySet()) {

                    logger.info("key: " + key);
                    NodeList nodeList = (NodeList) xPath.compile(key).evaluate(doc, XPathConstants.NODESET);

                    if (nodeList.item(0) != null) {
                        Node firstNode = nodeList.item(0);
                        firstNode.getFirstChild().setNodeValue(newConfigs.get(key));
                    }

                    System.out.println(nodeList);
                }
                updatedString = convertXMLtoString(doc);
                //                updatedString = prettyFormat(doc, 4);

            } catch (ParserConfigurationException e) {
                System.out.println(e);
            } catch (SAXException e) {
                System.out.println(e);
            } catch (IOException e) {
                System.out.println(e);
            } catch (XPathExpressionException e) {
                System.out.println(e);
            }
        } else {
            logger.info("New configurations for " + formattedFileName + " was not found in "
                    + DEPLOYMENT_PROPERTIES_FILE_NAME);
        }

        return updatedString;
    }

    private static String convertToJson(String yamlString) {
        Yaml yaml = new Yaml();
        Map<String, Object> map = (Map<String, Object>) yaml.load(yamlString);

        JSONObject jsonObject = new JSONObject(map);
        return jsonObject.toString();
    }

    private static String convertToXML(String jsonString) {
        String xmlString = null;
        try {
            JSONObject json = new JSONObject(jsonString);
            xmlString = XML.toString(json);
        } catch (JSONException e) {

        }
        //Need to add a root element
        return "<" + XML_ROOT + ">" + xmlString + "</" + XML_ROOT + ">";
    }

    public static String convertXMLtoString(Document doc) {
        String xmlString = "";
        try {
            // Method 1
            StringWriter stringWriter = new StringWriter();
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", 4);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            transformer.transform(new DOMSource(doc), new StreamResult(stringWriter));
            xmlString = stringWriter.toString();

            // Method 2
            //            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            //            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            //            //initialize StreamResult with File object to save to file
            //            StreamResult result = new StreamResult(new StringWriter());
            //            DOMSource source = new DOMSource(doc);
            //            transformer.transform(source, result);
            //            xmlString = result.getWriter().toString();

            System.out.println("+++++++++++++++++");
            System.out.println(xmlString);
            System.out.println("+++++++++++++++++");
        } catch (Exception ex) {
            //            throw new RuntimeException("Error converting to String", ex);
            System.out.println("xml string: " + xmlString);
        }
        return xmlString;
    }

    //    public static String prettyFormat(String input, int indent) {
    //        try {
    //            Source xmlInput = new StreamSource(new StringReader(input));
    //            StringWriter stringWriter = new StringWriter();
    //            StreamResult xmlOutput = new StreamResult(stringWriter);
    //            TransformerFactory transformerFactory = TransformerFactory.newInstance();
    //            transformerFactory.setAttribute("indent-number", indent);
    //            Transformer transformer = transformerFactory.newTransformer();
    //            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    //            transformer.transform(xmlInput, xmlOutput);
    //            return xmlOutput.getWriter().toString();
    //        } catch (Exception e) {
    //            throw new RuntimeException(e); // simple exception handling, please review it
    //        }
    //    }

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
                    int index = key.toString().indexOf("/");
                    //                    String[] temp = key.toString().split("/");
                    String fileName = key.toString().substring(0, index);
                    String xpath = key.toString().substring(index);

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
