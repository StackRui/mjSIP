/*
 * Copyright (C) 2005 Luca Veltri - University of Parma - Italy
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

package org.mjsip.ua.cli;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

import org.mjsip.media.MediaDesc;
import org.mjsip.sip.address.NameAddress;
import org.mjsip.sip.provider.SipProvider;
import org.mjsip.ua.UserAgent;
import org.mjsip.ua.UserAgentListener;
import org.mjsip.ua.UserAgentProfile;
import org.slf4j.LoggerFactory;
import org.zoolu.util.ScheduledWork;


/** Simple command-line-based SIP user agent (UA).
  * It includes audio/video applications.
  * <p>It can use external audio/video tools as media applications.
  * Currently only RAT (Robust Audio Tool) and VIC are supported as external applications.
  */
public class UserAgentCli implements UserAgentListener {
	
	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(UserAgentCli.class);
	
	/** SipProvider. */
	protected SipProvider sip_provider;

	/** User Agent */
	protected UserAgent ua;

	/** UserAgentProfile */
	protected UserAgentProfile ua_profile;
			
	/** Standard input */
	BufferedReader stdin=null; 
			
	/** Standard output */
	PrintStream stdout=null; 

		  
	// ************************* UA internal state *************************
	  
	/** UA_IDLE=0 */
	protected static final String UA_IDLE="IDLE";
	/** UA_INCOMING_CALL=1 */
	protected static final String UA_INCOMING_CALL="INCOMING_CALL";
	/** UA_OUTGOING_CALL=2 */
	protected static final String UA_OUTGOING_CALL="OUTGOING_CALL";
	/** UA_ONCALL=3 */
	protected static final String UA_ONCALL="ONCALL";
	
	/** Call state: <P>UA_IDLE=0, <BR>UA_INCOMING_CALL=1, <BR>UA_OUTGOING_CALL=2, <BR>UA_ONCALL=3 */
	String call_state=UA_IDLE;
	

	/** Changes the call state */
	protected void changeStatus(String state) {
		call_state=state;
		LOG.debug("state: "+call_state); 
	}

	/** Checks the call state */
	protected boolean statusIs(String state) {
		return call_state.equals(state); 
	}

	/** Gets the call state */
	protected String getStatus() {
		return call_state; 
	}


	// *************************** Public methods **************************

	/** Creates a new UA. */
	public UserAgentCli(SipProvider sip_provider, UserAgentProfile ua_profile) {
		this.sip_provider=sip_provider;
		this.ua_profile=ua_profile;
		ua=new UserAgent(sip_provider,ua_profile,this);      
		if (!ua_profile.noPrompt) stdin=new BufferedReader(new InputStreamReader(System.in)); 
		if (!ua_profile.noPrompt) stdout=System.out;
		run();
	}


	/** Becomes ready for receive a new incoming call. */
	public void readyToReceive() {
		LOG.info("WAITING FOR INCOMING CALL");
		if (!ua_profile.audio && !ua_profile.video)
			LOG.info("ONLY SIGNALING, NO MEDIA");
		//ua.listen();
		changeStatus(UA_IDLE);
		LOG.info("digit the callee's URI to make a call or press 'enter' to exit");
	} 


	/** Makes a new call */
	public void call(String target_uri) {
		ua.hangup();
		LOG.info("CALLING " + target_uri);
		LOG.info("calling "+target_uri);
		if (!ua_profile.audio && !ua_profile.video)
			LOG.info("ONLY SIGNALING, NO MEDIA");
		ua.call(target_uri);
		changeStatus(UA_OUTGOING_CALL);
	} 


	/** Accepts an incoming call */
	public void accept() {
		ua.accept();
		changeStatus(UA_ONCALL);
		if (ua_profile.hangupTime>0) automaticHangup(ua_profile.hangupTime); 
		LOG.info("press 'enter' to hangup"); 
	} 


	/** Terminates a call */
	public void hangup() {
		LOG.info("hangup");
		ua.hangup();
		changeStatus(UA_IDLE);
		if (ua_profile.callTo!=null) {
			if (ua_profile.recallTime>0 && (ua_profile.recallCount--)>0) {
				automaticCall(ua_profile.recallTime,ua_profile.callTo.toString());
			} 
			else exit();
		}
		else readyToReceive();
	} 
	

