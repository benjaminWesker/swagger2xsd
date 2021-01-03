package com.bdef.yamlconvertor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.xml.sax.SAXException;

import com.bdef.beans.Convertor;
import com.bdef.bundle.CodeReturn;
import com.bdef.yamlconvertor.yaml2xsd.Yaml2Xsd;

public class YamlConvertor {

	private Convertor convertor;
	
	final private String CONVERSION_ERROR = "ERROR";	
	final private String SELECT_FILE = "SELECT_FILE";
	final private String YAML_ERROR = "YAML";
	final private String SAVE_ERROR = "SAVE_ERROR";
	final private String MISSING_SAVE_PATH = "MISSING_SAVE_PATH";
	final private String SAVE_SUCCESS = "SAVE_SUCCESS";
	final private String VALIDATION_ERROR = "VAL_ERROR";
	final private String VALIDATION_SUCCESS = "VAL_SUCCESS";
	final private String CONVERSION_SUCCESS = "SUCCESS";
	final private String ERASE_FILE = "ERASE_FILE";
		
	public YamlConvertor(Convertor convertor) {
		this.convertor = convertor;
	}
	
	public boolean generateXsd()
	{

		String xsdConverted = "";
		
		String filePath = convertor.getYamlFilePath();
		
		if(filePath.isEmpty()) {
			generateMessage(getUTF8BundleMessage(SELECT_FILE));
			return false;
		}			

		try {
			xsdConverted = Yaml2Xsd.convert(filePath);
			
		} catch (IOException e) {
			generateMessage(getUTF8BundleMessage(YAML_ERROR));
			return false;
		} catch (IllegalArgumentException e) {
			generateMessage( getUTF8BundleMessage(CONVERSION_ERROR) + getUTF8BundleMessage(e.getMessage()) );
			return false;
		}

		this.convertor.setConvertedResult(xsdConverted);
		
		generateMessage( getUTF8BundleMessage(CONVERSION_SUCCESS));
		
		return true;

	}
	
	public boolean isSchemaValid(String schema) {
		
		try {
			Yaml2Xsd.validateSchema(schema);
			generateMessage( getUTF8BundleMessage(VALIDATION_SUCCESS));
		} catch (SAXException e) {
			generateMessage( getUTF8BundleMessage(VALIDATION_ERROR) + e.getMessage());
			return false;
		}
		
		return true;
	}

	public void setFilePath(String pathFile)
	{
		final String ext = this.convertor.getExtension();
		final String fileName = (pathFile.endsWith(ext) ? pathFile : pathFile + ext);
		setSavePath(fileName);
	}
	
	public boolean fileExist()
	{
		final boolean exist = new File(getFileName()).exists();
		System.out.println(getFileName());
		if( exist )
			generateMessage( getUTF8BundleMessage(ERASE_FILE)); 
		return exist;
	}

	public void saveFile(String content) {
		final String fileName = getFileName();
		
		if( fileName == null || fileName.isEmpty() ) {
			generateMessage( getUTF8BundleMessage(MISSING_SAVE_PATH)); 
			setNotSaved();
		}
  
		try(BufferedWriter bw = new BufferedWriter( new FileWriter(fileName)  ) ){
			bw.write(content);	
			generateMessage( getUTF8BundleMessage(SAVE_SUCCESS) + " " + fileName);
		} catch (IOException e) {
			generateMessage( getUTF8BundleMessage(SAVE_ERROR) + " " + fileName);
			setNotSaved();
		}
		  
		 setSaved();
}
	
	/* ====------------ getters methode ------------=== */	
	public String getResultText() {
		return this.convertor.getConvertedResult();
	}
	
	public String getGenerateMsg() {
		return this.convertor.getGenerateMsg();
	}
	
	public String getFileName()
	{
		return this.convertor.getSavePath();
	}

	public boolean isSaved() {
		return this.convertor.isSaved();
	}	
	
	/* ====------------ private methode ------------=== */	
	private void generateMessage(String message)
	{
		this.convertor.putGenerateMsg( message );
	}
	
	private void setSaved() {
		this.convertor.setSaved(true);
	}
	
	private void setNotSaved() {
		this.convertor.setSaved(false);
	}
	
	
	private void setSavePath(String fileName) {
		this.convertor.setSavePath(fileName);
	}
	
	/* convert properties messages to UTF8 */
	private String getUTF8BundleMessage(String key){
		
		return CodeReturn.getString(key); 
	
	}


	
}
