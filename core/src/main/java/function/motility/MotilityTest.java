package function.motility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import fiji.threshold.Auto_Local_Threshold;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileSaver;
import ij.plugin.AVI_Reader;
import ij.process.ImageProcessor;
import movieio.jmf.JMF_MovieReaderSimple;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.CLIJ2Ops;
import net.haesleinhuepf.clij2.plugins.Median2DBox;
import net.haesleinhuepf.clij2.plugins.Median2DSphere;
import net.haesleinhuepf.clij2.plugins.SumOfAllPixels;
import net.haesleinhuepf.clijx.CLIJx;
import net.haesleinhuepf.clijx.plugins.LocalThresholdBernsen;
import net.haesleinhuepf.clijx.plugins.LocalThresholdPhansalkar;
import tables.DimensionMap;
/**
 * https://www.researchgate.net/publication/329115119_Segmentation_of_Total_Cell_Area_in_Brightfield_Microscopy_Images
 * https://clij.github.io/clij2-docs/reference_simpleITKCannyEdgeDetection
 * https://clij.github.io/clij2-docs/reference_simpleITKZeroCrossingBasedEdgeDetection
 * Enhance contrast
 * @author MMB
 *
 */
public class MotilityTest {
	public static ImageStack stack;
	public static CLIJ2 clij;
	public static CLIJx clijx;
	public static int split = 10;
	
	public static void main(String[] args) {
		//testAll();
		saveAll();
	}
	public static void testAll() {
		clij = CLIJ2.getInstance();
		clijx = CLIJx.getInstance();
		String neutro = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro\\Neutro.avi";
		System.out.println("Neutro");
		test(neutro);
		String med3d = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro+HUVEC\\Neutro+HUVEC-3D.avi";
		System.out.println("Neutro+HUVEC-3D");
		test(med3d);
		String med2d = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro+HUVEC\\Neutro+HUVEC-2D.avi";
		System.out.println("Neutro+HUVEC-2D");
		test(med2d);
		String fast3d = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro+PBMC+HUVEC\\Neutro+PBMC+HUVEC-3D.avi";
		System.out.println("Neutro+PBMC+HUVEC-3D");
		test(fast3d);
		String fast2d = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro+PBMC+HUVEC\\Neutro+PBMC+HUVEC-2D.avi";
		System.out.println("Neutro+PBMC+HUVEC-2D");
		test(fast2d);
//		String med3d = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro+HUVEC\\Neutro+HUVEC-3D001.avi";
//		System.out.println("Neutro+HUVEC-3D001");
//		test(med3d);
//		String med2d = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro+HUVEC\\Neutro+HUVEC-2D001.avi";
//		System.out.println("Neutro+HUVEC-2D001");
//		test(med2d);
//		String fast3d = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro+PBMC+HUVEC\\Neutro+PBMC+HUVEC-3D001.avi";
//		System.out.println("Neutro+PBMC+HUVEC-3D001");
//		test(fast3d);
//		String fast2d = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro+PBMC+HUVEC\\Neutro+PBMC+HUVEC-2D001.avi";
//		System.out.println("Neutro+PBMC+HUVEC-2D001");
//		test(fast2d);
	}
	public static void saveAll() {
		clij = CLIJ2.getInstance();
		clijx = CLIJx.getInstance();
//		String neutro = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro\\Neutro.avi";
//		save(neutro, "Neutro");
//		
//		String med3d001 = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro+HUVEC\\Neutro+HUVEC-3D001.avi";
//		save(med3d001, "Neutro+HUVEC-3D");
		
		String med2d001 = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro+HUVEC\\Neutro+HUVEC-2D001.avi";
		save(med2d001, "Neutro+HUVEC-2D");
		
//		String fast3d001 = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro+PBMC+HUVEC\\Neutro+PBMC+HUVEC-3D001.avi";
//		save(fast3d001, "Neutro+PBMC+HUVEC-3D");
//		
//		String fast2d001 = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro+PBMC+HUVEC\\Neutro+PBMC+HUVEC-2D001.avi";
//		save(fast2d001, "Neutro+PBMC+HUVEC-2D");
	}
	public static void spread(String fileName) {
		stack = getImageStack(fileName);
		long[][] total = new long[split][split];
		for(int i = 2; i<stack.size(); i++) {
			ImageProcessor ip =  stack.getProcessor(i);
			int[][] arr = getArr(ip);
			ImageProcessor ip2 =  stack.getProcessor(i-1).convertToByteProcessor();
			int[][] arr2 = getArr(ip2);
			long[][] ss = sumSplit(abs(difference(arr,arr2)),split);
			for(int r = 0; r<split; r++) {
				for(int c = 0; c<split; c++) total[r][c]+=ss[r][c];
			}
		}
		for(long[] ll : total) System.out.println(Arrays.toString(ll));
	}
	
