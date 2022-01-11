package function.motility;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;

import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileSaver;
import ij.plugin.AVI_Reader;
import ij.plugin.ContrastEnhancer;
import ij.process.ImageProcessor;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.Maximum2DSphere;
import net.haesleinhuepf.clij2.plugins.Median2DBox;
import net.haesleinhuepf.clij2.plugins.Median2DSphere;
import net.haesleinhuepf.clij2.plugins.SumOfAllPixels;
import net.haesleinhuepf.clijx.CLIJx;
import net.haesleinhuepf.clijx.plugins.LocalThresholdPhansalkar;
import net.haesleinhuepf.clijx.simpleitk.SimpleITKCannyEdgeDetection;
/**
 * Take some region bigger than radius of cell. If it has variance below some threshold, label it as sstrong background. 
 * sobel edge detection
 * @author MMB
 *
 */
public class MotilityTest{
	public static ImageStack stack;
	public static CLIJ2 clij;
	public static CLIJx clijx;
	public static int split = 10;
	public static int gap = 5;
	//public static String baseFile = "D:\\Experiments\\2021-11-16 Under-oil Neutrophils R29 (EasySep-Healthy (ID105-2)-Neutrophils vs NeutrophilsPBMCs vs Neutrophils HUVEC vs Neutrophils+PBMCs+HUVEC)\\Neutrophil Plate";
	public static String baseFile = "D:\\Experiments\\2021-11-16 Under-oil Neutrophils R29 (EasySep-Healthy (ID105-2)-Neutrophils vs NeutrophilsPBMCs vs Neutrophils HUVEC vs Neutrophils+PBMCs+HUVEC)";
	public static String destFile = "C:\\\\Users\\\\MMB\\\\Desktop\\\\Joseph Cai\\\\Neutrophil Motility Analysis\\FinalResults.csv";
	
	public static void main(String[] args) throws IOException {
		
		getFiles();
		//testAll();
		//saveAll();
	}
	
