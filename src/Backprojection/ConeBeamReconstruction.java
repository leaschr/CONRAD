package CTRecon;

import java.io.IOException;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import edu.stanford.rsl.conrad.data.numeric.Grid3D;
import edu.stanford.rsl.conrad.data.numeric.NumericPointwiseOperators;
import edu.stanford.rsl.conrad.filtering.CosineWeightingTool;
import edu.stanford.rsl.conrad.filtering.ExtremeValueTruncationFilter;
import edu.stanford.rsl.conrad.filtering.ImageFilteringTool;
import edu.stanford.rsl.conrad.filtering.RampFilteringTool;
import edu.stanford.rsl.conrad.filtering.TruncationCorrectionTool;
import edu.stanford.rsl.conrad.filtering.redundancy.ParkerWeightingTool;
import edu.stanford.rsl.conrad.geometry.trajectories.Trajectory;
import edu.stanford.rsl.conrad.opencl.OpenCLBackProjector;
import edu.stanford.rsl.conrad.opencl.OpenCLForwardProjector;
import edu.stanford.rsl.conrad.phantom.NumericalSheppLogan3D;
import edu.stanford.rsl.conrad.utils.Configuration;
import edu.stanford.rsl.conrad.utils.FileUtil;
import edu.stanford.rsl.conrad.utils.ImageUtil;
import edu.stanford.rsl.tutorial.RamLakKernel;
import edu.stanford.rsl.tutorial.cone.ConeBeamBackprojector;
import edu.stanford.rsl.tutorial.cone.ConeBeamCosineFilter;
import edu.stanford.rsl.tutorial.cone.ConeBeamProjector;
import edu.stanford.rsl.tutorial.phantoms.Phantom3D;
import edu.stanford.rsl.tutorial.phantoms.Sphere3D;

/**
 * Simple example that computes and displays a cone-beam reconstruction.
 * 
 * @author Recopra Seminar Summer 2012
 * 
 */
public class ConeBeamReconstruction {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new ImageJ();
		
		Configuration conf = Configuration.loadConfiguration("/proj/ciptmp/recoData/CONRADsettings.xml");
		Configuration.setGlobalConfiguration(conf);
		
		Trajectory geo = conf.getGeometry();
		double focalLength = geo.getSourceToDetectorDistance();
		int maxU_PX = geo.getDetectorWidth();
		int maxV_PX = geo.getDetectorHeight();
		double deltaU = geo.getPixelDimensionX();
		double deltaV = geo.getPixelDimensionY();
		double maxU = (maxU_PX) * deltaU;
		double maxV = (maxV_PX) * deltaV;
		int imgSizeX = geo.getReconDimensionX();
		int imgSizeY = geo.getReconDimensionY();
		int imgSizeZ = geo.getReconDimensionZ();
		//Phantom3D test3D = new Sphere3D(imgSizeX, imgSizeY, imgSizeZ);
		
		
		
		Grid3D impAsGrid = null;
		  try {
		    // we need ImageJ in the following
		    //new ImageJ();
		    // locate the file
		    // here we only want to select files ending with ".bin". This will open them as "Dennerlein" format.
		    // Any other ImageJ compatible file type is also OK.
		    // new formats can be added to HandleExtraFileTypes.java
		    //String filenameString = FileUtil.myFileChoose(".tif", false);
		    // call the ImageJ routine to open the image:
		    ImagePlus imp = IJ.openImage("/proj/ciptmp/recoData/DensityProjection_No248_Static60_0.8deg_REFERENCE.tif");
		    // Convert from ImageJ to Grid3D. Note that no data is copied here.
		    // The ImageJ container is only wrapped. Changes to the Grid will also affect the ImageJ ImagePlus.
		    impAsGrid = ImageUtil.wrapImagePlus(imp);
		    // Display the data that was read from the file.
		    impAsGrid.show("Data from file");
		  } catch (Exception e) {
		    e.printStackTrace();
		  }
	
		Grid3D sino = impAsGrid;
		
		
		
		ParkerWeightingTool pwt = new ParkerWeightingTool();
		CosineWeightingTool cwt = new CosineWeightingTool();
		RampFilteringTool rft = new RampFilteringTool();
		OpenCLBackProjector back = new OpenCLBackProjector();
		
		try {
			pwt.configure();
			cwt.configure();
			rft.configure();
			back.configure();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		ImageFilteringTool[] filters = new ImageFilteringTool[] {pwt, cwt, rft, back};
		
		
		Grid3D filt = ImageUtil.applyFiltersInParallel(sino, filters);
		//filt.show("after Filtering");
		
		Grid3D recImage = filt;
			
		//ConeBeamBackprojector cbbp = new ConeBeamBackprojector();
		//Grid3D recImage = cbbp.backprojectPixelDrivenCL(filt);
		recImage.show("recImage");
		if (true)
			return;
	

	}
}