	public static void save(String source, String fileName) {
		stack = getImageStack(source);
		ImagePlus image1 = (getThresholdedImage(new ImagePlus(fileName+" Thresholded", stack.getProcessor(1).convertToByteProcessor())));
		ImagePlus image2 = (getThresholdedImage(new ImagePlus(fileName+" Thresholded", stack.getProcessor(100).convertToByteProcessor())));
		FileSaver fs = new FileSaver(absoluteDifference(image1,image2));
		fs.saveAsTiff("C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Images\\"+fileName+" Difference1-100.tif");
	}
	/**
	 * Performs tests on movie
	 * @param fileName movie to be processed
	 */
	public static void test(String fileName) {
		stack = getImageStack(fileName);
		long white = 2048*2044-sum(getThresholdedImage(new ImagePlus("",stack.getProcessor(1).convertToByteProcessor())).getProcessor().getIntArray())/255;
		System.out.println("White: "+white);
		int base = 1;
		int length = 10;
		
		
//		long[] total = new long[length];
//		for(int i = 2; i<=length*(stack.size()/length); i++) {
//			
//			if(i%length==1) {
//				
//				base = i;
//			}
//			else total[i%length]+=combined(i,base);
//			//System.out.println((diff(i,base))/1000);
//		}
//		for(long l : total) System.out.println(l*1000/white);
//		
		for(int i = 4; i<=stack.size(); i++) {
			double difference = combined(i-3,i);
			System.out.println((int)(difference*1000/white));
		}
		
	}
	/**
	 * Finds the difference between two frames in video
	 * @param index1 index of first frame
	 * @param index2 index of second frame
	 * @return sum(abs(difference(two frames)))
	 */
	public static long diff(int index1, int index2) {
		ImageProcessor ip =  stack.getProcessor(index1);
		int[][] arr = getArr(ip);
		ImageProcessor ip2 =  stack.getProcessor(index2).convertToByteProcessor();
		int[][] arr2 = getArr(ip2);
		return sum(abs(difference(arr,arr2)),split);
	}
	/**
	 * Gets int array representation of image, possibly with threshold added
	 * @param ip the image to be processed
	 * @return array containing pixel values
	 */
	public static int[][] getArr (ImageProcessor ip){
		ImagePlus imagePlus = new ImagePlus("",ip.convertToByteProcessor());
//		Auto_Local_Threshold thresholder = new Auto_Local_Threshold();
//		imagePlus = (ImagePlus)thresholder.exec(imagePlus,"Median",60,0.0,0.0,true)[0];
//		ip = imagePlus.getProcessor();
		int[][] arr = ip.convertToByteProcessor().getIntArray();
		return arr;
	}
	/**
	 * Sum of elements in array
	 * @param arr1 array to be summed
	 * @return sum
	 */
	public static long sum(int[][] arr1) {
		long total = 0;
		for(int i = 0; i<arr1.length; i++) {
			for(int j = 0; j<arr1[0].length; j++) {
				total+=arr1[i][j];
			}
		}
		return total;
	}
	/**
	 * Adds all elements of an array divided into small sections
	 * @param arr1 array to be summed
	 * @param split How many sections both horizontal and vertical (total number of partitions is split^2)
	 * @return 2D array of sums of each section
	 */
	public static long[][] sumSplit(int[][] arr1, int split) {
		long[][] total = new long[split][split];
		double sizex = arr1.length/(double)split;
		double sizey = arr1.length/(double)split;
		for(int i = 0; i<arr1.length; i++) {
			for(int j = 0; j<arr1[0].length; j++) {
				total[(int) Math.floor(i/sizex)][(int) Math.floor(j/sizey)]+=arr1[i][j];
			}
		}
		return total;
	}
	/**
	 * Finds median of elements in SumSplit
	 * @param arr1 array to be summed
	 * @param split How many sections both horizontal and vertical (total number of partitions is split^2)
	 * @return median of elements in sumSplit
	 */
	public static long sum(int[][] arr1, int split) {
		long[][] total = sumSplit(arr1, split);
		long[] total2 = new long[split*split];
		int count = 0;
		for(int i = 0; i<split; i++) {
			for(int j = 0; j<split; j++) {
				total2[count]=total[i][j];
				count++;
			}
		}
		Arrays.sort(total2);
		return total2[split*split/2];
	}
	/**
	 * Subtracts elements of arr1 from corresponding elements of arr2
	 * @param arr1 array to be subtracted
	 * @param arr2 array to be subtracted from
	 * @return array of all differences
	 */
	public static int[][] difference(int[][] arr1,  int[][] arr2){
		int[][] ret = new int[arr1.length][arr1[0].length];
		for(int i = 0; i<arr1.length; i++) {
			for(int j = 0; j<arr1[0].length; j++) {
				ret[i][j] = arr2[i][j]-arr1[i][j];
			}
		}
		return ret;
	}
	public static ImagePlus absoluteDifference(ImagePlus src1, ImagePlus src2) {
		
		ClearCLBuffer s1 = clij.push(src1);
		ClearCLBuffer s2 = clij.push(src2);
		ClearCLBuffer difference = clij.create(s1);
		clij.absoluteDifference(s1,s2, difference);
		ImagePlus ret = clij.pull(difference);
		s1.close();
		s2.close();
		difference.close();
		return ret;
	}
	