	/** Starts the UA */
	void run() {
		
		try {
			// Set the re-invite
			if (ua_profile.reinviteTime>0) {
				reInvite(ua_profile.reinviteTime);
			}

			// Set the transfer (REFER)
			if (ua_profile.transferTo!=null && ua_profile.transferTime>0) {
				callTransfer(ua_profile.transferTo,ua_profile.transferTime);
			}

			if (ua_profile.doUnregisterAll) {
				// ########## unregisters ALL contact URIs
				LOG.info("UNREGISTER ALL contact URIs");
				ua.unregisterall();
			} 

			if (ua_profile.doUnregister) {
				// unregisters the contact URI
				LOG.info("UNREGISTER the contact URI");
				ua.unregister();
			} 

			if (ua_profile.doRegister) {
				// ########## registers the contact URI with the registrar server
				LOG.info("REGISTRATION");
				ua.loopRegister(ua_profile.expires,ua_profile.expires/2,ua_profile.keepaliveTime);
			}         
			
			if (ua_profile.callTo!=null) {
				// UAC
				call(ua_profile.callTo.toString()); 
				LOG.info("press 'enter' to cancel");
				readLine();
				hangup();
			}
			else {
				// UAS + UAC
				if (ua_profile.acceptTime>=0)
					LOG.info("AUTO ACCEPT MODE");
				readyToReceive();
				while (stdin!=null) {
					String line=readLine();
					if (statusIs(UA_INCOMING_CALL)) {
						if (line.toLowerCase().startsWith("n")) {
							hangup();
						}
						else {
							accept();
						}
					}
					else
					if (statusIs(UA_IDLE)) {
						if (line!=null && line.length()>0) {
							call(line);
						}
						else {
							exit();
						}
					}
					else
					if (statusIs(UA_ONCALL)) {
						hangup();
					}
				}
			}
		}
		catch (Exception e)  {  e.printStackTrace(); System.exit(0);  }
	}


	/** Exits */
	public void exit() {
		try {  Thread.sleep(1000);  } catch (Exception e) {}
		System.exit(0);
	}


	// ******************* UserAgent callback functions ******************

	/** When a new call is incoming */
	@Override
	public void onUaIncomingCall(UserAgent ua, NameAddress callee, NameAddress caller, MediaDesc[] media_descs) {
		if (ua_profile.redirectTo!=null) {
			// redirect the call
			ua.redirect(ua_profile.redirectTo);
			LOG.info("call redirected to "+ua_profile.redirectTo);
		}         
		else
		if (ua_profile.acceptTime>=0) {
			// automatically accept the call
			//accept();
			automaticAccept(ua_profile.acceptTime);
		}
		else          {
			changeStatus(UA_INCOMING_CALL);
			LOG.info("incoming call from "+caller.toString());
			LOG.info("accept? [yes/no]");
		}
	}
	
	/** When an ougoing call is stated to be in progress */
	@Override
	public void onUaCallProgress(UserAgent ua) {
		
	}

	/** When an ougoing call is remotly ringing */
	@Override
	public void onUaCallRinging(UserAgent ua) {
		
	}

	/** When an ougoing call has been accepted */
	@Override
	public void onUaCallAccepted(UserAgent ua) {
		changeStatus(UA_ONCALL);
		LOG.info("call accepted");
		if (ua_profile.hangupTime>0) automaticHangup(ua_profile.hangupTime);
		else
			LOG.info("press 'enter' to hangup");
	}
	
	/** When a call has been transferred */
	@Override
	public void onUaCallTransferred(UserAgent ua) {
		
	}

	/** When an incoming call has been cancelled */
	@Override
	public void onUaCallCancelled(UserAgent ua) {
		readyToReceive();
	}

	/** When an ougoing call has been refused or timeout */
	@Override
	public void onUaCallFailed(UserAgent ua, String reason) {
		if (ua_profile.callTo!=null) exit();
		else readyToReceive();
	}

	/** When a call has been locally or remotely closed */
	@Override
	public void onUaCallClosed(UserAgent ua) {
		if (ua_profile.callTo!=null) exit();
		else readyToReceive();     
	}

