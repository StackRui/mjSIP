package org.mjsip.ua;


import org.mjsip.media.FlowSpec.Direction;
import org.mjsip.sip.address.GenericURI;
import org.mjsip.sip.address.NameAddress;
import org.mjsip.sip.address.SipURI;
import org.mjsip.sip.address.UnexpectedUriSchemeException;
import org.mjsip.sip.provider.SipProvider;
import org.zoolu.util.Configure;
import org.zoolu.util.Flags;
import org.zoolu.util.Parser;


/** {@link UserAgent} configuration options.
  */
public class UAConfig extends Configure {
		 
	/** 
	 * Constructs a {@link UAConfig} from the given configuration file and program arguments.
	 */
	public static UAConfig init(String file, Flags flags) {
		UAConfig result=new UAConfig();
		result.loadFile(file);
		result.updateWith(flags);
		result.normalize();
		return result;
	}

	/** 
	 * Constructs a {@link UAConfig} from configuration file values.
	 */
	public static UAConfig init(String file) {
		UAConfig result=new UAConfig();
		result.loadFile(file);
		result.normalize();
		return result;
	}

	/** Display name for the user.
	  * It is used in the user's AOR registered to the registrar server
	  * and used as From URI. */
	public String displayName=null;

	/** User's name.
	  * It is used to build the user's AOR registered to the registrar server
	  * and used as From URI. */
	public String user=null;

	/** Fully qualified domain name (or address) of the proxy server.
	  * It is part of the user's AOR registered to the registrar server
	  * and used as From URI.
	  * <p>
	  * If <i>proxy</i> is not defined, the <i>registrar</i> value is used in its place.
	  * <p>
	  * If <i>registrar</i> is not defined, the <i>proxy</i> value is used in its place. */
	public String proxy=null;

	/** Fully qualified domain name (or address) of the registrar server.
	  * It is used as recipient for REGISTER requests.
	  * <p>
	  * If <i>registrar</i> is not defined, the <i>proxy</i> value is used in its place.
	  * <p>
	  * If <i>proxy</i> is not defined, the <i>registrar</i> value is used in its place. */
	public String registrar=null;

	/** UA address.
	  * It is the SIP address of the UA and is used to form the From URI if no proxy is configured. */
	public String ua_address=null;

	/** User's name used for server authentication. */
	public String authUser=null;
	/** User's realm used for server authentication. */
	public String authRealm=null;
	/** User's passwd used for server authentication. */
	public String authPasswd=null;

	/** Relative path of UA media resources (gif, wav, etc.) within the UA jar file or within the resources folder. 
	  * By default, the folder "media/org/mjsip/ua" is used. */
	public String mediaPath="media/org/mjsip/ua";

	/** Absolute path (or complete URI) of the buddy list file where the buddy list is and loaded from (and saved to).
	  * By default, the file "buddy.lst" is used. */
	public String buddyListFile="buddy.lst";

	/** Whether registering with the registrar server */
	public boolean doRegister=false;

	/** Expires time (in seconds). */
	public int expires=3600;

	/** Rate of keep-alive tokens (datagrams) sent toward the outbound proxy
	  * (if present) or toward the registrar server.
	  * Its value specifies the delta-time (in millesconds) between two
	  * keep-alive tokens. <br>
	  * Set keepalive_time=0 for not sending keep-alive datagrams. */
	public long keepaliveTime=0;

	/** Automatic call a remote user secified by the 'call_to' value.
	  * Use value 'NONE' for manual calls (or let it undefined).  */
	public NameAddress callTo=null;
			
	/** Response time in seconds; it is the maximum time the user can wait before responding to an incoming call; after such time the call is automatically declined (refused). */
	public int refuseTime=20;
	/** Automatic answer time in seconds; time&lt;0 corresponds to manual answer mode. */
	public int acceptTime=-1;        

	/** Automatic call transfer time in seconds; time&lt;0 corresponds to no auto transfer mode. */
	public int transferTime=-1;
	
