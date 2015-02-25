package CTRecon;

import ij.ImageJ;
import edu.stanford.rsl.conrad.data.numeric.Grid2D;
import edu.stanford.rsl.conrad.data.numeric.InterpolationOperators;
import edu.stanford.rsl.conrad.data.numeric.NumericPointwiseOperators;

public class FanBeamReconstruction extends BasicImageOperations {

	public FanBeamReconstruction(int width, int height) {
		super(width, height);
	}
	
	protected Grid2D generateSinogram(int detectorLength, int projections) {
		
		Grid2D fanogram = new Grid2D(detectorLength, projections);
		// radius of source
		int d = detectorLength/2+100;
		
		for (int beta = 0; beta < projections; beta++) {
			for (int t = -detectorLength/2; t < detectorLength/2; t++) {
				
				double betaRad = (double)beta / 180 * Math.PI;
				double gamma = Math.atan((double)t/d);
				
				double s = d * Math.sin(gamma);
				double theta = gamma + betaRad;
				
				double value = 0.0;
				
				if (Math.abs(Math.tan(theta)) < 1) {
					// small angles
					for (int y = -this.getHeight()/2; y < this.getHeight()/2; y++) {
						float x = (float)((s-y*Math.sin(theta))/Math.cos(theta));
						value += InterpolationOperators.interpolateLinear(this, x + getWidth()/2, y + getHeight()/2);
					}
				} else {
					// big angles
					for (int x = -this.getWidth()/2; x < this.getWidth()/2; x++) {
						float y = (float)((s-x*Math.cos(theta))/Math.sin(theta));
						value += InterpolationOperators.interpolateLinear(this, x + getWidth()/2, y + getHeight()/2);
					}
					
				}
				fanogram.putPixelValue(t + detectorLength/2, beta, value);
			}
		}

		return fanogram;
	}
	
	public static Grid2D Fanogram2Sinogram(Grid2D fanogram, int offset, int projections) {
		
		int detectorLength = fanogram.getWidth();
		// radius of source
		int d = detectorLength/2 + offset;
		
		
		Grid2D sinogram = new Grid2D(fanogram.getWidth(), fanogram.getHeight());
		
		for (int s = -fanogram.getWidth()/2; s < fanogram.getWidth()/2; s++) {
			for (int theta = 0; theta < fanogram.getHeight(); theta++) {
				
				double thetaRad = (double)theta / 180 * Math.PI;
				
				double gamma = Math.asin((double)s/d);
				
				double beta = thetaRad - gamma;
				double t = d*Math.tan(gamma);
				
				// go back to angles in degree
				beta = beta *180 / Math.PI;
				
				if (beta < 0) {
					beta = beta + 2.0 * gamma*180/Math.PI + 180;
					t = -t;
					//beta += projections - 1;
				} else if (beta >= projections - 1) {
					//beta -= projections - 1;
					beta = beta + 2.0 * gamma*180/Math.PI - 180;
					t = -t;
				}
				
				double value = InterpolationOperators.interpolateLinear(fanogram, t + fanogram.getWidth()/2, beta);
				
				sinogram.putPixelValue(s + fanogram.getWidth()/2, theta, value);
				
			}
		}
		
		return sinogram;
		
	}
	
	public Grid2D shortScan(int detectorLength, int projections) {
		
		// generation of fanogram
		Grid2D fanogram = this.generateSinogram(detectorLength, projections);
		fanogram.show("Fanogram");
		
		// convert fanogram to sinogram
		Grid2D sinogram = Fanogram2Sinogram(fanogram, 100, projections);
		sinogram.show("Sinogram");
		
		FilteredBackProjection fi = new FilteredBackProjection(this.getWidth(), this.getHeight());
	
		// filtered Backprojection
		Grid2D filtBackproject = fi.filteredBackproject(sinogram);
		
		return filtBackproject;
	}

	

	public static void main(String[] args) {
		
		int widthPhantom = 80;
		int heightPhantom = 80;
		int projections = 360;
		
		int detectorLength = (int) Math.sqrt(widthPhantom*widthPhantom+heightPhantom*heightPhantom);
		// radius
		int d = detectorLength/2 + 10;
		
		double gamma = Math.atan((double)detectorLength/(2*d));
		
		// convert to degree
		gamma = gamma * 180 / Math.PI;
		
		int projectionsShort = (int) (180 + 2.0 * gamma);
		
		// create phantom
		FanBeamReconstruction phantom = new FanBeamReconstruction(widthPhantom, heightPhantom);
		new ImageJ();
		
		phantom.show("Grid");	

		

		
		// generation of fanogram
		Grid2D fanogram = phantom.generateSinogram(detectorLength, projections);
		fanogram.show("Fanogram");
		
		// convert fanogram to sinogram
		Grid2D sinogram = Fanogram2Sinogram(fanogram, 100, projections);
		sinogram.show("Sinogram");
		
		FilteredBackProjection fi = new FilteredBackProjection(widthPhantom, heightPhantom);

		// filtered Backprojection
		Grid2D filtBackproject = fi.filteredBackproject(sinogram);
		filtBackproject.show();
		
		
		// short scan
		Grid2D shortscanRecon = phantom.shortScan(detectorLength, projectionsShort);
		shortscanRecon.show();
		
		// difference image
		Grid2D difference = (Grid2D) NumericPointwiseOperators.subtractedBy(phantom, shortscanRecon);
		difference.show();

	}

}
