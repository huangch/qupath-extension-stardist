/*-
 * #%L
 * This file is part of QuPath.
 * %%
 * Copyright (C) 2014 - 2016 The Queen's University of Belfast, Northern Ireland
 * Contact: IP Management (ipmanagement@qub.ac.uk)
 * Copyright (C) 2018 - 2020 QuPath developers, The University of Edinburgh
 * %%
 * QuPath is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * QuPath is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with QuPath.  If not, see <https://www.gnu.org/licenses/>.
 * #L%
 */

package qupath.ext.stardist;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.objects.PathAnnotationObject;
import qupath.lib.objects.PathObject;
import qupath.lib.plugins.AbstractInteractivePlugin;
import qupath.lib.plugins.PluginRunner;
import qupath.lib.plugins.parameters.ParameterList;

import qupath.ext.stardist.StarDist2D;
import qupath.ext.stardist.StarDist2D.Builder;

/**
 * The Qupath Interactive Plusin implementation for Star-Dist nucleus detection.
 * 
 * @author C.H. Huang
 *
 * @param <T>
 */
public class StarDistCellNucleusDetection<T> extends AbstractInteractivePlugin<T> {
	private static Logger logger = LoggerFactory.getLogger(StarDistCellNucleusDetection.class);
	
    private static final boolean COMPILE_TIME = true;
    private static final String COMPILE_TIME_MODEL_LOCATION = "/workspace/sptx/qupath-main/qupath-extension-stand/python_backend";
				
	private String resultString = null;

	@Override
	protected void preprocess(final PluginRunner<T> pluginRunner) {
	};
	
	@Override
	public Collection<Class<? extends PathObject>> getSupportedParentObjectClasses() {
		return Collections.singleton(PathAnnotationObject.class);
	}

	@Override
	public String getName() {
		return "Stardict-based Cell Nucleus Detection";
	}

	@Override
	public String getDescription() {
		return "Stardict-based Cell Nucleus Detection";
	}

	@Override
	public String getLastResultsDescription() {
		return resultString;
	}

