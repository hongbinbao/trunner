package com.tizen.tizenrunner.impl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.tizen.tizenrunner.api.ISnapshot;

/**
 * Class used to represent snapshot image from tizen device.
 * @author Bao Hongbin
 */
public class TizenSnapshot implements ISnapshot {
	private final byte[] image;

    /**
     * Create a new instance of TizenSnapshot.
     * @param image the image data from device daemon.
     */
	TizenSnapshot(byte[] data) {
        this.image = data;
    }

	/**
	 * Write the snapshot to a local file.
	 */
    @Override
    public boolean writeToFile(String path, String format) {
        File myfile = null;
        BufferedOutputStream fileOut = null;
        try {
            myfile = new File(path);
            myfile.delete();
            try{
                fileOut = new BufferedOutputStream(new FileOutputStream(myfile));
			    fileOut.write(image);
            }catch(FileNotFoundException e1){
            	e1.printStackTrace();
            }finally{
	            fileOut.flush();
	        	if(fileOut != null){
				    fileOut.close();
	        	}
            }
		} catch (IOException e) {
			e.printStackTrace();
		}

        return true;
    }

}