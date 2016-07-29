package com.jmt.fpsdemo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.util.Log;

public class FileOperate {
	private static final String TAG = "FileOperate";
	
	public static void writeToFile(File file, byte[] DataBuf) throws IOException {		
		FileOutputStream outputStream = new FileOutputStream(file);
		DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
		dataOutputStream.write(DataBuf);
		dataOutputStream.flush();
		dataOutputStream.close();		
	}
	
	public static ArrayList<File> getFileList(File file) {
		ArrayList<File> result = new ArrayList<File>();
		if (!file.isDirectory()) {
			Log.i(TAG, "File path:  "+file.getAbsolutePath());
		} else {
			File[] directoryList = file.listFiles(new FileFilter() {
				public boolean accept (File file) {
					if (file.isFile()) {
						return true;
					} else {
						return false;
					}
				}
			});
			   
			for (int i = 0; i < directoryList.length; i++){ 
				result.add(directoryList[i]);
			}
		}
		return result;
	}
	
	public static byte[] getContentByLocalFile (File path) throws IOException{
		FileInputStream inputStream = new FileInputStream(path);
		DataInputStream datainputStream = new DataInputStream(inputStream);
		byte[] DataBuf = new byte[datainputStream.available()]; 
		datainputStream.read(DataBuf);
		datainputStream.close();
		
		return DataBuf;   
	}
	
	public static void copyFolder(String oldPath, String newPath) {
		try {
			(new File(newPath)).mkdirs(); 
			File a = new File(oldPath);
			String[] file = a.list();
			File temp = null;
			
			for (int i = 0; i < file.length; i++) {
				if (oldPath.endsWith(File.separator)) {
					temp = new File(oldPath + file[i]);
				} else {
					temp = new File(oldPath + File.separator + file[i]);
				}

				if (temp.isFile()) {
					FileInputStream input = new FileInputStream(temp);
					FileOutputStream output = new FileOutputStream(newPath + "/" + (temp.getName()).toString());
					byte[] b = new byte[1024 * 5];
					int len;
					while ((len = input.read(b)) != -1) {
						output.write(b, 0, len);
					}
					output.flush();
					output.close();
					input.close();
				}
				if (temp.isDirectory()) {
					copyFolder(oldPath + "/" + file[i], newPath + "/" + file[i]);
				}
			}
		} catch (Exception e) {
			Log.i (TAG, "Copy folder error");
			e.printStackTrace();
		}
	}
}