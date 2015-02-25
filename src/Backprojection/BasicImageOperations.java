package CTRecon;

import java.awt.Color;
import java.awt.image.ColorModel;

import edu.stanford.rsl.conrad.data.numeric.Grid2D;
import edu.stanford.rsl.conrad.data.numeric.InterpolationOperators;
import ij.*;
import ij.process.*;


public class BasicImageOperations extends Grid2D {

	public BasicImageOperations(int width, int height) {
		super(width, height);
		
		helperCircle(width, height, width/2, height/2, 20, 30, 1.0f);
		
		helperRect(width, height, (int) (5.0f*width/12), (int) (5.0f*width/12), 5, 5, 0.8f);
		helperRect(width, height, (int) (7.0f*width/12), (int) (5.0f*width/12), 5, 5, 0.8f);
		
		
		
		/*ImagePlus imp = IJ.createImage("My new image", "8-bit black", width, height, 1);  
		ImageProcessor ip = imp.getProcessor();  
		
		Color c = new Color(211,211,211);
		ip.setColor(c);
		ip.fillOval(width/8, height/8, width/2, (int)(0.7*height));
		
		ip.setColor(new Color(255,255,255));
		// circle
		//ip.fillOval(width/4, height/4, width/2, height/2);
		
		ip.fillOval(width/2 - width/20, height/2 - height/10, width/10, height/10);
		ip.fillOval(width/2 - width/4, height/2 - height/10, width/10, height/10);
	
		buffer = (float[]) ip.toFloat(1, new FloatProcessor(width, height)).getPixels();
		initialize(width, height);
		notifyAfterWrite();*/
		
	}
	
	
	private void helperRect(int width, int height, int x, int y, int widthObject, int heightObject, float colorIndex) {
		
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				if ((Math.abs(i - x) < widthObject/2) && (Math.abs(j - y) < heightObject/2)) {
					// inside
					setAtIndex(i, j, colorIndex);
				}
			}
		}
		
	}
	
	private void helperCircle(int width, int height, int x, int y, int widthObject, int heightObject, float colorIndex) {
		
		int a = widthObject/2;
		int b = heightObject/2;
		
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				if (b*b*(i-x)*(i-x) + a*a*(j-y)*(j-y) < a*a*b*b) {
					// inside
					setAtIndex(i, j, colorIndex);
				}
			}
		}
		
	}
	
	

	protected Grid2D generateSinogram(int detectorLength) {
		
		Grid2D sinogram = new Grid2D(detectorLength, 180);
		
		for (int t = 0; t < 180; t++) {
			for (int s = -detectorLength/2; s < detectorLength/2; s++) {
				
				double value = 0.0;
				float theta = (float) ((float)t /180*Math.PI);
				
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
				sinogram.putPixelValue(s + detectorLength/2, t, value);
			}
			
		}
		
		return sinogram;
		
	}
	
	
	public static void main(String[] args) {
		BasicImageOperations phantom = new BasicImageOperations(40, 40);
		new ImageJ();
		
		phantom.show("Grid");	

		Grid2D sinogram = phantom.generateSinogram(128);

		sinogram.show("Sinogram");
		      
	}

}