	/** Automatic re-inviting time in seconds; time&lt;0 corresponds to no auto re-invite mode.  */
	public int reinviteTime=-1;
	
	/** Automatic re-call time in seconds; time&lt;0 corresponds to no auto re-call mode.  */
	public int recallTime=-1;
	
	/** Number of successive automatic re-calls; it is used only if call_to!=null, re_call_time&gt;0, and re_call_count&gt;0.  */
	public int recallCount=-1;

	/** Redirect incoming call to the secified URI.
	  * Use value 'NONE' for not redirecting incoming calls (or let it undefined). */
	public NameAddress redirectTo=null;

	/** Transfer calls to the secified URI.
	  * Use value 'NONE' for not transferring calls (or let it undefined). */
	public NameAddress transferTo=null;

	/** No offer in the invite */
	public boolean noOffer=false;
	/** Do not use system audio  */
	public boolean noSystemAudio=false;
	/** Do not use prompt */
	public boolean noPrompt=false;

	/** Whether playing in receive only mode */
	public boolean recvOnly=false;
	/** Whether playing in send only mode */
	public boolean sendOnly=false;
	
	/** Media address (use it if you want to use a media address different from the via address) */
	public String mediaAddr=null;

	/** Whether using symmetric_rtp */
	public boolean symmetricRtp=false;

	// ******************** undocumented parametes ********************

	/** Whether running the UAS (User Agent Server), or acting just as UAC (User Agent Client). In the latter case only outgoing calls are supported. */
	public boolean uaServer=true;

	/** Whether running an Options Server, that automatically responds to OPTIONS requests. */
	public boolean optionsServer=true;

	/** Whether running an Null Server, that automatically responds to not-implemented requests. */
	public boolean nullServer=true;

	/** Sender synchronization adjustment, that is the time (in milliseconds) that a frame
	  * should be sent in advance by the RTP sender, before the nominal time.
	  * A value less that 0 means no re-synchronization explicitely performed by the RTP sender.
	  * <p>
	  * Note that when using audio capturing, synchronization with the sample rate
	  * is implicitely performed by the audio capture device and frames are read at constat bit rate.
	  * However, a value of this parameter >=0 (explicit re-synchronization) is suggested
	  * in order to let the read() method be non-blocking (in the other case
	  * the UA audio performances seem to decrease). */
	//public int javax_sound_sync_adj=2;

	/** Whether enforcing time synchronization to RTP source stream.
	  * If synchronization is explicitely performed, the depature time of each RTP packet is equal to its nominal time.
	  * <p>
	  * Note that when using audio capturing, synchronization with the sample rate
	  * is implicitely performed by the audio capture device and frames are read at constat bit rate.
	  * However, an explicit re-synchronization is suggested
	  * in order to let the read() method be non-blocking (in the other case
	  * the UA audio performance seems decreasing. */
	public boolean javaxSoundSync=true;

	/** Receiver random early drop (RED) rate. Actually it is the inverse of packet drop rate.
	  * It can used to prevent long play back delay. 
	  * A value less or equal to 0 means that no packet dropping is explicitely
	  * performed at the RTP receiver. */
	public int randomEarlyDropRate=20;

	/** Constructs a {@link UAConfig} */
	private UAConfig() {
		super();
	}

	/** 
	 * The flow direction.
	 */
	public Direction getDirection() {
		if (recvOnly) {
			return Direction.RECV_ONLY;
		} else if (sendOnly) {
			return Direction.SEND_ONLY;
		} else {
			return Direction.FULL_DUPLEX;
		}
	}

