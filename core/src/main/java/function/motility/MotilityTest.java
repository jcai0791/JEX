package function.motility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import fiji.threshold.Auto_Local_Threshold;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.AVI_Reader;
import ij.process.ImageProcessor;
import movieio.jmf.JMF_MovieReaderSimple;
import tables.DimensionMap;

public class MotilityTest {
	public static ImageStack stack;
	public static int threshold;
	public static int split = 10;
	public static void main(String[] args) {
		String neutro = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro\\Neutro.avi";
		System.out.println("Neutro");
		test(neutro);
		
		String med3d = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro+HUVEC\\Neutro+HUVEC-3D.avi";
		System.out.println("Neutro+HUVEC-3D");
		test(med3d);
		
		String med2d = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro+HUVEC\\Neutro+HUVEC-2D.avi";
		System.out.println("Neutro+HUVEC-2D");
		test(med2d);
		
		String fast3d = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro+PBMC+HUVEC\\Neutro+PBMC+HUVEC-3D001.avi";
		System.out.println("Neutro+PBMC+HUVEC-3D");
		spread(fast3d);
		
		String fast2d = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro+PBMC+HUVEC\\Neutro+PBMC+HUVEC-2D.avi";
		System.out.println("Neutro+PBMC+HUVEC-2D");
		test(fast2d);
		
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
	 * Performs tests on movie
	 * @param fileName movie to be processed
	 */
	public static void test(String fileName) {
		stack = getImageStack(fileName);
		//System.out.println(stack.getSize());
		threshold = stack.getProcessor(1).convertToByteProcessor().getAutoThreshold();
		
//		ImageProcessor previp =  stack.getProcessor(1).convertToByteProcessor();
//		previp.autoThreshold();
//		int[][] prev = previp.getIntArray();
//		
//		for(int i = 2; i<stack.size(); i++) {
//			ImageProcessor ip =  stack.getProcessor(i).convertToByteProcessor();
//			ip.autoThreshold();
//			int[][] arr = ip.getIntArray();
//			System.out.println(sum(abs(difference(arr,prev))));
//			prev= arr;
//		}
		int base = 1;
		long[] total = new long[20];
		for(int i = 2; i<=20*(stack.size()/20); i++) {
			total[i%20]+=diff(i,base);
			if(i%20==1) {
				
				base = i;
			}
			//System.out.println((diff(i,base))/1000);
		}
		System.out.println(Arrays.toString(total));
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
