package com.bdef.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.bdef.beans.Convertor;
import com.bdef.bundle.InterfaceApplication;
import com.bdef.yamlconvertor.YamlConvertor;

public class ResultFrame extends JFrame
{

	private static final long serialVersionUID = 1L;

	/*==============----------------------   TEXTE APPLI ----------------------==============*/
	private static final String APP_NAME = InterfaceApplication.getString(  "APP_NAME"  );
	private static final String FRAME_TITLE = InterfaceApplication.getString( "FRAME_TITLE" );
	
	private static final String VALID_BTN_TITLE = InterfaceApplication.getString("VALID_BTN_TITLE");
	private static final String CLOSE_BTN_TITLE = InterfaceApplication.getString("CLOSE_BTN_TITLE");
	private static final String SAVE_BTN_TITLE = InterfaceApplication.getString("SAVE_BTN_TITLE");
	
	private static final String SAVE_BEFORE_CLOSE_MESSAGE = InterfaceApplication.getString("SAVE_BEFORE_CLOSE_MESSAGE");
	private static final String SAVE_BEFORE_CLOSE_TITLE = InterfaceApplication.getString("SAVE_BEFORE_CLOSE_TITLE");
	
	private static final String WIN_TITLE_CONVERSION_END = InterfaceApplication.getString("WIN_TITLE_CONVERSION_END");
	private static String WIN_TITLE_CONVERSION_ON_ERROR =  InterfaceApplication.getString("WIN_TITLE_CONVERSION_ON_ERROR");

	
	private static final String WIN_TITLE_VALIDATION_SCHEME = InterfaceApplication.getString("WIN_TITLE_VALIDATION_SCHEME");
	private static final String WIN_TITLE_SAVE_SCHEME = InterfaceApplication.getString("WIN_TITLE_SAVE_SCHEME");

	
	/*==============----------------------   WINDOW PARAMETERS ----------------------==============*/
   	private static final int WINDOW_WIDTH = 700;

   	private static final int WINDOW_HEIGHT = 850 ;
   	
   	
	private static final Dimension windowSize = new Dimension(WINDOW_WIDTH,WINDOW_HEIGHT);

	/*==============----------------------   JOBJECTS ----------------------==============*/		
	private JButton validXml, save, close; 
	private JTextArea result;
	
	/*==============----------------------   Beans ----------------------==============*/		
	private YamlConvertor yamlConvertor;

	public ResultFrame(Convertor convertor) throws HeadlessException
	{
		
		this.setSize(windowSize);
		
		this.setTitle(APP_NAME);
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.setLocationRelativeTo(null);
		
		initResultFrameCoponents();
		
		yamlConvertor = new YamlConvertor(convertor);
		
		displayResult();


	} 
	
	/*======--------- View ------======*/
	private void initResultFrameCoponents()
	{
		JPanel container = (JPanel) this.getContentPane();
		
		container.add(borderPan(),BorderLayout.WEST);
		container.add(borderPan(),BorderLayout.EAST);
		container.add(setFrameTitle(), BorderLayout.NORTH);
		
		result = new JTextArea();
		
		JScrollPane scrollPane = new JScrollPane(result);
		
		container.add(scrollPane);
		
		container.add(buttonBar(), BorderLayout.SOUTH);
	}
	
	
	
	private JPanel setFrameTitle()
	{
		JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JLabel title = new JLabel(FRAME_TITLE);
		titlePanel.add(title);
		
		return titlePanel;
	}
	
	private JPanel borderPan() 
	{
		final JPanel container = new JPanel();
	
		container.setPreferredSize(new Dimension(15,0));
		
		return container;
	}
	
	private JPanel buttonBar() 
	{
		JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		
		validXml = new JButton(VALID_BTN_TITLE);
		save = new JButton(SAVE_BTN_TITLE);
		close= new JButton(CLOSE_BTN_TITLE);
		
		/* set listener */
		close.addActionListener(this::closeWindow);
		save.addActionListener(this::saveAs);
		validXml.addActionListener(this::validScheme);
				
		buttonBar.add(validXml);
		buttonBar.add(save);
		buttonBar.add(close);
		
		return buttonBar;
	}
	
	/*======--------- Listener ------======*/
	public void closeWindow(ActionEvent event)
	{		
		if(!yamlConvertor.isSaved()) {
			if( askSaveBeforeExitMessage() == JOptionPane.YES_OPTION )
				displaySaveFile();
		}
		close();
	}
	
	public void saveAs(ActionEvent event)
	{
		displaySaveFile();
	}
	
	public void validScheme(ActionEvent event)
	{
		showValidSchemaMessage();
	}

	/*======--------- ask method ------======*/
	private int askEraseChoice() {
		return JOptionPane.showConfirmDialog(ResultFrame.this, this.yamlConvertor.getGenerateMsg(),WIN_TITLE_SAVE_SCHEME,JOptionPane.YES_NO_OPTION);
	}
	
	private int askSaveBeforeExitMessage() {
		return JOptionPane.showConfirmDialog(ResultFrame.this, SAVE_BEFORE_CLOSE_MESSAGE,SAVE_BEFORE_CLOSE_TITLE,JOptionPane.YES_NO_OPTION);
	}
	
	/*======--------- display ------======*/
	private void displayResult()
	{
		final boolean conversionOnError = !yamlConvertor.generateXsd();

		if(!conversionOnError) {
			result.setText(yamlConvertor.getResultText());
			this.setVisible(true);
			 showMessage(WIN_TITLE_CONVERSION_END);
		} else {
			showErrorMessage(WIN_TITLE_CONVERSION_ON_ERROR);
			close();
		}

	}
	
	private void displaySaveFile()
	{
		JFileChooser savePath = new JFileChooser();
		savePath.setFileFilter(new FileNameExtensionFilter("*.xsd", "xsd"));
		
		if(savePath.showSaveDialog(ResultFrame.this) == JFileChooser.APPROVE_OPTION ) {
			yamlConvertor.setFilePath(savePath.getSelectedFile().toString());
			
			if(fileExist()) {
				if (askEraseChoice() == JOptionPane.YES_OPTION)
					save();
				else
					displaySaveFile();
			}
			else
				save();
		}
	
	}
	
	private void showValidSchemaMessage() 
	{
		if(validScheme())
			showMessage(WIN_TITLE_VALIDATION_SCHEME);
		else
			showErrorMessage(WIN_TITLE_VALIDATION_SCHEME);
	}

	private void showMessage(String title)
	{
	
		JOptionPane.showMessageDialog(
				ResultFrame.this,
				this.yamlConvertor.getGenerateMsg(), 
				title, 
				JOptionPane.INFORMATION_MESSAGE
				);	

	}
	
	private void showErrorMessage(String title)
	{
	
		JOptionPane.showMessageDialog(
				ResultFrame.this,
				this.yamlConvertor.getGenerateMsg(), 
				title, 
				JOptionPane.ERROR_MESSAGE
				);	

	}
	
	/*======--------- controler ------======*/
	private boolean fileExist() {
		return yamlConvertor.fileExist();
	}
	
	private boolean isSaved() {
		return yamlConvertor.isSaved();
	}
	
	private void save() 
	{
		yamlConvertor.saveFile(result.getText() );
		
		if(isSaved())
			showMessage(WIN_TITLE_SAVE_SCHEME);	
		else
			showErrorMessage(WIN_TITLE_SAVE_SCHEME);
	}
	
	private boolean validScheme()
	{
		return yamlConvertor.isSchemaValid(result.getText());
	}
	
	private void close() 
	{
		dispose();
	}
}