package com.bdef.beans;

public class Convertor {

	private String yamlFilePath;

	private String convertedResult = new String();
	
	private String generateMsg = new String();
	
	private final String extension = ".xsd";

	public String getExtension() {
		return extension;
	}

	private boolean isSaved = false;
	private String savePath = new String();

	public boolean isSaved() {
		return isSaved;
	}

	public String getSavePath() {
		return savePath;
	}

	public void setSavePath(String savePath) {
		this.savePath = savePath;
	}

	public void setSaved(boolean isSaved) {
		this.isSaved = isSaved;
	}

	public Convertor(String yamlFilePath) {
		this.yamlFilePath = yamlFilePath;
	}
	
	public String getYamlFilePath() {
		return yamlFilePath;
	}

	public String getGenerateMsg() {
		return generateMsg;
	}

	public void putGenerateMsg(String message) {
		this.generateMsg =  message;
	}

	public void setYamlFilePath(String yamlFilePath) {
		this.yamlFilePath = yamlFilePath;
	}

	public String getConvertedResult() {
		return convertedResult;
	}

	public void setConvertedResult(String convertedResult) {
		this.convertedResult = convertedResult;
	}
	
	
}
