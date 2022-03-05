/*-
 * Copyright 2020-2021 QuPath developers,  University of Edinburgh
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package qupath.ext.stardist;

import org.controlsfx.control.action.Action;

import javafx.scene.control.Menu;
import qupath.lib.common.Version;
import qupath.lib.gui.ActionTools;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.GitHubProject;
import qupath.lib.gui.extensions.QuPathExtension;
import qupath.lib.gui.tools.MenuTools;

/**
 * Install StarDist as an extension.
 * <p>
 * Currently this doesn't really do anything much, beyond including a reference 
 * in the listed extensions of QuPath and enabling some compatibility/update checks.
 * StarDist itself is only accessible via scripting.
 * In the future, the extension may also add a UI.
 * <p>
 * UPDATE (3/5/2022): Added UI entry for StarDist Nuclei Detection
 * 
 * @author Pete Bankhead, C. H. Huang
 */
public class StarDistExtension implements QuPathExtension, GitHubProject {

	@Override
	public void installExtension(QuPathGUI qupath) {
		final Menu menu = qupath.getMenu("Extensions", true);
		
		MenuTools.addMenuItems(
				menu,
				null,
				qupath.createPluginAction("StarDist-based Nucleus Detection", StarDistCellNucleusDetection.class, null)
				);		
	}

	@Override
	public String getName() {
		return "StarDist extension";
	}

	@Override
	public String getDescription() {
		return "Run StarDist nucleus detection via scripting.\n"
				+ "See the extension repository for citation information.";
	}
	
	@Override
	public Version getQuPathVersion() {
		return Version.parse("0.3.0-rc2");
	}

	@Override
	public GitHubRepo getRepository() {
		return GitHubRepo.create(getName(), "qupath", "qupath-extension-stardist");
	}

}
