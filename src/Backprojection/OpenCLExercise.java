package CTRecon;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLProgram;

import ij.ImageJ;
import edu.stanford.rsl.conrad.data.numeric.Grid2D;
import edu.stanford.rsl.conrad.data.numeric.NumericPointwiseOperators;
import edu.stanford.rsl.conrad.data.numeric.opencl.OpenCLGrid2D;
import edu.stanford.rsl.conrad.opencl.OpenCLUtil;


public class OpenCLExercise extends BasicImageOperations {
	
	public OpenCLExercise(int width, int height) {
		super(width, height);
	}

	public static long timeCPU(Grid2D phantom) {
		
		long time_Start = System.currentTimeMillis();
		
		int width = phantom.getWidth();
		int height = phantom.getHeight();
		
		for (long i = 0; i < 1e6; i++) {
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					phantom.addAtIndex(x, y, phantom.getAtIndex(x, y));
				}
			}
		}
		
		long time_End = System.currentTimeMillis();
		
		return time_End - time_Start;
		
	}
	
	public static long timeOpenCL(Grid2D phantom) {
		
		long time_Start = System.currentTimeMillis();
		
		CLContext context = OpenCLUtil.getStaticContext();
		CLDevice device = context.getMaxFlopsDevice();
		
		OpenCLGrid2D clPhantom = new OpenCLGrid2D(phantom, context, device);
		
		
		for (long i = 0; i < 1e6; i++) {
			NumericPointwiseOperators.addBy(clPhantom, clPhantom);
		}
		
		long time_End = System.currentTimeMillis();
		
		return time_End - time_Start;

	}
	
	
	public static Grid2D addGridsOpenCL(Grid2D grid1, Grid2D grid2) {
		
		OpenCLGrid2D grid1OpenCL = new OpenCLGrid2D(grid1);
		OpenCLGrid2D grid2OpenCL = new OpenCLGrid2D(grid2);
		OpenCLGrid2D grid3OpenCL = new OpenCLGrid2D(grid2);
		
		
		
		// get context and fastest device
		CLContext context = OpenCLUtil.getStaticContext();
		CLDevice device = context.getMaxFlopsDevice();
		
		// create command queue
		CLCommandQueue queue = device.createCommandQueue();
		
		// get local and global size
		int elementCount = grid1OpenCL.getWidth() * grid1OpenCL.getHeight(); // elements in image
		int localSize = 128;
		int globalSize = OpenCLUtil.roundUp(localSize, elementCount);
		
		// create program
		CLProgram program = null;
		try {
			InputStream programFile = OpenCLExercise.class.getResourceAsStream("addGrids.cl");
			program = context.createProgram(programFile).build();
		} catch (IOException e) {
			e.printStackTrace();
		}


		// a and b for input, c for result
		grid1OpenCL.getDelegate().prepareForDeviceOperation();
		CLBuffer<FloatBuffer> clBufferA = grid1OpenCL.getDelegate().getCLBuffer();
		grid2OpenCL.getDelegate().prepareForDeviceOperation();
		CLBuffer<FloatBuffer> clBufferB = grid2OpenCL.getDelegate().getCLBuffer();
		grid3OpenCL.getDelegate().prepareForDeviceOperation();
		CLBuffer<FloatBuffer> clBufferC = grid3OpenCL.getDelegate().getCLBuffer();

		
		
		// create kernel and give arguments
		CLKernel kernel = program.createCLKernel("add");
		kernel.putArgs(clBufferA, clBufferB, clBufferC)
		.putArg(elementCount);
		
		
		// put values on CLBuffer
		queue
		.put1DRangeKernel(kernel, 0, globalSize, localSize)
		.finish();
		
		
		// set flag to notify changes in buffer
		grid3OpenCL.getDelegate().notifyDeviceChange();		
		
		
		// get values back from CLBuffer
		float bufferResult[] = new float[elementCount];
		for (int i = 0; i < elementCount; i++) {
			bufferResult[i] = clBufferC.getBuffer().get();
		}
			
		// create new OpenCLGrid
		Grid2D grid3 = new Grid2D(bufferResult, grid1OpenCL.getWidth(), grid1OpenCL.getHeight());
		return grid3;

		
	}
	
	public Grid2D backprojectOpenCL(Grid2D sinogram) {
		
		int width = this.getWidth();
		int height = this.getHeight();
		
		OpenCLGrid2D sino = new OpenCLGrid2D(sinogram);
		OpenCLGrid2D backprojected = new OpenCLGrid2D(new Grid2D(width, height));
		
		int sinogramLength = sinogram.getWidth();
		
		// get context and fastest device
		CLContext context = OpenCLUtil.getStaticContext();
		CLDevice device = context.getMaxFlopsDevice();
		
		// create command queue
		CLCommandQueue queue = device.createCommandQueue();
		
		// get local and global size
		int elementCountX = width; // elements in image in x-direction
		int elementCountY = height;	// elements in image in y-direction
		int localSizeX = 16;
		int globalSizeX = OpenCLUtil.roundUp(localSizeX, elementCountX);
		
		int localSizeY = 16;
		int globalSizeY = OpenCLUtil.roundUp(localSizeY, elementCountY);
		
		
		// create program
		CLProgram program = null;
		try {
			InputStream programFile = OpenCLExercise.class.getResourceAsStream("backproject.cl");
			program = context.createProgram(programFile).build();
		} catch (IOException e) {
			e.printStackTrace();
		}


		sino.getDelegate().prepareForDeviceOperation();
		CLBuffer<FloatBuffer> clBufferA = sino.getDelegate().getCLBuffer();
		backprojected.getDelegate().prepareForDeviceOperation();
		CLBuffer<FloatBuffer> clBufferB = backprojected.getDelegate().getCLBuffer();
	
		
		
		// create kernel and give arguments
		CLKernel kernel = program.createCLKernel("backproject");
		kernel.putArgs(clBufferA, clBufferB)
		.putArg(elementCountX)
		.putArg(elementCountY)
		.putArg(sinogramLength);
		
		
		// put values on CLBuffer
		queue
		.put2DRangeKernel(kernel, 0, 0, globalSizeX, globalSizeY, localSizeX, localSizeY)
		.finish();
		
		
		// set flag to notify changes in buffer
		backprojected.getDelegate().notifyDeviceChange();		
		
		return new Grid2D(backprojected);
		
	}

	
	public static void main(String[] args) {
		
		int widthPhantom = 512;
		int heightPhantom = 512;
		
		int detectorLength = (int) Math.sqrt(widthPhantom*widthPhantom+heightPhantom*heightPhantom);
		
		OpenCLExercise phantom = new OpenCLExercise(widthPhantom, heightPhantom);
		new ImageJ();
		
		phantom.show("Original");
		
		//long timeCPU = timeCPU  (phantom);
		//long timeOpenCL = timeOpenCL(phantom);
		
		// convert to seconds
		//float timeCPUSeconds = (float) timeCPU / 1000;
		//float timeOpenCLSeconds = (float) timeOpenCL / 1000;
		
		//System.out.println("CPU time: " + timeCPUSeconds + "s\nGPU time: " + timeOpenCLSeconds + "s");
		
		//Grid2D result = addGridsOpenCL(phantom, phantom);

		//result.show("Added");
		
		// difference image
		//Grid2D difference = (Grid2D) NumericPointwiseOperators.subtractedBy(result, phantom);
		//difference.show("Difference");


		// generation of sinogram
		Grid2D sinogram = phantom.generateSinogram(detectorLength);
		sinogram.show("Sinogram");
		
		FilteredBackProjection fbp = new FilteredBackProjection(widthPhantom, heightPhantom);
		
		fbp.filterRamLak(sinogram);
		sinogram.show("Filtered Sinogram");
				
		long time1 = System.currentTimeMillis();
		Grid2D backprojectedOpenCL = phantom.backprojectOpenCL(sinogram);
		backprojectedOpenCL.show("Backprojection OpenCL");
		long time2 = System.currentTimeMillis();
		long timeDiffGPU = time2 - time1;
		float timeDiffGPUSeconds = (float)timeDiffGPU / 1000;
		
		time1 = System.currentTimeMillis();
		Grid2D backprojected = fbp.backproject(sinogram);
		backprojected.show("Backprojection CPU");
		time2 = System.currentTimeMillis();
		long timeDiffCPU = time2 - time1;
		float timeDiffCPUSeconds = (float)timeDiffCPU / 1000;
		
		System.out.println("Time for GPU Backprojection: " + timeDiffGPUSeconds + "s\nTime for CPU Backprojection: " + timeDiffCPUSeconds + "s");
		
	}

}