	/** Inits the UserAgentProfile. */
	private void normalize() {
		if (proxy!=null && proxy.equalsIgnoreCase(Configure.NONE)) proxy=null;
		if (registrar!=null && registrar.equalsIgnoreCase(Configure.NONE)) registrar=null;
		if (displayName!=null && displayName.equalsIgnoreCase(Configure.NONE)) displayName=null;
		if (user!=null && user.equalsIgnoreCase(Configure.NONE)) user=null;
		if (authRealm!=null && authRealm.equalsIgnoreCase(Configure.NONE)) authRealm=null;

		setUnconfiguredAttributes(null);
	}


	// ************************ public methods ************************

	/** Gets the user's AOR (Address Of Record) registered to the registrar server
	  * and used as From URI.
	  * <p>
	  * In case of <i>proxy</i> and <i>user</i> parameters have been defined
	  * it is formed as "<i>display_name</i>" &lt;sip:<i>user</i>@<i>proxy</i>&gt;,
	  * otherwhise the local UA address (obtained by the SipProvider) is used.
	  * @return the user's name address */
	public NameAddress getUserURI() {
		if (proxy!=null && user!=null) return new NameAddress(displayName,new SipURI(user,proxy));
		else return new NameAddress(displayName,new SipURI(user,ua_address));
	}

	/** Sets the user's AOR (Address Of Record) registered to the registrar server
	  * and used as From URI.
	  * <p>
	  * It actually sets the <i>display_name</i>, <i>user</i>, and <i>proxy</i> parameters.
	  * <p>
	  * If <i>registrar</i> is not defined, the <i>proxy</i> value is used in its place.
	  * @param naddr the user's name address formed by the user's display name (optional) and URI */
	public void setUserURI(NameAddress naddr) {
		GenericURI naddr_uri=naddr.getAddress();
		if (!naddr_uri.isSipURI()) throw new UnexpectedUriSchemeException(naddr_uri.getScheme());
		// else
		SipURI uri=new SipURI(naddr_uri);
		if (displayName==null) displayName=naddr.getDisplayName();
		if (user==null) user=uri.getUserName();
		if (proxy==null) proxy=(uri.hasPort())? uri.getHost()+":"+uri.getPort() : uri.getHost();
		if (registrar==null) registrar=proxy;
	}

	/** Sets server and authentication attributes (if not already done).
	  * It actually sets <i>ua_address</i>, <i>registrar</i>, <i>proxy</i>, <i>auth_realm</i>,
	  * and <i>auth_user</i> attributes.
	  * <p>
	  * Note: this method sets such attributes only if they haven't still been initilized.
	  * @param sip_provider the SIP provider used for initializing these attributes */
	public void setUnconfiguredAttributes(SipProvider sip_provider) {
		if (registrar==null && proxy!=null) registrar=proxy;
		if (proxy==null && registrar!=null) proxy=registrar;
		if (authRealm==null && proxy!=null) authRealm=proxy;
		if (authRealm==null && registrar!=null) authRealm=registrar;
		if (authUser==null && user!=null) authUser=user;
		if (ua_address==null && sip_provider!=null) {
			ua_address=sip_provider.getViaAddress();
			if (sip_provider.getPort()!=sip_provider.sipConfig().getDefaultPort()) ua_address+=":"+sip_provider.getPort();
		}
	}

	// *********************** protected methods **********************

