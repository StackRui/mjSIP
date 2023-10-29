/*
 * Copyright (C) 2007 Luca Veltri - University of Parma - Italy
 * 
 * This source code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * Author(s):
 * Luca Veltri (luca.veltri@unipr.it)
 */
package org.mjsip.examples;

import java.io.File;

import org.mjsip.media.MediaDesc;
import org.mjsip.sip.address.NameAddress;
import org.mjsip.sip.message.SipMessage;
import org.mjsip.sip.provider.SipConfig;
import org.mjsip.sip.provider.SipOptions;
import org.mjsip.sip.provider.SipProvider;
import org.mjsip.sip.provider.SipStack;
import org.mjsip.time.Scheduler;
import org.mjsip.time.SchedulerConfig;
import org.mjsip.ua.MediaConfig;
import org.mjsip.ua.MultipleUAS;
import org.mjsip.ua.ServiceConfig;
import org.mjsip.ua.ServiceOptions;
import org.mjsip.ua.UAConfig;
import org.mjsip.ua.UserAgent;
import org.mjsip.ua.UserAgentListener;
import org.mjsip.ua.UserAgentListenerAdapter;
import org.mjsip.ua.pool.PortConfig;
import org.mjsip.ua.pool.PortOptions;
import org.mjsip.ua.pool.PortPool;
import org.mjsip.ua.streamer.StreamerFactory;
import org.zoolu.util.Flags;

/** Jukebox is a simple audio server.
  * It automatically responds to incoming calls and sends the audio file
  * as selected by the caller through the request-line parameter 'audiofile'.
  */
public class Jukebox extends MultipleUAS {

	/** URI resource parameter */
	public static String PARAM_RESOURCE="resource";
	
	/** Default available ports */
	public static int MEDIA_PORTS=20;

	/** Maximum life time (call duration) in seconds */
	public static int MAX_LIFE_TIME=600;

	/** Media file path */
	public static String MEDIA_PATH=".";

	private ExampleMediaConfig _mediaConfig;

	private PortPool _portPool;

	/** 
	 * Creates a {@link Jukebox}. 
	 */
	public Jukebox(SipProvider sip_provider, StreamerFactory streamerFactory, UAConfig uaConfig,
			ExampleMediaConfig mediaConfig, PortPool portPool, ServiceOptions serviceConfig) {
		super(sip_provider,streamerFactory, uaConfig, uaConfig, serviceConfig);
		_mediaConfig = mediaConfig;
		_portPool = portPool;
	}
	
	@Override
	protected UserAgentListener createCallHandler(SipMessage msg) {
		return new UserAgentListenerAdapter() {
			private MediaConfig _callMedia;

			/** From UserAgentListener. When a new call is incoming. */
			@Override
			public void onUaIncomingCall(UserAgent ua, NameAddress callee, NameAddress caller, MediaDesc[] media_descs) {
				String audio_file=MEDIA_PATH+"/"+callee.getAddress().getParameter(PARAM_RESOURCE);
				if (audio_file!=null) {
					if (new File(audio_file).isFile()) {
						_mediaConfig.setSendFile(audio_file);
					}
				}
				if (_mediaConfig.getSendFile() != null) {
					_callMedia = MediaConfig.from(_mediaConfig.getMediaDescs());
					_callMedia.allocateMediaPorts(_portPool);
					ua.accept(_callMedia.getMediaDescs());
				} else {
					ua.hangup();
				}
				
				_mediaConfig.allocateMediaPorts(_portPool);
			}
			
			@Override
			public void onUaCallClosed(UserAgent ua) {
				if (_callMedia != null) {
					_callMedia.releaseMediaPorts(_portPool);
				}
			}
		};
	}

	/** The main method. */
	public static void main(String[] args) {
		System.out.println("Jukebox "+SipStack.version);

		int media_ports=MEDIA_PORTS;
		boolean prompt_exit=false;

		for (int i=0; i<args.length; i++) {
			if (args[i].equals("--mports")) {
				try {
					media_ports=Integer.parseInt(args[i+1]);
					args[i]="--skip";
					args[++i]="--skip";
				}
				catch (Exception e) {  e.printStackTrace();  }
			}
			else
			if (args[i].equals("--mpath")) {
				MEDIA_PATH=args[i+1];
				args[i]="--skip";
				args[++i]="--skip";
			}
			else
			if (args[i].equals("--prompt")) {
				prompt_exit=true;
				args[i]="--skip";
			}
		}
		Flags flags=new Flags("Jukebox", args);
		String config_file=flags.getString("-f","<file>", System.getProperty("user.home") + "/.mjsip-ua" ,"loads configuration from the given file");
		SipOptions sipConfig = SipConfig.init(config_file, flags);
		UAConfig uaConfig = UAConfig.init(config_file, flags, sipConfig);
		SchedulerConfig schedulerConfig = SchedulerConfig.init(config_file);
		ExampleMediaConfig mediaConfig = ExampleMediaConfig.init(config_file, flags);
		PortOptions portConfig = PortConfig.init(config_file, flags);
		ServiceOptions serviceConfig=ServiceConfig.init(config_file, flags);         
		flags.close();
		
		PortPool portPool = new PortPool(portConfig.getMediaPort(), portConfig.getPortCount());

		mediaConfig.setAudio(true);
		mediaConfig.setVideo(false);
		uaConfig.setSendOnly(true);
		new Jukebox(new SipProvider(sipConfig, new Scheduler(schedulerConfig)),ExampleStreamerFactory.createStreamerFactory(mediaConfig, uaConfig),uaConfig, mediaConfig, portPool, serviceConfig);
		
		// promt before exit
		if (prompt_exit) 
		try {
			System.out.println("press 'enter' to exit");
			(new java.io.BufferedReader(new java.io.InputStreamReader(System.in))).readLine();
			System.exit(0);
		}
		catch (Exception e) {}
	}    

}