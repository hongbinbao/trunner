package com.tizen.tizenrunner.impl;

import com.tizen.tizenrunner.TizenRunnerOptions;
import com.tizen.tizenrunner.api.IDevice;
import com.tizen.tizenrunner.api.IDeviceStub;

/**
 * Class for implement the IDeviceStub interface on Tizen.
 * @author Bao Hongbin
 */
public class TizenStub implements IDeviceStub {
	private String ipaddr = "127.0.0.1";
	private int stubport = 3490;
	private IDevice tizenDevice;
	
	
	public TizenStub(TizenRunnerOptions options){
    	ipaddr = options.getHost();
    	stubport = options.getPort();
	}
	
	@Override
	public IDevice waitForConnection() {
		tizenDevice = new TizenDeviceImpl(ipaddr,stubport);
		return tizenDevice;
	}

	@Override
	public IDevice waitForConnection(long timeoutMs, String deviceIdRegex) {
		tizenDevice = new TizenDeviceImpl(ipaddr,stubport);
		return tizenDevice;
		
	}

	@Override
	public void shutdown() {
	       if(tizenDevice != null){
	           tizenDevice.dispose();
	       }
	}

}