	@Override
	public ParameterList getDefaultParameterList(ImageData<T> imageData) {
		
		try {
			if(!imageData.getServer().getPixelCalibration().hasPixelSizeMicrons()) {
				final Alert a = new Alert(AlertType.ERROR);
				
				a.setTitle("Error");
				a.setHeaderText("Something wrong in the image properties.");
				a.setContentText("Please check the image properties in left panel. Most likely the pixel size is unknown.");

				a.showAndWait();
				
				throw new Exception("No pixel size information");
			}
			
			final String mainPathUtf8 = StarDistCellNucleusDetection.class.getProtectionDomain().getCodeSource().getLocation().getPath();
			final String mainPathStr = URLDecoder.decode(mainPathUtf8, "UTF-8");
			final String modelLocation = COMPILE_TIME?
					COMPILE_TIME_MODEL_LOCATION:
					mainPathStr.substring(System.getProperty("os.name").startsWith("Windows")? 1: 0, mainPathStr.lastIndexOf("/"));
			
			logger.info("StarDist Model Location: "+modelLocation);
			
    		final File modelDir = new File(modelLocation);
    		final FileFilter modelFileFilter = new WildcardFileFilter("*.pb");
    		File[] modelFiles = modelDir.listFiles(modelFileFilter);

    		final List<String> modellList = new ArrayList<String>();
    		for(File f: modelFiles) {
    			modellList.add(FilenameUtils.getName(f.toString()));
    		}
    		    		    	    		
    		final ImageServer<BufferedImage> server = (ImageServer<BufferedImage>) imageData.getServer();		
    		final double imagePixelSizeMicrons = server.getPixelCalibration().getAveragedPixelSizeMicrons();
    		
			final ParameterList params = new ParameterList()
					.addTitleParameter("General Parameters")			
					.addDoubleParameter("threshold", "Probability (detection) threshold", 0.1, null, "Probability (detection) threshold")
					.addDoubleParameter("normalizePercentilesLow", "Percentile normalization (lower bound)", 1, null, "Percentile normalization (lower bound)")
					.addDoubleParameter("normalizePercentilesHigh", "Percentile normalization (higher bound)", 99, null, "Percentile normalization (lower bound)")
					.addTitleParameter("Measurements")
					.addBooleanParameter("includeProbability", "Add probability as a measurement (enables later filtering). Default: false", false, "Add probability as a measurement (enables later filtering)")
					.addBooleanParameter("measureShape", "Add shape measurements. Default: false", false, "Add shape measurements")
					.addBooleanParameter("measureIntensity", "Add shape measurements. Default: false", false, "Add shape measurements")
					.addTitleParameter("Additional Parameters")
					.addChoiceParameter("pathModel", "Specify the model .pb file", modellList.get(0), modellList, "Choose the model that should be used for object classification")
					.addDoubleParameter("pixelSize", "Resolution for detection. Default: value provided by QuPath.", imagePixelSizeMicrons, null, "Resolution for detection")
					.addStringParameter("channel", "Select detection channel (e.g., DAPI. Default: [empty] = N/A)", "")
					.addDoubleParameter("cellExpansion", "Approximate cells based upon nucleus expansion (e.g., 5.0. Default: -1 = N/A)", -1, null, "Approximate cells based upon nucleus expansion")		
					.addDoubleParameter("cellConstrainScale", "Constrain cell expansion using nucleus size (e.g., 1.5. Default: -1 = N/A)", -1, null, "Constrain cell expansion using nucleus size")
					;
			
			return params;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}

	@Override
	protected Collection<? extends PathObject> getParentObjects(PluginRunner<T> runner) {
		return getHierarchy(runner).getSelectionModel().getSelectedObjects().stream().filter(p -> p.isAnnotation()).collect(Collectors.toList());
	}

	@Override
	protected void addRunnableTasks(ImageData<T> imageData, PathObject parentObject, List<Runnable> tasks) {}
	
	@Override
	protected Collection<Runnable> getTasks(final PluginRunner<T> runner) {
		final Collection<? extends PathObject> parentObjects = getParentObjects(runner);
		if (parentObjects == null || parentObjects.isEmpty())
			return Collections.emptyList();
		
		// Add a single task, to avoid multithreading - which may complicate setting parents
		final List<Runnable> tasks = new ArrayList<>(parentObjects.size());
		
		final String pathModel = (String)params.getChoiceParameterValue("pathModel");
		final double threshold = params.getDoubleParameterValue("threshold");
		final String channels = params.getStringParameterValue("channel");
		final double normalizePercentilesLow = params.getDoubleParameterValue("normalizePercentilesLow");
		final double normalizePercentilesHigh = params.getDoubleParameterValue("normalizePercentilesHigh");
		final double pixelSize = params.getDoubleParameterValue("pixelSize");
		final double cellExpansion = params.getDoubleParameterValue("cellExpansion");
		final double cellConstrainScale = params.getDoubleParameterValue("cellConstrainScale");
		final boolean measureShape = params.getBooleanParameterValue("measureShape");
		final boolean measureIntensity = params.getBooleanParameterValue("measureIntensity");
		final boolean includeProbability = params.getBooleanParameterValue("includeProbability");
		
		parentObjects.forEach(p -> {
			tasks.add(() -> {
				runDetection(
						(ImageData<BufferedImage>) runner.getImageData(), 
						p, 
						pathModel, 
						threshold, 
						channels, 
						normalizePercentilesLow, 
						normalizePercentilesHigh, 
						pixelSize, 
						cellExpansion,
						cellConstrainScale, 
						measureShape, 
						measureIntensity, 
						includeProbability);
			});
		});
		return tasks;
	}
	
	/**
	 * Create and add a new annotation by expanding the ROI of the specified PathObject.
	 * 
	 * @param imageData
	 * @param parentObject
	 * @param pathModel
	 * @param threshold
	 * @param channels
	 * @param normalizePercentilesLow
	 * @param normalizePercentilesHigh
	 * @param pixelSize
	 * @param cellExpansion
	 * @param cellConstrainScale
	 * @param measureShape
	 * @param measureIntensity
	 * @param includeProbability
	 */
	private static void runDetection(
			ImageData<BufferedImage> imageData, PathObject parentObject, String pathModel,
			double threshold, String channels, double normalizePercentilesLow, double normalizePercentilesHigh,
			double pixelSize, double cellExpansion, double cellConstrainScale, boolean measureShape,
			boolean measureIntensity, boolean includeProbability
			) {
		
		try {
		
			final List<PathObject> parentObjects = new ArrayList<PathObject>();
			parentObjects.add(parentObject);
			
			final String mainPathUtf8 = StarDistCellNucleusDetection.class.getProtectionDomain().getCodeSource().getLocation().getPath();
			final String mainPathStr = URLDecoder.decode(mainPathUtf8, "UTF-8");
			final String modelLocation = COMPILE_TIME? 
					COMPILE_TIME_MODEL_LOCATION: 
					mainPathStr.substring(System.getProperty("os.name").startsWith("Windows")? 1: 0, mainPathStr.lastIndexOf("/"));
			
			logger.info("StarDist Model Location: "+modelLocation);
			
			final Path stardistModel = Paths.get(modelLocation, pathModel);
			
			final Builder stardistBuilder = StarDist2D.builder(stardistModel.toString())
			        .threshold(threshold)
			        .normalizePercentiles(normalizePercentilesLow, normalizePercentilesHigh)
			        .pixelSize(pixelSize);
	
	        if(!channels.isBlank()) stardistBuilder.channels(channels);
	        if(cellExpansion > 0) stardistBuilder.cellExpansion(cellExpansion);
	        if(cellConstrainScale > 0) stardistBuilder.cellConstrainScale(cellConstrainScale);
			if(measureShape) stardistBuilder.measureShape();
			if(measureIntensity) stardistBuilder.measureIntensity();
			if(includeProbability) stardistBuilder.includeProbability(true);
			
			final StarDist2D stardist = stardistBuilder.build();
			
			stardist.detectObjects((ImageData<BufferedImage>) imageData, parentObjects);
			
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