	public static void getFiles() throws IOException {
		clij = CLIJ2.getInstance();
		clijx = CLIJx.getInstance();
		FileWriter writer = new FileWriter(new File(destFile));
		int total = FileUtils.listFiles(new File(baseFile), new String[] {"avi"}, true).size();
		int count = 0;
		System.out.println(total);
		for(File f : FileUtils.listFiles(new File(baseFile), new String[] {"avi"}, true)) {
			count++;
			System.out.println(count+" of "+total);
			System.out.println(f.getAbsolutePath().substring(160));
			save(f.getAbsolutePath(),f.getParentFile().getParentFile().getName()+", "+f.getParentFile().getName()+", "+f.getName());
			double motility = motilityIndex(f.getAbsolutePath());
			System.out.println(motility);
			writer.write(f.getParentFile().getParentFile().getParentFile().getName()+", "+f.getParentFile().getParentFile().getName()+", "+f.getParentFile().getName()+", "+f.getName()+", "+motility+"\n");
		}
		
		
		writer.close();
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
		String med3d001 = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro+HUVEC\\Neutro+HUVEC-3D001.avi";
		System.out.println("Neutro+HUVEC-3D001");
		test(med3d001);
		String med2d001 = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro+HUVEC\\Neutro+HUVEC-2D001.avi";
		System.out.println("Neutro+HUVEC-2D001");
		test(med2d001);
		String fast3d001 = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro+PBMC+HUVEC\\Neutro+PBMC+HUVEC-3D001.avi";
		System.out.println("Neutro+PBMC+HUVEC-3D001");
		test(fast3d001);
		String fast2d001 = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro+PBMC+HUVEC\\Neutro+PBMC+HUVEC-2D001.avi";
		System.out.println("Neutro+PBMC+HUVEC-2D001");
		test(fast2d001);
	}
	public static void saveAll() {
		clij = CLIJ2.getInstance();
		clijx = CLIJx.getInstance();
		String neutro = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro\\Neutro.avi";
		save(neutro, "Neutro");
		
		String med3d = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro+HUVEC\\Neutro+HUVEC-3D.avi";
		save(med3d, "Neutro+HUVEC-3D");
		String med2d = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro+HUVEC\\Neutro+HUVEC-2D.avi";
		save(med2d, "Neutro+HUVEC-2D");
		String fast3d = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro+PBMC+HUVEC\\Neutro+PBMC+HUVEC-3D.avi";
		save(fast3d, "Neutro+PBMC+HUVEC-3D");
		String fast2d = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro+PBMC+HUVEC\\Neutro+PBMC+HUVEC-2D.avi";
		save(fast2d, "Neutro+PBMC+HUVEC-2D");
		String med3d001 = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro+HUVEC\\Neutro+HUVEC-3D001.avi";
		save(med3d001, "Neutro+HUVEC-3D001");
		
		String med2d001 = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro+HUVEC\\Neutro+HUVEC-2D001.avi";
		save(med2d001, "Neutro+HUVEC-2D001");
		
		String fast3d001 = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro+PBMC+HUVEC\\Neutro+PBMC+HUVEC-3D001.avi";
		save(fast3d001, "Neutro+PBMC+HUVEC-3D001");
		
		String fast2d001 = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro+PBMC+HUVEC\\Neutro+PBMC+HUVEC-2D001.avi";
		save(fast2d001, "Neutro+PBMC+HUVEC-2D001");
	}
	
	
	public static void save(String source, String fileName) {
		stack = getImageStack(source);
		ImagePlus image1 = (getThresholdedImage(new ImagePlus(fileName+" Thresholded", stack.getProcessor(1).convertToByteProcessor())));
		FileSaver fs = new FileSaver(image1);
		fs.saveAsTiff("C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Thresholded Images\\"+fileName+" Phansalkar.tif");
	}
	/**
	 * Performs tests on movie
	 * @param fileName movie to be processed
	 */
	public static double motilityIndex(String fileName) {
		stack = getImageStack(fileName);
		long white = 2048*2044-sum(getThresholdedImage(new ImagePlus("",stack.getProcessor(1).convertToByteProcessor())).getProcessor().getIntArray())/255;
		System.out.println("White: "+white);
//		int base = 1;
//		int length = 10;
//		
//		
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
		double total = 0;
		int count = 0;
		for(int i = 2*gap+1; i<=stack.size(); i++) {
			double difference = combined(i-2*gap,i)-combined(i-gap,i);
			total+=(difference/10000);
			count++;
			System.out.println(count);
		}
		return total/count;
		
	}
	public static double motilityIndexCanny(String fileName) {
		stack = getImageStack(fileName);
		long white = 2048*2044-sum(getCannyThresholdedImage(new ImagePlus("",stack.getProcessor(1).convertToByteProcessor())).getProcessor().getIntArray())/255;
		System.out.println("White: "+white);
//		int base = 1;
//		int length = 10;
//		
//		
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
		double total = 0;
		int count = 0;
		for(int i = gap+1; i<=stack.size(); i++) {
			double difference = cannyCombined(i-gap,i);
			total+=(int)(difference*4/white);
			count++;
		}
		return total/count;
		
	}
	
	public static void test(String fileName) {
		stack = getImageStack(fileName);
		long white = 2048*2044-sum(getThresholdedImage(new ImagePlus("",stack.getProcessor(1).convertToByteProcessor())).getProcessor().getIntArray())/255;
//		int base = 1;
//		int length = 10;
//		
//		
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
		
		for(int i = 4; i<=stack.size(); i++) {
			double difference = combined(i-3,i);
			System.out.println((int)(difference*4/white));
		}
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
		ClearCLBuffer s1 = getThresholdedBuffer(src);
		ImagePlus ret = clij.pull(s1);
		s1.close();
		return ret;
	}
	
	public static ClearCLBuffer getThresholdedBuffer(ImagePlus src) {
		ClearCLBuffer s1 = clij.push(src);
		ClearCLBuffer s1smooth = clij.create(s1);
		ClearCLBuffer s1thresholded = clij.create(s1);
		ClearCLBuffer s1despeckle = clij.create(s1);
		//LocalThresholdBernsen.localThresholdBernsen(clijx, s1smooth, s1thresholded, 15, 0);
		clij.gaussianBlur2D(s1, s1smooth, 3, 3);
		LocalThresholdPhansalkar.localThresholdPhansalkar(clijx, s1smooth, s1thresholded, 15, (float)0.1, (float)0.4);
		Median2DBox.median2DBox(clij, s1thresholded, s1despeckle, 5, 5);
		//ImagePlus ret = clij.pull(s1despeckle);
		ClearCLBuffer s1output = clij.create(s1);
		clij.multiplyImageAndScalar(s1despeckle,s1output,255);
		s1.close();
		s1smooth.close();
		s1thresholded.close();
		s1despeckle.close();
		return s1output;
	}
	
	
	public static ImagePlus getCannyThresholdedImage(ImagePlus src) {
		ClearCLBuffer s1 = getCannyThresholdedBuffer(src);
		ImagePlus ret = clij.pull(s1);
		s1.close();
		return ret;
	}
	