	public static ImagePlus getThresholdedImage(ImagePlus src) {
		CLIJx clijx = CLIJx.getInstance();
		ClearCLBuffer s1 = clij.push(src);
		ClearCLBuffer s1thresholded = clij.create(s1);
		ClearCLBuffer s1despeckle = clij.create(s1);
		//LocalThresholdBernsen.localThresholdBernsen(clijx, s1, s1thresholded, 15, 0);
		LocalThresholdPhansalkar.localThresholdPhansalkar(clijx, s1, s1thresholded, 15, (float)0.15, (float)0.5);
		Median2DBox.median2DBox(clij, s1thresholded, s1despeckle, 1, 1);
		//ImagePlus ret = clij.pull(s1despeckle);
		ClearCLBuffer s1output = clij.create(s1);
		clij.multiplyImageAndScalar(s1despeckle,s1output,255);
		ImagePlus ret= clij.pull(s1output);
		s1.close();
		s1thresholded.close();
		s1despeckle.close();
		s1output.close();
		return ret;
	}
	
	public static double combined(ImagePlus src1, ImagePlus src2) {
		ClearCLBuffer s1 = clij.push(src1);
		ClearCLBuffer s2 = clij.push(src2);
		ClearCLBuffer s1thresholded = clij.create(s1);
		ClearCLBuffer s2thresholded = clij.create(s2);
		ClearCLBuffer s1despeckle = clij.create(s1);
		ClearCLBuffer s2despeckle = clij.create(s2);
//		LocalThresholdBernsen.localThresholdBernsen(clijx, s1, s1thresholded, 15, 0);
//		LocalThresholdBernsen.localThresholdBernsen(clijx, s2, s2thresholded, 15, 0);
		LocalThresholdPhansalkar.localThresholdPhansalkar(clijx, s1, s1thresholded, 15, (float)0.15, (float)0.5);
		LocalThresholdPhansalkar.localThresholdPhansalkar(clijx, s2, s2thresholded, 15, (float)0.15, (float)0.5);
		Median2DSphere.median2DSphere(clij, s1thresholded, s1despeckle, 5, 5);
		Median2DSphere.median2DSphere(clij, s2thresholded, s2despeckle, 5, 5);
		ClearCLBuffer difference = clij.create(s1);
		clij.absoluteDifference(s1despeckle,s2despeckle, difference);
		double ret = SumOfAllPixels.sumOfAllPixels(clij, difference);
		s1.close();
		s2.close();
		s1thresholded.close();
		s2thresholded.close();
		s1despeckle.close();
		s2despeckle.close();
		difference.close();
		return ret;
	}
	
	public static double combined(int first, int second) {
		return combined(new ImagePlus("",stack.getProcessor(first).convertToByteProcessor()),new ImagePlus("",stack.getProcessor(second).convertToByteProcessor()));
	}
	/**
	 * Takes absolute value of all elements of 2D array
	 * @param arr1 2D array
	 * @return 2D array with no negative elemeents
	 */
	public static int[][] abs(int[][] arr1){
		int[][] ret = new int[arr1.length][arr1[0].length];
		for(int i = 0; i<arr1.length; i++) {
			for(int j = 0; j<arr1[0].length; j++) {
				ret[i][j] = Math.abs(arr1[i][j]);
			}
		}
		return ret;
	}
	/**
	 * Gets image stack from movie file location
	 * @param fileName of an AVI file
	 * @return ImageStack of frames of movie
	 */
	public static ImageStack getImageStack(String fileName) {
		// Open an AVIreader
		AVI_Reader movieReader = new AVI_Reader();
		ImageStack stack = movieReader.makeStack(fileName, 1, 0, true, false, false);
		return stack;
	}

}
