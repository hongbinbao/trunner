package com.tizen.tizenrunner.impl;

import java.net.Socket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.tizen.tizenrunner.ConnectionManager;
import com.tizen.tizenrunner.api.IDevice;
import com.tizen.tizenrunner.api.ISnapshot;

/**
 * Connection library for setting up connection between host and device daemon.
 * @author Bao Hongbin
 *
 */
public class TizenDeviceImpl implements IDevice {
	private static final String FORWARD_SDB[] = {"sdb","forward", "tcp:3490","tcp:3490"};
	private static final String START_DAEMON_SDB[] = {"sdb", "shell","GASClient","/dev/input/event0","/dev/input/event1"};	
	///may need future
	//private static final String START_SDB[] = {"sdb","start-server"};
	//private static final String STOP_SDB[] = {"sdb","kill-server"};
	//private static final String TIZEN_DEBUG_BRIDGE = "sdb";
	//private static final String COMMAND_STOP_SDBSERVER = "sdb kill-server";
	//private static final String COMMAND_STOP_STARTSERVER = "start-server";
	//private static final String COMMAND_STOP_SDBFORWARD = "sdb forward tcp:3490 tcp:3490";
	//private static final String COMMAND_INVOKE_DAEMON = "sdb shell GASClient /dev/input/event0 /dev/input/event1";
	//wait for 30s timeout
	private static final long MANAGER_CREATE_TIMEOUT_MS = 30 * 1000;
	private static final long MANAGER_CREATE_WAIT_MS = 3 * 1000;
    private static final String DEFAULT_DAEMON_ADDRESS = "127.0.0.1"; 
    private ConnectionManager mConnectionManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    
	public TizenDeviceImpl(String ip,int port){
		mConnectionManager = createConnectionManager(ip, port);
	}
    