	public static ClearCLBuffer getCannyThresholdedBuffer(ImagePlus src) {
		ContrastEnhancer ce = new ContrastEnhancer();
		ce.setNormalize(true);
		ce.equalize(src);
		//CLIJx clijx = CLIJx.getInstance();
		ClearCLBuffer s1 = clij.push(src);
		ClearCLBuffer s1thresholded = clij.create(s1);
		ClearCLBuffer s1despeckle = clij.create(s1);
		ClearCLBuffer s1output = clij.create(s1);
		ClearCLBuffer s1notted = clij.create(s1);
		//SimpleITKZeroCrossingBasedEdgeDetection.simpleITKZeroCrossingBasedEdgeDetection(clij, s1, s1thresholded, (float) 10,(float) 0.9);
		SimpleITKCannyEdgeDetection.simpleITKCannyEdgeDetection(clij, s1, s1thresholded, (float)0.1, (float)8,(float) 0.1,(float) 0.5);
		//clij.binaryNot(s1thresholded,s1notted);
		Maximum2DSphere.maximum2DSphere(clij, s1thresholded, s1despeckle, 1, 1);
		//Maximum2DSphere.maximum2DSphere(clij, s1despeckle, s1notted, 1, 1);
		Median2DSphere.median2DSphere(clij, s1despeckle, s1notted, 1, 1);
		Median2DSphere.median2DSphere(clij, s1notted, s1despeckle, 1, 1);
		//Maximum2DSphere.maximum2DSphere(clij, s1despeckle, s1notted, 1, 1);
		//Minimum2DSphere.minimum2DSphere(clij, s1notted, s1despeckle, 10, 10);
		clij.binaryNot(s1despeckle,s1notted);
		clij.multiplyImageAndScalar(s1notted,s1output,255);
		s1.close();
		s1thresholded.close();
		s1despeckle.close();
		s1notted.close();
		return s1output;
	}
	
	public static double combined(ImagePlus src1, ImagePlus src2) {
		ClearCLBuffer s1 = getThresholdedBuffer(src1);
		ClearCLBuffer s2 = getThresholdedBuffer(src2);
		ClearCLBuffer difference = clij.create(s1);
		clij.absoluteDifference(s1,s2, difference);
		double ret = SumOfAllPixels.sumOfAllPixels(clij, difference);
		s1.close();
		s2.close();
		difference.close();
		return ret;
	}
	
	public static double cannyCombined(ImagePlus src1, ImagePlus src2) {
		ClearCLBuffer s1 = getCannyThresholdedBuffer(src1);
		ClearCLBuffer s2 = getCannyThresholdedBuffer(src2);
		ClearCLBuffer difference = clij.create(s1);
		clij.absoluteDifference(s1,s2, difference);
		double ret = SumOfAllPixels.sumOfAllPixels(clij, difference);
		s1.close();
		s2.close();
		difference.close();
		return ret;
	}
	
	public static double cannyCombined(int first, int second) {
		return cannyCombined(new ImagePlus("",stack.getProcessor(first).convertToByteProcessor()),new ImagePlus("",stack.getProcessor(second).convertToByteProcessor()));
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
//		ImagePlus imagePlus = new ImagePlus("",ip.convertToByteProcessor());
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
	/**
	 * Gets image stack from movie file location
	 * @param fileName of an AVI file
	 * @return ImageStack of frames of movie
	 */
	public static ImageStack getImageStack(String fileName) {
		AVI_Reader movieReader = new AVI_Reader();
		ImageStack stack = movieReader.makeStack(fileName, 1, 0, true, false, false);
		return stack;
	}


}
