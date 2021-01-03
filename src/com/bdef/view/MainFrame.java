package com.bdef.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.bdef.beans.Convertor;
import com.bdef.bundle.InterfaceApplication;

public class MainFrame extends JFrame
{

	private static final long serialVersionUID = 1L;
	
	
	/*==============----------------------   TEXTE APPLI ----------------------==============*/
	
	private static final String APP_NAME = InterfaceApplication.getString(  "APP_NAME"  );
	
	private static final String TITLE_GETFILE = InterfaceApplication.getString( "TITLE_GETFILE" );

	private static final String CONVERT_BTN_TITLE = InterfaceApplication.getString("CONVERT_BTN_TITLE");
	
	private static final String BROWNSE_BTN_TITLE = InterfaceApplication.getString("BROWNSE_BTN_TITLE");
	
	private static final String EXIT_MESSAGE = InterfaceApplication.getString("EXIT_MESSAGE");
	private static final String EXIT_TITLE = InterfaceApplication.getString("EXIT_TITLE");

	/*==============----------------------   WINDOW PARAMETERS ----------------------==============*/	
	private static final Color BACKGROUND_RESULT_COLOR = Color.white;
	
   	private static final int WINDOW_WIDTH = 500;

   	private static final int WINDOW_HEIGHT = 175;
   	
	private static final Dimension windowSize = new Dimension(WINDOW_WIDTH,WINDOW_HEIGHT);
	
	/*==============----------------------   JOBJECTS ----------------------==============*/	
	private JButton brownseInputButton;
	private JTextField pathInputField;	
	
	public MainFrame() throws HeadlessException
	{
		
		this.setSize(windowSize);
		
		this.setTitle(APP_NAME);
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.setLocationRelativeTo(null);

		JPanel container = (JPanel) this.getContentPane();
	
		container.add(optionPan(),BorderLayout.NORTH);
		
		container.add(convertOrExitPan(),BorderLayout.SOUTH);	
		
		this.setContentPane(container);

	} 
	
	/*======--------- View ------======*/
	private JPanel borderPan() 
	{
		final JPanel container = new JPanel();
	
		container.setPreferredSize(new Dimension(15,0));
		
		return container;
	}
	
	private JPanel convertOrExitPan() 
	{	
		final JPanel container = new JPanel( new FlowLayout( FlowLayout.RIGHT));
		final JButton convertButton = new JButton(CONVERT_BTN_TITLE);
		final JButton exitButton = new JButton("Quitter");
		
		exitButton.addActionListener(this::exitButtonListener);
		convertButton.addActionListener(this::convertButtonListener);
		
		container.add(convertButton);
		container.add(exitButton);

		return container;
	}

	
	private JPanel optionPan() 
	{
		final JPanel container = new JPanel(new BorderLayout());
		
		container.add(borderPan(), BorderLayout.WEST);
		container.add(fillOptionPan());
		container.add(borderPan(),BorderLayout.EAST);
		
		return container ;
	}
	
	private JPanel fillOptionPan() 
	{
		final JPanel optionPan = new JPanel(new GridLayout(4,0));
		
		optionPan.add(new JLabel(TITLE_GETFILE));
		 
		optionPan.add(inputPathField());
		
		return optionPan;
	}
	
	private JPanel inputPathField() 
	{
		final JPanel brownseInputContainer = new JPanel(new BorderLayout() );
		final JLabel labelInputPath = new JLabel("Path :");

		pathInputField = new JTextField();
		brownseInputButton = new JButton(BROWNSE_BTN_TITLE);
		brownseInputButton.addActionListener(this::brownseInputButtonListener);
		 
		brownseInputContainer.add(labelInputPath,BorderLayout.WEST);
		brownseInputContainer.add(pathInputField);
		brownseInputContainer.add(brownseInputButton,BorderLayout.EAST);	
		
		return brownseInputContainer;
	}
	
	
	/*======--------- Listener ------======*/
	private void exitButtonListener(ActionEvent event)
	{
		int exit = JOptionPane.showConfirmDialog(MainFrame.this, EXIT_MESSAGE,EXIT_TITLE,JOptionPane.YES_NO_OPTION);
		if( exit == JOptionPane.YES_OPTION)
			dispose();
	}

	
	private void brownseInputButtonListener(ActionEvent event)
	{
		File current = null;
		
		try {
			current = new File(".").getCanonicalFile();
		} catch (IOException e) {}
		JFileChooser inputFile = new JFileChooser(current);
		inputFile.setFileSelectionMode(JFileChooser.FILES_ONLY);
		inputFile.setFileFilter(new FileNameExtensionFilter("swagger file", "yaml"));
		
		inputFile.showOpenDialog(MainFrame.this);
		
		if(inputFile.getSelectedFile() != null)
			pathInputField.setText(inputFile.getSelectedFile().toString());		
	}
		
	
	private void convertButtonListener(ActionEvent event)
	{
		new ResultFrame(new Convertor(  pathInputField.getText() ));	
	}
	

}