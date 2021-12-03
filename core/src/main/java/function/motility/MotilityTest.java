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
		
		String fast3d = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro+PBMC+HUVEC\\Neutro+PBMC+HUVEC-3D.avi";
		System.out.println("Neutro+PBMC+HUVEC-3D");
		test(fast3d);
		
		String fast2d = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis\\Neutro+PBMC+HUVEC\\Neutro+PBMC+HUVEC-2D.avi";
		System.out.println("Neutro+PBMC+HUVEC-2D");
		test(fast2d);
		
	}
	public static void test(String fileName) {
		stack = getImageStack(fileName);
		System.out.println(stack.getSize());
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
		for(int i = 2; i<=stack.size(); i++) {
			System.out.println(diff(i,1)/10000);
		}
	}
	public static long diff(int index1, int index2) {
		ImageProcessor ip =  stack.getProcessor(index1);
		int[][] arr = getArr(ip);
		ImageProcessor ip2 =  stack.getProcessor(index2).convertToByteProcessor();
		int[][] arr2 = getArr(ip2);
		return sum(abs(difference(arr,arr2)),10);
	}
	public static int[][] getArr (ImageProcessor ip){
		ImagePlus imagePlus = new ImagePlus("",ip.convertToByteProcessor());
		Auto_Local_Threshold thresholder = new Auto_Local_Threshold();
		imagePlus = (ImagePlus)thresholder.exec(imagePlus,"Median",60,0.0,0.0,true)[0];
		ip = imagePlus.getProcessor();
		int[][] arr = ip.convertToByteProcessor().getIntArray();
		return arr;
	}
	public static long sum(int[][] arr1) {
		long total = 0;
		for(int i = 0; i<arr1.length; i++) {
			for(int j = 0; j<arr1[0].length; j++) {
				total+=arr1[i][j];
			}
		}
		return total;
	}
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
	public static int[][] difference(int[][] arr1,  int[][] arr2){
		int[][] ret = new int[arr1.length][arr1[0].length];
		for(int i = 0; i<arr1.length; i++) {
			for(int j = 0; j<arr1[0].length; j++) {
				ret[i][j] = arr2[i][j]-arr1[i][j];
			}
		}
		return ret;
	}
	public static int[][] abs(int[][] arr1){
		int[][] ret = new int[arr1.length][arr1[0].length];
		for(int i = 0; i<arr1.length; i++) {
			for(int j = 0; j<arr1[0].length; j++) {
				ret[i][j] = Math.abs(arr1[i][j]);
			}
		}
		return ret;
	}
	public static ImageStack getImageStack(String fileName) {
		// Open an AVIreader
		AVI_Reader movieReader = new AVI_Reader();
		ImageStack stack = movieReader.makeStack(fileName, 1, 0, true, false, false);
		return stack;
	}

}