	/**
	 * wait for the connection with device.
	 * @param address device daemon host
	 * @param port device daemon port
	 * @return The instance of ConnectionManager
	 */
	private Socket daemonSocket;
	private ConnectionManager createConnectionManager(String address, int port) {
		InetAddress addr;
        try {
            addr = InetAddress.getByName(address);
        } catch (UnknownHostException e) {
        	e.printStackTrace();
            return null;
        }
		invokeDaemon(address);
        boolean success = false;
        ConnectionManager connectionManager = null;
        long start = System.currentTimeMillis();
        while (!success){
        	//waitForConnection(MANAGER_CREATE_WAIT_MS);
            long now = System.currentTimeMillis();
            long diff = now - start;
            if (diff > MANAGER_CREATE_TIMEOUT_MS) {
                return null;
            }
            //Socket daemonSocket;
        	System.out.println("waiting................");
            try {
            	daemonSocket = new Socket(addr, port);
            } catch (IOException e) {
        	    success = false;
                continue;
            }
            
            try {
            	connectionManager = new ConnectionManager(daemonSocket);
            } catch (IOException e) {
            	success = false;
                continue;
            }
            try{
            	connectionManager.gotServerResponse();
            }catch(Exception e){
            	success = false;
            	continue;
            }
            success = true;
        }
        return connectionManager;
	}

	
	@Override
	public boolean touch(int x, int y) {
		try {
			return mConnectionManager.touch(x, y);
		} catch (IOException e) {
        	System.out.println("touch error!");
        	try {
				daemonSocket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
		return false;
	}
	
	@Override
	public boolean press(String key) {
		try {
			return mConnectionManager.press(key);
		} catch (IOException e) {
        	System.out.println("press key error!");
        	try {
				daemonSocket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public ISnapshot takeSnapshot() {
		try {
			return new TizenSnapshot(mConnectionManager.takeSnapshot("TSC"));
		} catch (IOException e) {
        	System.out.println("take snapshot error!");
        	try {
				daemonSocket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.printStackTrace();
			return null;
		}
	}
	
    @Override
    public void dispose() {
        try {
        	mConnectionManager.done();
        	mConnectionManager.quit();
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }

    public void invokeDaemon(String addr){
      	if(DEFAULT_DAEMON_ADDRESS.equals(addr)){
            if(!forwardSdb()){
            	System.exit(0);
            }
      	}
      	startDeviceDaemmon(START_DAEMON_SDB);
    }
    
    synchronized boolean forwardSdb(){
    	Process process = null;
        int status = -1;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(FORWARD_SDB);
			process = processBuilder.start();
			status = process.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			if(process!=null){
				process.destroy();
			}
		}
		if(status != 0){
			return false;
		}
		return true;
    }
    
    private void asyncGrabProcessOutput(final Process process) throws InterruptedException {
        Thread errorThread = new Thread("errorstream") {
            @Override
            public void run() {
                // create a buffer to read the stderr output
            	InputStream error = process.getErrorStream();
                InputStreamReader errorStream = new InputStreamReader(error);
                BufferedReader errorReader = new BufferedReader(errorStream);

                try {
                    while (true) {
                        String line = errorReader.readLine();
                        if (line != null) {
                        	line = null;
                        } else {
                            break;
                        }
                    }
                } catch (Exception e) {
                }finally{
                	try {
						error.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                }
            }
        };

        Thread inputThread = new Thread("inputStream") {
            @Override
            public void run() {
            	InputStream input = process.getInputStream();
                InputStreamReader inputStream = new InputStreamReader(input);
                BufferedReader inputStreamReader = new BufferedReader(inputStream);

                try {
                    while (true) {
                        String line = inputStreamReader.readLine();
                        if (line != null) {
                        	line = null;
                        } else {
                            break;
                        }
                    }
                } catch (Exception e) {

                }finally{
                	try {
						input.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
                }
            }
        };
        errorThread.setDaemon(true);
        inputThread.setDaemon(true);
        errorThread.start();
        inputThread.start();
    }
    
    private static String getGASClientId(){
    	Process process = null;
        try {
			process = Runtime.getRuntime().exec("sdb shell pgrep GASClient");
		} catch (IOException e) {
			System.out.println("sdb pgrep error");
			e.printStackTrace();
		}
        InputStreamReader is = new InputStreamReader(process.getInputStream());
        BufferedReader errReader = new BufferedReader(is);
        StringBuffer sb = null;
        try {
        	sb = new StringBuffer();
            while (true) {
                String line = errReader.readLine();
                if (line != null) {
                    sb.append(line);
                } else {
                    break;
                }
            }
        } catch (IOException e) {
        	return "null";
        }finally{
        	try {
				errReader.close();
	        	is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
        	if(process!=null){
        		process.destroy();
        	}
        }
        if("".equals(sb.toString().trim())){
        	return "null";
        }else{
            return sb.toString();	
        }    
    }

    
    private void startDeviceDaemmon(final String[] commands){
    	String pid = getGASClientId();
    	if(!"null".equals(pid)){
    	    try {
				Runtime.getRuntime().exec("sdb shell kill -9 " + pid);
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
        executor.submit(new Runnable() {
            @Override
            public void run() {
            	Process process = null;
                try {
                    ProcessBuilder processBuilder = new ProcessBuilder(commands);
                	processBuilder.redirectErrorStream();
                    process = processBuilder.start();
                   	InputStream input = process.getInputStream();
                    InputStreamReader inputStream = new InputStreamReader(input);
                    BufferedReader inputReader = new BufferedReader(inputStream);
                    try {
                        while (true) {
                            String line = inputReader.readLine();
                            if (line != null) {
                            	System.out.println("ling>>>");
        						System.out.println(line);
                            	line = null;
                            } else {
                            	System.out.println("bbbbbbbbbbbbbbbbbbbbbbbbb111");
                                break;
                            }
                        }
                    } catch (Exception e) {
                    }finally{
                    	try {
    						input.close();
    						System.out.println("close1111111111111111111111111111");
    					} catch (IOException e) {
    						System.out.println("bbbbbbbbbbbbbbbbbbbbbbbbb222");
    						e.printStackTrace();
    					}
                    }                    
                	
                } catch (Exception e) {
                	System.out.println("Exception during start GASClient on device.");
                    throw new RuntimeException(e);
                }finally{
                	//if re-connect auto. need to close previous process to avoid memory leak.
                	if(process!=null){
                		System.out.println("dddddddddddddddd");
                		process.destroy();
                	}
                	
                	
                }
              }
            });
    }
    
    private void startDeviceDaemmon1(final String[] commands){
    	String pid = getGASClientId();
    	if(!"null".equals(pid)){
    	    try {
				Runtime.getRuntime().exec("sdb shell kill -9 " + pid);
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
        executor.submit(new Runnable() {
            @Override
            public void run() {
            	Process process = null;
                try {
                    ProcessBuilder processBuilder = new ProcessBuilder(commands);
                    process = processBuilder.start();
                	asyncGrabProcessOutput(process);
                } catch (Exception e) {
                	System.out.println("Exception during start GASClient on device.");
                    throw new RuntimeException(e);
                }finally{
                	//if re-connect auto. need to close previous process to avoid memory leak.
                	if(process!=null){
                		process.destroy();
                	}
                	if( process.getInputStream()!=null){
                		try {
							process.getInputStream().close();
						} catch (IOException e) {
							System.out.println("11111111111");
							e.printStackTrace();
						}          		
                	}
                	if( process.getOutputStream()!=null){
                		try {
							process.getOutputStream().close();
						} catch (IOException e) {
							System.out.println("222222222222");
							e.printStackTrace();
						}          		
                	}
                	if( process.getErrorStream()!=null){
                		try {
							process.getErrorStream().close();
						} catch (IOException e) {
							System.out.println("33333333333");
							e.printStackTrace();
						}          		
                	}
                	
                }
              }
            });
        try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    }
    
    private void waitForConnection(long time){
        try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	
    }  
    
}