	/** Parses a single line (loaded from the config file)
	 * @param attribute The name of the option.
	 * @param par The {@link Parser} delivering the option value.
	  */
	@Override
	public void setOption(String attribute, Parser par) {
		if (attribute.equals("display_name"))   {  displayName=par.getRemainingString().trim();  return;  }
		if (attribute.equals("user"))           {  user=par.getString();  return;  }
		if (attribute.equals("proxy"))          {  proxy=par.getString();  return;  }
		if (attribute.equals("registrar"))      {  registrar=par.getString();  return;  }

		if (attribute.equals("auth_user"))      {  authUser=par.getString();  return;  } 
		if (attribute.equals("auth_realm"))     {  authRealm=par.getRemainingString().trim();  return;  }
		if (attribute.equals("auth_passwd"))    {  authPasswd=par.getRemainingString().trim();  return;  }

		if (attribute.equals("media_path"))     {  mediaPath=par.getStringUnquoted();  return;  }      
		if (attribute.equals("buddy_list_file")){  buddyListFile=par.getStringUnquoted();  return;  }      

		if (attribute.equals("do_register"))    {  doRegister=(par.getString().toLowerCase().startsWith("y"));  return;  }
		if (attribute.equals("expires"))        {  expires=par.getInt();  return;  } 
		if (attribute.equals("keepalive_time")) {  keepaliveTime=par.getInt();  return;  } 

		if (attribute.equals("call_to")) {
			String naddr=par.getRemainingString().trim();
			if (naddr==null || naddr.length()==0 || naddr.equalsIgnoreCase(Configure.NONE)) callTo=null;
			else callTo=NameAddress.parse(naddr);
			return;
		}
		if (attribute.equals("redirect_to")) {
			String naddr=par.getRemainingString().trim();
			if (naddr==null || naddr.length()==0 || naddr.equalsIgnoreCase(Configure.NONE)) redirectTo=null;
			else redirectTo=NameAddress.parse(naddr);
			return;
		}
		if (attribute.equals("transfer_to")) {
			String naddr=par.getRemainingString().trim();
			if (naddr==null || naddr.length()==0 || naddr.equalsIgnoreCase(Configure.NONE)) transferTo=null;
			else transferTo=NameAddress.parse(naddr);
			return;
		}
		if (attribute.equals("refuse_time"))    {  refuseTime=par.getInt();  return;  }
		if (attribute.equals("accept_time"))    {  acceptTime=par.getInt();  return;  }
		if (attribute.equals("transfer_time"))  {  transferTime=par.getInt();  return;  } 
		if (attribute.equals("re_invite_time")) {  reinviteTime=par.getInt();  return;  } 
		if (attribute.equals("re_call_time"))   {  recallTime=par.getInt();  return;  } 
		if (attribute.equals("re_call_count"))  {  recallCount=par.getInt();  return;  } 
		if (attribute.equals("no_offer"))       {  noOffer=(par.getString().toLowerCase().startsWith("y"));  return;  }
		if (attribute.equals("no_system_audio")){  noSystemAudio=(par.getString().toLowerCase().startsWith("y"));  return;  }
		if (attribute.equals("no_prompt"))      {  noPrompt=(par.getString().toLowerCase().startsWith("y"));  return;  }

		if (attribute.equals("recv_only"))      {  recvOnly=(par.getString().toLowerCase().startsWith("y"));  return;  }
		if (attribute.equals("send_only"))      {  sendOnly=(par.getString().toLowerCase().startsWith("y"));  return;  }
		
		if (attribute.equals("media_addr"))     {  mediaAddr=par.getString();  return;  } 
		if (attribute.equals("symmetric_rtp"))  {  symmetricRtp=(par.getString().toLowerCase().startsWith("y"));  return;  } 

		if (attribute.equals("ua_server")) {  uaServer=(par.getString().toLowerCase().startsWith("y"));  return;  }
		if (attribute.equals("options_server")) {  optionsServer=(par.getString().toLowerCase().startsWith("y"));  return;  }
		if (attribute.equals("null_server")) {  nullServer=(par.getString().toLowerCase().startsWith("y"));  return;  }
		if (attribute.equals("javax_sound_sync")) {  javaxSoundSync=(par.getString().toLowerCase().startsWith("y"));  return;  }
		if (attribute.equals("random_early_drop_rate")) {  randomEarlyDropRate=par.getInt();  return;  }
	}

