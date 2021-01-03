package com.bdef.yamlconvertor.yaml2xsd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/*-
 * #%L
 * jsons2xsd
 * %%
 * Copyright (C) 2014 - 2020 Morten Haraldsen (ethlo)
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class Jsons2Xsd
{
	private static final String TYPE_EMPTY = "empty";
    private static final String TYPE_REFERENCE = "reference";
    private static final String TYPE_ENUM = "enum";

    private static final String FIELD_NAME = "name";
    private static final String FIELD_PROPERTIES = "properties";
    private static final String FIELD_REQUIRED = "required";

    private static final String XSD_ATTRIBUTE = "attribute";
    private static final String XSD_ELEMENT = "element";
    private static final String XSD_SEQUENCE = "sequence";
    private static final String XSD_COMPLEXTYPE = "complexType";
    private static final String XSD_SIMPLETYPE = "simpleType";
    private static final String XSD_RESTRICTION = "restriction";
    private static final String XSD_VALUE = "value";
    private static final String XSD_CHOICE = "choice";

    private static final String XSD_OBJECT = "object";
    private static final String XSD_ARRAY = "array";

    private static final String JSON_REF = "$ref";
    private static final String JSON_SCHEMA = "schema";
    
    
    // à remplacer par une fonction de lecture du Json
    private static String OPERATION_ID = "operationId_";
    private static final String HTTPS = "[\"https\"]";
    
    private static final String XSD_INPUT_NAME = "InputMessage";  
    private static final String XSD_OUTPUT_NAME = "OutputMessage";  

    private static final Map<String, String> typeMapping = new HashMap<>();
 
    static
    {
        // Primitive types
        typeMapping.put(JsonSimpleType.STRING_VALUE, XsdSimpleType.STRING_VALUE);
        typeMapping.put(JsonComplexType.OBJECT_VALUE, XsdComplexType.OBJECT_VALUE);
        typeMapping.put(JsonComplexType.ARRAY_VALUE, XsdComplexType.ARRAY_VALUE);
        typeMapping.put(JsonSimpleType.NUMBER_VALUE, XsdSimpleType.DECIMAL_VALUE);
        typeMapping.put(JsonSimpleType.BOOLEAN_VALUE, XsdSimpleType.BOOLEAN_VALUE);
        typeMapping.put(JsonSimpleType.INTEGER_VALUE, XsdSimpleType.INT_VALUE);

        // String formats
        typeMapping.put("string|uri", "anyURI");
        typeMapping.put("string|email", XsdSimpleType.STRING_VALUE);
        typeMapping.put("string|phone", XsdSimpleType.STRING_VALUE);
        typeMapping.put("string|date-time", XsdSimpleType.DATETIME_VALUE);
        typeMapping.put("string|date", XsdSimpleType.DATE_VALUE);
        typeMapping.put("string|time", XsdSimpleType.TIME_VALUE);
        typeMapping.put("string|utc-millisec", XsdSimpleType.LONG_VALUE);
        typeMapping.put("string|regex", XsdSimpleType.STRING_VALUE);
        typeMapping.put("string|color", XsdSimpleType.STRING_VALUE);
        typeMapping.put("string|style", XsdSimpleType.STRING_VALUE);
        
        // Number formats
        typeMapping.put(JsonSimpleType.NUMBER_VALUE+"|double", XsdSimpleType.DOUBLE_VALUE);
        typeMapping.put(JsonSimpleType.INTEGER_VALUE+"|int64", XsdSimpleType.INT_VALUE);
        typeMapping.put(JsonSimpleType.INTEGER_VALUE+"|int32", XsdSimpleType.INT_VALUE);
        
    }

    private Jsons2Xsd()
    {
    }

    private static final ObjectMapper mapper = new ObjectMapper();
    
    
    public static void validateSchema(String schema) throws SAXException
    {

    	XmlUtil.validateSchema(schema);
    
    }
    
    /*
     * Description
     * ==---- 
     *convert Json to xsd as xml with default config
     * ===--*/
    public static String convert(JsonNode jsonSchema) throws IOException, IllegalArgumentException
    {
    	final Config cfg = config(jsonSchema);
    	
		return XmlUtil.asXmlString( convert(jsonSchema, cfg) );
    }
    
    
    private static Document convert(JsonNode jsonSchema, Config cfg ) throws IOException , IllegalArgumentException
    {
    	
        final Element schemaRoot = createDocument(cfg);	
        final Set<String> neededElements = new LinkedHashSet<>();

		final JsonNode hasPost = jsonSchema.findValue("post");
		Assert.notNull(hasPost, "POST");
		
		jsonSchema.findValues("post").forEach( post ->		
		{
			setOperationID(post);
			
			createRootElement(neededElements, post, schemaRoot, cfg);
	
		    try {
				convertParameters(neededElements, post, schemaRoot, cfg);
			} catch (IllegalArgumentException | IOException e) {
				throw new IllegalArgumentException(e.getMessage());
			}
	
		    convertResponses(neededElements, post, schemaRoot, cfg);
		} 
		);
		
	    convertDefinitions(neededElements, jsonSchema, schemaRoot, cfg);
        
         return schemaRoot.getOwnerDocument();
    }
    
    /* Description
     * ==---- default config ===--*/
    private static Config config(JsonNode jsonSchema)
    {
    	String targetNamespace = setTargetNamespace(jsonSchema);
    	
		 final Config cfg = new Config.Builder()
				    .targetNamespace(targetNamespace)
				    .name("convert")
				    .nsAlias("tns")
				    .build();
    	
    	return cfg;
    }
    
    private static String setTargetNamespace(JsonNode jsonSchema) 
    {
    	String targetNamespace = "no_target_defined";;
    	if(jsonSchema.get("host") != null && jsonSchema.get("basePath") != null & jsonSchema.get("schemes") != null )
    		targetNamespace = (jsonSchema.get("schemes").toString().equals(HTTPS) ? "https://" : "http://") + jsonSchema.get("host").asText() + jsonSchema.get("basePath").asText();
    	return targetNamespace;
    }
    
	private static void setOperationID(JsonNode jsonSchema)
	{	
		if(jsonSchema.findValue("operationId") != null)
			OPERATION_ID = jsonSchema.findValue("operationId").asText() + "_";
	
	}
    
    private static void createRootElement(Set<String> neededElements, JsonNode jsonSchema,  Element schemaRoot, Config cfg)
    {
	    final Element inputElement = element(schemaRoot,XSD_ELEMENT);
	    inputElement.setAttribute("name", OPERATION_ID + XSD_INPUT_NAME);
	    inputElement.setAttribute("type", cfg.getNsAlias() + ":" + OPERATION_ID + XSD_INPUT_NAME);
	    
	    final JsonNode responses = jsonSchema.findValue("responses");
	    
	    if(responses != null)
	    	doIterateElementsResponses(neededElements, schemaRoot, responses, cfg);
    }
    
    private static void convertParameters(Set<String> neededElements, JsonNode jsonSchema, Element schemaRoot, Config cfg) throws IOException , IllegalArgumentException
    {
		final JsonNode parametersNode = jsonSchema.findValue("parameters");
		Assert.notNull(parametersNode, "PARAMETERS");	
		
	    final Element inputComplexType = element(schemaRoot,XSD_COMPLEXTYPE);
	    inputComplexType.setAttribute("name", OPERATION_ID + XSD_INPUT_NAME);
	    
	    final Element inputSequence = element(inputComplexType,XSD_SEQUENCE);  
	    
	    final String parameters = parametersNode.toString().replace("[", "").replace("]", "");
	    
	    for(String s : parameters.split("\\},\\{")) {
	    	
	    	int start = parameters.indexOf(s) - 1;
	    	int end = start + s.length()+ 2;
	    	String res =  parameters.substring(  (start < 0 ? 0 : start) , ( end < parameters.length() ? end : parameters.length()));

	    	doIterateAllParameters(neededElements, inputSequence, res, cfg);
	    	
	    }

    }
    
    private static void doIterateAllParameters(Set<String> neededElements, Element schemaRoot, String jsonSchema, Config cfg) throws IOException
    {
       final JsonNode parameter = mapper.readTree(jsonSchema);
      
       doIterateParameters(neededElements, schemaRoot, parameter, cfg);
 
    }   
    
    private static void doIterateParameters(Set<String> neededElements, Element elem, JsonNode node, Config cfg) 
    {
	  	final Element newElement = element(elem,XSD_ELEMENT);
        final String name = node.findValue("name").asText().replace(" ", "");
        final String xsdType = (node.findValue("type") != null ? determineXsdType(cfg, name, node) : TYPE_REFERENCE);
        final boolean required = (  node.findValue("required") != null && node.findValue("required").asText().equals("true") ? true : false  ); 
        
        newElement.setAttribute("name", name);
        
        if (!XSD_OBJECT.equals(xsdType) && !XSD_ARRAY.equals(xsdType))
        {
            // Simple type
        	newElement.setAttribute("type", xsdType);
        }
               
        if (!required)
        {
            // Not required
        	newElement.setAttribute("minOccurs", "0");
        }
        
        handleContent(neededElements, node, cfg, xsdType , newElement);
 
    }
    
    private static void convertResponses(Set<String> neededElements, JsonNode jsonSchema, Element schemaRoot, Config cfg)
    {
    	final JsonNode responses = jsonSchema.findValue("responses");
    	if(responses != null)
    		doIterateResponses(neededElements, schemaRoot, responses, cfg);
    }
    
    private static void convertDefinitions(Set<String> neededElements, JsonNode jsonSchema, Element schemaRoot, Config cfg) throws IllegalArgumentException
    {
    	final JsonNode definitions = jsonSchema.get("definitions");
    	Assert.notNull(definitions, "DEFINITIONS");
    	
    	doIterateDefinitions(neededElements, schemaRoot, definitions, cfg);

    }

    private static Element createDocument(Config cfg)
    {
        final Document xsdDoc = XmlUtil.newDocument();
        xsdDoc.setXmlStandalone(true);

        final Element schemaRoot = element(xsdDoc, "schema");
        schemaRoot.setAttribute("targetNamespace", cfg.getTargetNamespace());
        schemaRoot.setAttribute("xmlns:" + cfg.getNsAlias(), cfg.getTargetNamespace());
      
        schemaRoot.setAttribute("elementFormDefault", "qualified");
        if (cfg.isAttributesQualified())
        {
            schemaRoot.setAttribute("attributeFormDefault", "qualified");
        }
      
        return schemaRoot;
    }
    

 
 
    private static void doIterateElementsResponses(Set<String> neededElements, Element elem, JsonNode node, Config cfg) 
    {
    	
        final Iterator<Entry<String, JsonNode>> iter = node.fields();
        while (iter.hasNext())
        {
            final Entry<String, JsonNode> entry = iter.next();
            final String key = entry.getKey();
            final JsonNode val = entry.getValue();
            
            if (!neededElements.contains(key) && cfg.isIncludeOnlyUsedTypes())
            {
                continue;
            }
            
            final Element responses = element(elem, XSD_ELEMENT);
            
            final JsonNode schema = val.get(JSON_SCHEMA);
                 
            final String xsdType = (schema != null ? determineXsdType(cfg, key, schema) : TYPE_EMPTY);
            
            final String name = OPERATION_ID + ( Integer.valueOf(key) < 400 ? XSD_OUTPUT_NAME : key);
            responses.setAttribute("name",name);
            
            
            if(xsdType.contentEquals(TYPE_REFERENCE)) handleReference(neededElements, responses, schema, cfg);
            else if(xsdType.contentEquals(TYPE_EMPTY)) responses.setAttribute("type", cfg.getNsAlias() + ":" + name);
            else handleContent(neededElements, schema, cfg, xsdType, responses);
  
        }
    }
    
    private static void doIterateResponses(Set<String> neededElements, Element elem, JsonNode node, Config cfg)
    {
        final Iterator<Entry<String, JsonNode>> iter = node.fields();
        while (iter.hasNext())
        {
            final Entry<String, JsonNode> entry = iter.next();
            final String key = entry.getKey();
            final JsonNode val = entry.getValue();
          
            if (!neededElements.contains(key) && cfg.isIncludeOnlyUsedTypes())
            {
                continue;
            }
        
            final JsonNode schema = val.get(JSON_SCHEMA);
        
            if (schema == null ) {
            	final Element responses = element(elem, XSD_COMPLEXTYPE);  
            	final String name = OPERATION_ID + ( Integer.valueOf(key) < 400 ? XSD_OUTPUT_NAME : key);
            	responses.setAttribute("name", name);
            	final Element seq  = element(responses, XSD_SEQUENCE);  
            	final Element r  = element(seq, XSD_ELEMENT);
            	r.setAttribute("name", "response_" + key);
       	
            }
            
        }
    }

    private static void doIterateDefinitions(Set<String> neededElements, Element elem, JsonNode node, Config cfg)
    {
        final Iterator<Entry<String, JsonNode>> iter = node.fields();
        while (iter.hasNext())
        {
            final Entry<String, JsonNode> entry = iter.next();
            final String key = entry.getKey();
            final JsonNode val = entry.getValue();
            
            if (!neededElements.contains(key) && cfg.isIncludeOnlyUsedTypes())
            {
                continue;
            }

            if (key.equals("Link"))
            {
                final Element schemaComplexType = element(elem, XSD_COMPLEXTYPE);
                schemaComplexType.setAttribute(FIELD_NAME, key);
                final Element href = element(schemaComplexType, XSD_ATTRIBUTE);
                final Element rel = element(schemaComplexType, XSD_ATTRIBUTE);
                final Element title = element(schemaComplexType, XSD_ATTRIBUTE);
                final Element method = element(schemaComplexType, XSD_ATTRIBUTE);
                final Element type = element(schemaComplexType, XSD_ATTRIBUTE);

                href.setAttribute(FIELD_NAME, "href");
                href.setAttribute("type", XsdSimpleType.STRING_VALUE);

                rel.setAttribute(FIELD_NAME, "rel");
                rel.setAttribute("type", XsdSimpleType.STRING_VALUE);

                title.setAttribute(FIELD_NAME, "title");
                title.setAttribute("type", XsdSimpleType.STRING_VALUE);

                method.setAttribute(FIELD_NAME, "method");
                method.setAttribute("type", XsdSimpleType.STRING_VALUE);

                type.setAttribute(FIELD_NAME, "type");
                type.setAttribute("type", XsdSimpleType.STRING_VALUE);
            }
            else
            {
                final String xsdType = determineXsdType(cfg, key, val);
                handleContent(neededElements, val, cfg, xsdType, elem);
                ((Element)elem.getLastChild()).setAttribute(FIELD_NAME, key);
            }
        }
    }

    private static void handleObject(Set<String> neededElements, Element elem, JsonNode node, Config cfg)
    {
        final JsonNode properties = node.get(FIELD_PROPERTIES);
        if (properties != null)
        {
            final Element complexType = element(elem, XSD_COMPLEXTYPE);
            addDocumentation(complexType, node);
            final Element schemaSequence = element(complexType, XSD_SEQUENCE);

            doIterate(neededElements, schemaSequence, properties, getRequiredList(node), cfg);
        }
        else if (node.get("oneOf") != null)
        {
            final ArrayNode oneOf = (ArrayNode) node.get("oneOf");
            handleChoice(neededElements, elem, oneOf, cfg);
        }
    }

    private static void handleChoice(Set<String> neededElements, Element elem, ArrayNode oneOf, Config cfg)
    {
        final Element complexTypeElem = element(elem, XSD_COMPLEXTYPE);
        final Element choiceElem = element(complexTypeElem, XSD_CHOICE);
        for (JsonNode e : oneOf)
        {
            final Element nodeElem = element(choiceElem, XSD_ELEMENT);
            final JsonNode refs = e.get(JSON_REF);
            String fixRef = refs.asText().replace("#/definitions/", cfg.getNsAlias() + ":");
            String name = fixRef.substring(cfg.getNsAlias().length() + 1);
            name = name.replace(" ", "_");
            nodeElem.setAttribute(FIELD_NAME, name);
            nodeElem.setAttribute("type", fixRef);

            neededElements.add(name);
        }
    }

    private static void doIterate(Set<String> neededElements, Element elem, JsonNode node, List<String> requiredList, Config cfg)
    {
        if (node.isObject())
        {
            final Iterator<Entry<String, JsonNode>> fieldIter = node.fields();
            while (fieldIter.hasNext())
            {
                final Entry<String, JsonNode> entry = fieldIter.next();
                final String key = entry.getKey();
                final JsonNode val = entry.getValue();
                doIterateSingle(neededElements, key, val, elem, requiredList.contains(key), cfg);
            }
        }
        else if (node.isArray())
        {
            int i = 0;
            for (JsonNode entry : node)
            {
                final String key = String.format("item%s", i++);
                doIterateSingle(neededElements, key, entry, elem, requiredList.contains(key), cfg);
            }
        }
    }

    private static void doIterateSingle(Set<String> neededElements, String name, JsonNode val, Element elem, boolean required, Config cfg)
    {
        final String xsdType = determineXsdType(cfg, name, val);
        final Element nodeElem = element(elem, XSD_ELEMENT);
        addDocumentation(nodeElem, val);
        name = name.replace(" ", "_");
        nodeElem.setAttribute(FIELD_NAME, name);

        if (!XSD_OBJECT.equals(xsdType) && !XSD_ARRAY.equals(xsdType))
        {
            // Simple type
            nodeElem.setAttribute("type", xsdType);
        }

        if (!required)
        {
            // Not required
            nodeElem.setAttribute("minOccurs", "0");
        }

        handleContent(neededElements, val, cfg, xsdType, nodeElem);
    }

    private static void handleContent(Set<String> neededElements, JsonNode val, Config cfg, String xsdType, Element nodeElem) {
        switch (xsdType)
        {
            case XSD_ARRAY:
                handleArray(neededElements, nodeElem, val, cfg);
                break;

            case XsdSimpleType.DECIMAL_VALUE:
            case XsdSimpleType.DOUBLE_VALUE:
            case XsdSimpleType.INT_VALUE:
                handleNumber(nodeElem, xsdType, val);
                break;

            case "enum":
                handleEnum(nodeElem, val);
                break;

            case XSD_OBJECT:
                handleObject(neededElements, nodeElem, val, cfg);
                break;

            case XsdSimpleType.STRING_VALUE:
                handleString(nodeElem, val);
                break;

            case TYPE_REFERENCE:
                handleReference(neededElements, nodeElem, val, cfg);
                break;
            default:
                if (nodeElem.getNodeName().equals("schema")) {
                    final Element simpleType = element(nodeElem, XSD_SIMPLETYPE);
                    final Element restriction = element(simpleType, XSD_RESTRICTION);
                    restriction.setAttribute("base", xsdType);
                }
        }
    }

    private static void handleReference(Set<String> neededElements, Element nodeElem, JsonNode val, Config cfg)
    {
        final JsonNode refs = val.findValue(JSON_REF);
        nodeElem.removeAttribute("type");
        String fixRef = refs.asText().replace("#/definitions/", cfg.getNsAlias() + ":");
        String name = fixRef.substring(cfg.getNsAlias().length() + 1);
        name = name.replace(" ", "_");
        String oldName = nodeElem.getAttribute(FIELD_NAME);

        if (oldName.trim().length() == 0)
        {
            nodeElem.setAttribute(FIELD_NAME, cfg.getItemNameMapper().apply(name));
        }
        nodeElem.setAttribute("type", fixRef);

        neededElements.add(name);
    }

    private static void handleString(Element nodeElem, JsonNode val)
    {
        final Integer minimumLength = getIntVal(val, "minLength");
        final Integer maximumLength = getIntVal(val, "maxLength");
        final String expression = val.path("pattern").textValue();

        if (minimumLength != null || maximumLength != null || expression != null || nodeElem.getNodeName().equals("schema"))
        {
            nodeElem.removeAttribute("type");
            final Element simpleType = element(nodeElem, XSD_SIMPLETYPE);
            addDocumentation(simpleType, val);
            final Element restriction = element(simpleType, XSD_RESTRICTION);
            restriction.setAttribute("base", XsdSimpleType.STRING_VALUE);

            if (minimumLength != null)
            {
                final Element min = element(restriction, "minLength");
                min.setAttribute(XSD_VALUE, Integer.toString(minimumLength));
            }

            if (maximumLength != null)
            {
                final Element max = element(restriction, "maxLength");
                max.setAttribute(XSD_VALUE, Integer.toString(maximumLength));
            }

            if (expression != null)
            {
                final Element max = element(restriction, "pattern");
                max.setAttribute(XSD_VALUE, expression);
            }
        }
    }

    private static void handleEnum(Element nodeElem, JsonNode val)
    {
        nodeElem.removeAttribute("type");
        final Element simpleType = element(nodeElem, XSD_SIMPLETYPE);
        addDocumentation(simpleType, val);
        final Element restriction = element(simpleType, XSD_RESTRICTION);
        restriction.setAttribute("base", XsdSimpleType.STRING_VALUE);
        final JsonNode enumNode = val.get("enum");
        for (int i = 0; i < enumNode.size(); i++)
        {
            final String enumVal = enumNode.path(i).asText();
            final Element enumElem = element(restriction, "xsd:enumeration");
            enumElem.setAttribute(XSD_VALUE, enumVal);
        }
    }

    private static void handleNumber(Element nodeElem, String xsdType, JsonNode jsonNode)
    {
        final Long minimum = getLongVal(jsonNode, "minimum");
        final Long maximum = getLongVal(jsonNode, "maximum");

        if (minimum != null || maximum != null || nodeElem.getNodeName().equals("schema"))
        {
            nodeElem.removeAttribute("type");
            final Element simpleType = element(nodeElem, XSD_SIMPLETYPE);
            addDocumentation(simpleType, jsonNode);
            final Element restriction = element(simpleType, XSD_RESTRICTION);

            boolean shouldBeLong = false;
            if (minimum != null)
            {
                if (minimum < Integer.MIN_VALUE) {
                    shouldBeLong = true;
                }
                final Element min = element(restriction, "minInclusive");
                min.setAttribute(XSD_VALUE, Long.toString(minimum));
            }

            if (maximum != null)
            {
                if (maximum > Integer.MAX_VALUE) {
                    shouldBeLong = true;
                }
                final Element max = element(restriction, "maxInclusive");
                max.setAttribute(XSD_VALUE, Long.toString(maximum));
            }

            String xsdTypeToUse = shouldBeLong ? XsdSimpleType.LONG_VALUE : xsdType;
            restriction.setAttribute("base", xsdTypeToUse);
        }
    }

    private static void handleArray(Set<String> neededElements, Element nodeElem, JsonNode jsonNode, Config cfg)
    {
        final JsonNode arrItems = jsonNode.path("items");
        final String arrayXsdType = determineXsdType(cfg, arrItems.path("type").textValue(), arrItems);
        if (cfg.isUnwrapArrays()) {
            handleArrayElements(neededElements, jsonNode, arrItems, arrayXsdType, nodeElem, cfg);
        } else {
            final Element complexType = element(nodeElem, XSD_COMPLEXTYPE);
            final Element sequence = element(complexType, XSD_SEQUENCE);
            final Element arrElem = element(sequence, XSD_ELEMENT);

            handleArrayElements(neededElements, jsonNode, arrItems, arrayXsdType, arrElem, cfg);

            final String o = arrElem.getAttribute("name");
            if (o == null || o.trim().length() == 0)
            {
                arrElem.setAttribute(FIELD_NAME, "item");
            }
        }
    }

    private static void handleArrayElements(Set<String> neededElements, JsonNode jsonNode, final JsonNode arrItems, final String arrayXsdType, final Element arrElem, Config cfg)
    {
        if (arrayXsdType.equals(TYPE_REFERENCE))
        {
            handleReference(neededElements, arrElem, arrItems, cfg);
        }
        else if (arrayXsdType.equals(JsonComplexType.OBJECT_VALUE))
        {
            handleObject(neededElements, arrElem, arrItems, cfg);
        }
        else
        {
            String oldName = arrElem.getAttribute(FIELD_NAME);
            if (oldName.trim().length() == 0)
            {
                arrElem.setAttribute(FIELD_NAME, "item");
            }
            arrElem.setAttribute("type", arrayXsdType);
        }

        // Minimum items
        final Integer minItems = getIntVal(jsonNode, "minItems");
        arrElem.setAttribute("minOccurs", minItems != null ? Integer.toString(minItems) : "0");

        // Max Items
        final Integer maxItems = getIntVal(jsonNode, "maxItems");
        arrElem.setAttribute("maxOccurs", maxItems != null ? Integer.toString(maxItems) : "unbounded");
    }

    private static String determineXsdType(final Config cfg, String key, JsonNode node) throws IllegalArgumentException
    {
        final String jsonType = node.path("type").textValue();
        final String jsonFormat = node.path("format").textValue();
        final boolean isEnum = node.get(TYPE_ENUM) != null;
        final boolean isRef = node.get(JSON_REF) != null;
        final boolean hasProperties = node.get(FIELD_PROPERTIES) != null;
       
        if (isRef)
        {
            return TYPE_REFERENCE;
        }
        else if (isEnum)
        {
            return TYPE_ENUM;
        }
        
        Assert.notNull(jsonType, "TYPE");
        
        if (hasProperties || jsonType.equalsIgnoreCase(JsonComplexType.OBJECT_VALUE))
        {
            return XsdComplexType.OBJECT_VALUE;
        }
        else if (jsonType.equalsIgnoreCase(JsonComplexType.ARRAY_VALUE))
        {
            return XsdComplexType.ARRAY_VALUE;
        }

        // Check built-in
        String xsdType = getType(jsonType, jsonFormat);
        if (xsdType != null)
        {
            return xsdType;
        }

        // Check cusom mapping in config
        xsdType = cfg.getType(jsonType, jsonFormat);
        if (xsdType != null)
        {
            return xsdType;
        }

        // Check for non-json mappings
        final Optional<Entry<String, String>> mapping = cfg.getTypeMapping()
                .entrySet()
                .stream()
                .filter(e->e.getKey().startsWith(jsonType + "|"))
                .findFirst();
        if (mapping.isPresent() && (isFormatMatch(mapping.get().getKey(), jsonType, jsonFormat) || cfg.isIgnoreUnknownFormats()))
        {
            return mapping.get().getValue();
        }

        throw new IllegalArgumentException("Unable to determine XSD type for json type=" + jsonType + ", format=" + jsonFormat);
   
    }

    private static void addDocumentation(Element element, JsonNode node) {
        final JsonNode description = node.get("description");
        final boolean parentIsElement = element.getParentNode().getNodeName().equals(XSD_ELEMENT);
        if(description != null && !parentIsElement) {
            final Element annotation = element(element, "annotation");
            final Element documentation = element(annotation, "documentation");
            documentation.setTextContent(description.textValue());
        }
    }

    private static boolean isFormatMatch(final String key, final String jsonType, final String jsonFormat)
    {
        return key.equalsIgnoreCase(jsonType + "|" + jsonFormat);
    }

    private static Integer getIntVal(JsonNode node, String attribute)
    {
        return node.get(attribute) != null ? node.get(attribute).intValue() : null;
    }

    private static Long getLongVal(JsonNode node, String attribute)
    {
        return node.get(attribute) != null ? node.get(attribute).longValue() : null;
    }

    private static Element element(Node element, String name)
    {
        return XmlUtil.createXsdElement(element, name);
    }

    private static String getType(String type, String format)
    {
        final String key = (type + (format != null ? ("|" + format) : "")).toLowerCase();
        return typeMapping.get(key);
    }

    private static List<String> getRequiredList(JsonNode jsonNode) throws IllegalArgumentException
    {
        if (jsonNode.path(FIELD_REQUIRED).isMissingNode())
        {
            return Collections.emptyList();
        }
        Assert.isTrue(jsonNode.path(FIELD_REQUIRED).isArray(), "REQUIRED_PROPERTY");
        List<String> requiredList = new ArrayList<>();
        for (JsonNode requiredField : jsonNode.withArray(FIELD_REQUIRED))
        {
            Assert.isTrue(requiredField.isTextual(), "REQUIRED_STRING");
            requiredList.add(requiredField.asText());
        }
        return requiredList;
    }
}
