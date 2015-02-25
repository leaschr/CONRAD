package CTRecon;


import ij.ImageJ;
import edu.stanford.rsl.conrad.data.numeric.Grid1D;
import edu.stanford.rsl.conrad.data.numeric.Grid1DComplex;
import edu.stanford.rsl.conrad.data.numeric.Grid2D;
import edu.stanford.rsl.conrad.data.numeric.InterpolationOperators;



public class FilteredBackProjection extends BasicImageOperations {

	public FilteredBackProjection(int width, int height) {
		super(width, height);
	}
	
	public static void filter(Grid2D sinogram) {
		
		// go through all angles
		for (int t = 0; t < sinogram.getHeight(); t++) {
			
			Grid1DComplex proj = new Grid1DComplex(sinogram.getSubGrid(t));
			
			int spacing = proj.getSize()[0] / sinogram.getWidth();

			// Nyquist
			float maxFreq = (float) 1.0f / (2.0f * spacing);
			
			// FFT
			proj.transformForward();
			
			// number of Entries after FFT
			int numberOfEntries = proj.getSize()[0];
			
			float stepsize = (maxFreq * 2.0f) / numberOfEntries;
			
			// first part -> ascending ramp
			for (int i = 0; i < numberOfEntries/2; i++) {
				proj.multiplyAtIndex(i, i * stepsize);
			}
			
			// second part -> descending ramp
			for (int i = numberOfEntries/2; i < numberOfEntries; i++) {
				proj.multiplyAtIndex(i, (numberOfEntries - i) * stepsize);
			}
			
			
			// Inverse FFT
			proj.transformInverse();
			
			for (int s = 0; s < proj.getSize()[0]/2;s++) {
				sinogram.putPixelValue(s, t, proj.getRealAtIndex(s));
			}

		}

	}
	
	public void filterRamLak(Grid2D sinogram) {
		
		// go through all angles
		for (int t = 0; t < sinogram.getHeight(); t++) {
			
			Grid1DComplex proj = new Grid1DComplex(sinogram.getSubGrid(t));
			
			// FFT of projection
			proj.transformForward();
			
			// Ram-Lak filter in spatial domain
			Grid1DComplex ramlak = new Grid1DComplex(proj.getSize()[0]/2);
			
			// length of Ram-Lak
			int length = ramlak.getSize()[0];
			
			ramlak.setAtIndex(0, 1.0f/4.0f);
			for (int i = 1; i < length/2; i++) {
				
				if (i % 2 == 0) {
					// even
					ramlak.setAtIndex(i, 0);
				} else {
					// odd
					ramlak.setAtIndex(i, (float) (-1.0f/(i*i*Math.PI*Math.PI)));
				}
			}
			
			for (int i = length/2; i < length; i++) {
				if ((length-i) % 2 == 0) {
					// even
					ramlak.setAtIndex(i, 0);
				} else {
					// odd
					ramlak.setAtIndex(i, (float) (-1.0f/((length-i)*(length-i)*Math.PI*Math.PI)));
				}
			}

			
			// FFT of Ram-Lak
			ramlak.transformForward();
			
			
			// Multiplicate in Fourier space
			for (int i = 0; i < length; i++) {
				proj.multiplyAtIndex(i, ramlak.getRealAtIndex(i), ramlak.getImagAtIndex(i));
			}
			
			// Inverse FFT
			proj.transformInverse();
			
			for (int s = 0; s < sinogram.getWidth();s++) {
				sinogram.putPixelValue(s, t, proj.getRealAtIndex(s));
			}

		}
		
	}
	
	public Grid2D backproject(Grid2D sinogram) {
		
		int width = this.getWidth();
		int height = this.getHeight();
		
		Grid2D backprojected = new Grid2D(width, height);
		
		for (int t = 0; t < 180; t++) {
			for (int x = -width/2; x < width/2; x++) {
				for (int y = -height/2; y < height/2; y++) {
					
					// search for s and interpolate
					float theta = (float) ((float)t /180 * Math.PI);
					float s = (float) (x * Math.cos(theta) + y * Math.sin(theta));
					
					// interpolate linear on detector
					float value = InterpolationOperators.interpolateLinear(sinogram.getSubGrid(t), s + sinogram.getWidth()/2);
					
					
					int x_new = x + width/2;
					int y_new = y + height/2;
					
					// set value
					backprojected.putPixelValue(x_new, y_new, backprojected.getAtIndex(x_new, y_new) + value);
				}
			}
		}
		return backprojected;
	}
	
	
	public Grid2D filteredBackproject(Grid2D sinogram) {
		filter(sinogram);
		return this.backproject(sinogram);
	}
	
	public Grid2D filteredBackprojectRamLak(Grid2D sinogram) {
		filterRamLak(sinogram);
		return this.backproject(sinogram);
	}


	public static void main(String[] args) {

		int widthPhantom = 80;
		int heightPhantom = 80;
		
		// create phantom
		FilteredBackProjection phantom = new FilteredBackProjection(widthPhantom,heightPhantom);
		new ImageJ();
		
		phantom.show("Grid");	

		
		int detectorLength = (int) Math.sqrt(widthPhantom*widthPhantom+heightPhantom*heightPhantom);
		
		// generation of sinogram
		Grid2D sinogram = phantom.generateSinogram(detectorLength);
		sinogram.show("Sinogram");
		
		Grid2D filteredSinogram = new Grid2D(sinogram);
		
		// filtered Backprojection
		Grid2D filtBackproject = phantom.filteredBackproject(sinogram);
		filtBackproject.show();
		
		// filtered Backprojection Ram-Lak
		Grid2D filtBackprojectRamLak = phantom.filteredBackprojectRamLak(filteredSinogram);
		filtBackprojectRamLak.show();

	}

}