	/**
	 * Adds settings read from command line arguments.
	 */
	protected void updateWith(Flags flags) {
		Boolean no_prompt=flags.getBoolean("--no-prompt",null,"do not prompt");
		if (no_prompt!=null) this.noPrompt=no_prompt.booleanValue();
		
		Boolean no_system_audio=flags.getBoolean("--no-audio",null,"do not use system audio");
		if (no_system_audio!=null) this.noSystemAudio=no_system_audio.booleanValue();
		
		int regist_time=flags.getInteger("-g","<time>",-1,"registers the contact address with the registrar server for a gven duration, in seconds");
		if (regist_time>=0) {  this.doRegister=true;  this.expires=regist_time;  }
		
		long keepalive_time=flags.getLong("--keep-alive","<msecs>",-1,"send keep-alive packets each given milliseconds");
		if (keepalive_time>=0) this.keepaliveTime=keepalive_time;
		
		Boolean no_offer=flags.getBoolean("-n",null,"no offer in invite (offer/answer in 2xx/ack)");
		if (no_offer!=null) this.noOffer=no_offer.booleanValue();
		
		String call_to=flags.getString("-c","<call_to>",null,"calls a remote user");      
		if (call_to!=null) this.callTo=NameAddress.parse(call_to);
		
		String redirect_to=flags.getString("-r","<uri>",null,"redirects the call to new user <uri>");
		if (redirect_to!=null) this.redirectTo=NameAddress.parse(redirect_to);
		
		String[] transfer=flags.getStringTuple("-q",2,"<uri> <secs>",null,"transfers the call to <uri> after <secs> seconds");
		
		String transfer_to=transfer!=null? transfer[0] : null; 
		if (transfer_to!=null) this.transferTo=NameAddress.parse(transfer_to);
		
		int transfer_time=transfer!=null? Integer.parseInt(transfer[1]) : -1;
		if (transfer_time>0) this.transferTime=transfer_time;
		
		int accept_time=flags.getInteger("-y","<secs>",-1,"auto answers after given seconds");      
		if (accept_time>=0) this.acceptTime=accept_time;
		
		int re_invite_time=flags.getInteger("-i","<secs>",-1,"re-invites after given seconds");
		if (re_invite_time>0) this.reinviteTime=re_invite_time;
		
		int re_call_time=flags.getInteger("--re-call-time","<time>",-1,"re-calls after given seconds");
		if (re_call_time>0) this.recallTime=re_call_time;
		
		int re_call_count=flags.getInteger("--re-call-count","<n>",-1,"number of successive automatic re-calls");
		if (re_call_count>0) this.recallCount=re_call_count;
		
		String display_name=flags.getString("--display-name","<str>",null,"display name");
		if (display_name!=null) this.displayName=display_name;
		
		String user=flags.getString("--user","<user>",null,"user name");
		if (user!=null) this.user=user;
		
		String proxy=flags.getString("--proxy","<proxy>",null,"proxy server");
		if (proxy!=null) this.proxy=proxy;
		
		String registrar=flags.getString("--registrar","<registrar>",null,"registrar server");
		if (registrar!=null) this.registrar=registrar;
		
		String auth_user=flags.getString("--auth-user","<user>",null,"user name used for authenticat");
		if (auth_user!=null) this.authUser=auth_user;
		
		String auth_realm=flags.getString("--auth-realm","<realm>",null,"realm used for authentication");
		if (auth_realm!=null) this.authRealm=auth_realm;
		
		String auth_passwd=flags.getString("--auth-passwd","<passwd>",null,"passwd used for authentication");
		if (auth_passwd!=null) this.authPasswd=auth_passwd; 

		Boolean recv_only=flags.getBoolean("--recv-only",null,"receive only mode, no media is sent");
		if (recv_only!=null) this.recvOnly=recv_only.booleanValue();
		
		Boolean send_only=flags.getBoolean("--send-only",null,"send only mode, no media is received");
		if (send_only!=null) this.sendOnly=send_only.booleanValue();
		
		// for backward compatibility
		String from_uri=flags.getString("--from-uri","<uri>",null,"user's address-of-record (AOR)");
		if (from_uri!=null) this.setUserURI(NameAddress.parse(from_uri));
	}

}
