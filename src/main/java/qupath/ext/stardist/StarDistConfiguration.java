/*
* #%L
* ST-AnD is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as
* published by the Free Software Foundation, either version 3 of the
* License, or (at your option) any later version.
* 
* ST-AnD is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License 
* along with ST-AnD.  If not, see <https://www.gnu.org/licenses/>.
* #L%
*/

package qupath.ext.stardist;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.util.Callback;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

import qupath.lib.gui.QuPathGUI;
//import qupath.lib.gui.commands.interfaces.PathCommand;
import qupath.lib.gui.prefs.PathPrefs;


/**
* Command used to create and show a suitable dialog box for StarDist Configuration.
* 
* @author C. H. Huang
*
*/

public class StarDistConfiguration implements Runnable {
	final private static String name = "StarDist Configuration";
	final private static StringProperty stardistLocationProperty = PathPrefs.createPersistentPreference("stardistModelLocation", null);
	
	private QuPathGUI qupath;
	
	public StarDistConfiguration(final QuPathGUI qupath) {
		this.qupath = qupath;
	}
   
	@Override
	public void run() {
		final Dialog<Map<String, String>> dialog = new Dialog<>();
		dialog.setTitle("Configuration");
		dialog.setHeaderText("StarDist Nuclei Detection");
		dialog.setResizable(true);
		 
		final Label stardistLocationLabel = new Label("StarDist Model Location: ");
		final TextField stardistLocationText = new TextField();				
		
		final String stardistLocationString = stardistLocationProperty.get();
		stardistLocationText.setText(stardistLocationString);

		final Button stardistExecChsrBtn = new Button("...");
		stardistExecChsrBtn.setOnAction(new EventHandler<ActionEvent>() {
           public void handle(ActionEvent e) {
           	final DirectoryChooser dirChooser = new DirectoryChooser();
               File selectedFile = (File) dirChooser.showDialog(null);
               stardistLocationText.setText(selectedFile.toString());
           }
		});	
		
		GridPane grid = new GridPane();
		grid.add(stardistLocationLabel, 1, 1);
		grid.add(stardistLocationText, 2, 1);		
		grid.add(stardistExecChsrBtn, 3, 1);		
		dialog.getDialogPane().setContent(grid);
		         
		final ButtonType buttonTypeOk = new ButtonType("Ok", ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().add(buttonTypeOk);
		 
		dialog.setResultConverter((Callback<ButtonType, Map<String, String>>) new Callback<ButtonType, Map<String, String>>() {
		    @Override
		    public Map<String, String> call(ButtonType b) {
		 
		        if (b == buttonTypeOk) {
		        	final Map<String, String> result = new HashMap<String, String>();
		        	result.put("stardistLocation", stardistLocationText.getText());
		        	
		            return result;
		        }
		 
		        return null;
		    }
		});
		         
		final Optional<Map<String, String>> result = dialog.showAndWait();
		         
		if (result.isPresent()) {	
			stardistLocationProperty.set(result.get().get("stardistLocation"));			
		}		
	}
}