	/** When a new media session is started. */
	@Override
	public void onUaMediaSessionStarted(UserAgent ua, String type, String codec) {
		//printLog(type+" started "+codec);
	}

	/** When a media session is stopped. */
	@Override
	public void onUaMediaSessionStopped(UserAgent ua, String type) {
		//log(type+" stopped");
	}


	/** When registration succeeded. */
	@Override
	public void onUaRegistrationSucceeded(UserAgent ua, String result) {
		LOG.info("Registration succeeded: "+result); 
	}

	/** When registration failed. */
	@Override
	public void onUaRegistrationFailed(UserAgent ua, String result) {
		LOG.error("Registration failed: "+result); 
	}
	

	// ************************ scheduled events ************************

	/** Schedules a re-inviting after <i>delay_time</i> secs. It simply changes the contact address. */
	/*void reInvite(final NameAddress contact, final int delay_time) {
		new ScheduledWork(delay_time*1000L) {
			public void doWork() {
				log("AUTOMATIC RE-INVITING/MODIFING");
				ua.modify(contact,null);
			}
		};
	}*/
	/** Schedules a re-inviting after <i>delay_time</i> secs. It simply changes the contact address. */
	void reInvite(final int delay_time) {
		LOG.info("AUTOMATIC RE-INVITING/MODIFING: "+delay_time+" secs"); 
		if (delay_time==0) ua.modify(null);
		else new ScheduledWork(delay_time*1000L) {  @Override
		public void doWork() {  ua.modify(null);  }  };
	}


	/** Schedules a call-transfer after <i>delay_time</i> secs. */
	/*void callTransfer(final NameAddress transfer_to, final int delay_time) {
		new ScheduledWork(delay_time*1000L) {
			public void doWork() {
				printLog("AUTOMATIC REFER/TRANSFER");
				ua.transfer(transfer_to);
			}
		};
	}*/
	/** Schedules a call-transfer after <i>delay_time</i> secs. */
	void callTransfer(final NameAddress transfer_to, final int delay_time) {
		LOG.info("AUTOMATIC REFER/TRANSFER: "+delay_time+" secs");
		if (delay_time==0) ua.transfer(transfer_to);
		else new ScheduledWork(delay_time*1000L) {  @Override
		public void doWork() {  ua.transfer(transfer_to);  }  };
	}

	/** Schedules an automatic answer after <i>delay_time</i> secs. */
	/*void automaticAccept(final int delay_time) {
		new ScheduledWork(delay_time*1000L) {
			public void doWork() {
				log("AUTOMATIC ANSWER");
				accept();
			}
		};
	}*/
	/** Schedules an automatic answer after <i>delay_time</i> secs. */
	void automaticAccept(final int delay_time) {
		LOG.info("AUTOMATIC ANSWER: "+delay_time+" secs");
		if (delay_time==0) accept();
		else new ScheduledWork(delay_time*1000L) {  @Override
		public void doWork() {  accept();  }  };
	}

	/** Schedules an automatic hangup after <i>delay_time</i> secs. */
	/*void automaticHangup(final int delay_time) {
		new ScheduledWork(delay_time*1000L) {
			public void doWork() {
				log("AUTOMATIC HANGUP");
				hangup();
			}
		};
	}*/
	/** Schedules an automatic hangup after <i>delay_time</i> secs. */
	void automaticHangup(final int delay_time) {
		LOG.info("AUTOMATIC HANGUP: "+delay_time+" secs");
		if (delay_time==0) hangup();
		else new ScheduledWork(delay_time*1000L) {  @Override
		public void doWork() {  hangup();  }  };
	}
	
	/** Schedules an automatic re-call after <i>delay_time</i> secs. */
	void automaticCall(final int delay_time, final String remote_uri) {
		LOG.info("AUTOMATIC RE-CALL: "+delay_time+" secs");
		if (delay_time==0) call(remote_uri);
		else new ScheduledWork(delay_time*1000L) {  @Override
		public void doWork() {  call(remote_uri);  }  };
	}


	// ******************************* Logs ******************************

	/** Read a new line from stantard input. */
	protected String readLine() {
		try { if (stdin!=null) return stdin.readLine(); } catch (IOException e) {}
		return null;
	}

}