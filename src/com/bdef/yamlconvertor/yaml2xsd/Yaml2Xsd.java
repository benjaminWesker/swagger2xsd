package com.bdef.yamlconvertor.yaml2xsd;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class Yaml2Xsd {
	
    private Yaml2Xsd()
    {
    }


    public static String convert(String pathToYaml) throws JsonProcessingException, IOException, IllegalArgumentException
    {
    	final String json = createJson(pathToYaml); 
    	
    	ObjectMapper mapper = new ObjectMapper();
    	
    	JsonNode jsonSchema = mapper.readTree(json);
 			
 		return Jsons2Xsd.convert(jsonSchema);
 		
 		
    }
    
    public static void validateSchema(String schema) throws SAXException
    {

    	 Jsons2Xsd.validateSchema(schema);
    
    }
    
    
	/*====================================--------------------------------------------------------------------*/
	/*
	 *  Cree un string JSON a partie d'un fichier YAML
	 * 
	 */
	public static String createJson(String pathToYaml) throws JsonProcessingException, IOException 
	{
	   final String yaml = new String(Files.readAllBytes(Paths.get(pathToYaml)));

	   return convertYamlToJson(yaml);
	}
	/*====================================--------------------------------------------------------------------*/
	/*
	 *  Converti un string yaml en json
	 * 
	 */
	private static String convertYamlToJson(String yaml) throws JsonProcessingException, IOException 
	{
		final ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
		final Object obj = yamlReader.readValue(yaml, Object.class);
		final ObjectMapper jsonWriter = new ObjectMapper();
		
		return jsonWriter.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    }

	
}
