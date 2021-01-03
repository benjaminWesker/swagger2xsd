import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.bdef.view.MainFrame;

public class Main {

	public static void main(String[] args) {
		
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		} 
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
			System.out.println("unable to initialize interface model");
		}
		
		MainFrame myWindow = new MainFrame();
		myWindow.setVisible(true);
	}

}
