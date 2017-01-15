package com.cw.litenote.note;

import com.cw.litenote.R;
import com.cw.litenote.db.DB_page;
import com.cw.litenote.main.Page;
import com.cw.litenote.util.UilCommon;
import com.cw.litenote.util.audio.AudioPlayer;
import com.cw.litenote.util.audio.UtilAudio;
import com.cw.litenote.util.image.AsyncTaskAudioBitmap;
import com.cw.litenote.util.image.TouchImageView;
import com.cw.litenote.util.image.UtilImage;
import com.cw.litenote.util.video.UtilVideo;
import com.cw.litenote.util.video.VideoViewCustom;
import com.cw.litenote.util.ColorSet;
import com.cw.litenote.util.CustomWebView;
import com.cw.litenote.util.Util;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.Layout.Alignment;
import android.text.style.AlignmentSpan;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

class Note_adapter extends FragmentStatePagerAdapter
{
	static int mLastPosition;
	private static LayoutInflater inflater;
	private FragmentActivity act;
	private static String mWebTitle;
	private ViewPager pager;
	DB_page db_page;

    Note_adapter(ViewPager viewPager, FragmentActivity activity)
    {
    	super(activity.getFragmentManager());
		pager = viewPager;
    	act = activity;
        inflater = act.getLayoutInflater();
        mLastPosition = -1;
		db_page = new DB_page(act,Util.getPref_lastTimeView_page_tableId(act));
        System.out.println("Note_adapter / constructor / mLastPosition = " + mLastPosition);
    }
    
	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		container.removeView((View) object);
	}

    @SuppressLint("SetJavaScriptEnabled")
	@Override
	public Object instantiateItem(ViewGroup container, final int position) 
    {
    	System.out.println("Note_adapter / instantiateItem / position = " + position);
    	// Inflate the layout containing 
    	// 1. picture group: image,video, thumb nail, control buttons
    	// 2. text group: title, body, time 
    	View pagerView = inflater.inflate(R.layout.note_view_adapter, container, false);
    	int style = Note.getStyle();
        pagerView.setBackgroundColor(ColorSet.mBG_ColorArray[style]);

    	// Picture group
        ViewGroup pictureGroup = (ViewGroup) pagerView.findViewById(R.id.pictureContent);
        String tagPictureStr = "current"+ position +"pictureView";
        pictureGroup.setTag(tagPictureStr);
    	
        // image view
    	TouchImageView imageView = ((TouchImageView) pagerView.findViewById(R.id.image_view));
        String tagImageStr = "current"+ position +"imageView";
        imageView.setTag(tagImageStr);

		// video view
    	VideoViewCustom videoView = ((VideoViewCustom) pagerView.findViewById(R.id.video_view));
        String tagVideoStr = "current"+ position +"videoView";
        videoView.setTag(tagVideoStr);

		ProgressBar spinner = (ProgressBar) pagerView.findViewById(R.id.loading);

        // link web view
		CustomWebView linkWebView = ((CustomWebView) pagerView.findViewById(R.id.link_web_view));
        String tagStr = "current"+position+"linkWebView";
        linkWebView.setTag(tagStr);

        // line view
        View line_view = pagerView.findViewById(R.id.line_view);

    	// text group
        ViewGroup textGroup = (ViewGroup) pagerView.findViewById(R.id.textGroup);

        // Set tag for text web view
    	CustomWebView textWebView = ((CustomWebView) pagerView.findViewById(R.id.textGroup).findViewById(R.id.textBody));
        tagStr = "current"+position+"textWebView";
        textWebView.setTag(tagStr);

		// set text web view
        setWebView(textWebView,spinner,CustomWebView.TEXT_VIEW);

        String linkUri = db_page.getNoteLinkUri(position,true);
        String strTitle = db_page.getNoteTitle(position,true);
        String strBody = db_page.getNoteBody(position,true);

        // View mode
    	// picture only
	  	if(Note.isPictureMode())
	  	{
			System.out.println("Note_adapter / _instantiateItem / isPictureMode ");
	  		pictureGroup.setVisibility(View.VISIBLE);
	  	    showPictureView(position,imageView,videoView,linkWebView,spinner);

	  	    line_view.setVisibility(View.GONE);
	  	    textGroup.setVisibility(View.GONE);
	  	}
	    // text only
	  	else if(Note.isTextMode())
	  	{
			System.out.println("Note_adapter / _instantiateItem / isTextMode ");
	  		pictureGroup.setVisibility(View.GONE);

	  		line_view.setVisibility(View.VISIBLE);
	  		textGroup.setVisibility(View.VISIBLE);

	  	    if( Util.isYouTubeLink(linkUri) ||
	 	  	   !Util.isEmptyString(strTitle)||
	 	  	   !Util.isEmptyString(strBody)   )
	  	    {
	  	    	showTextWebView(position,textWebView);
	  	    }
	  	}
  		// picture and text
	  	else if(Note.isViewAllMode())
	  	{
			System.out.println("Note_adapter / _instantiateItem / isViewAllMode ");

			// picture
			pictureGroup.setVisibility(View.VISIBLE);
	  	    showPictureView(position,imageView,videoView,linkWebView,spinner);

	  	    line_view.setVisibility(View.VISIBLE);
	  	    textGroup.setVisibility(View.VISIBLE);

			// text
	  	    if( Util.isYouTubeLink(linkUri) ||
			!Util.isEmptyString(strTitle)||
	  	       !Util.isEmptyString(strBody)   )
	  	    {
	  	    	showTextWebView(position,textWebView);
	  	    }
	  	}

		// footer of note view
		TextView footerText = (TextView) pagerView.findViewById(R.id.note_view_footer);
		if(!Note.isPictureMode())
		{
			footerText.setVisibility(View.VISIBLE);
			footerText.setText(String.valueOf(position+1)+"/"+ pager.getAdapter().getCount());
            footerText.setTextColor(ColorSet.mText_ColorArray[Note.mStyle]);
            footerText.setBackgroundColor(ColorSet.mBG_ColorArray[Note.mStyle]);
		}
		else
			footerText.setVisibility(View.GONE);

    	container.addView(pagerView, 0);
    	
		return pagerView;			
    } //instantiateItem
	
    // show text web view
    private void showTextWebView(int position,CustomWebView textWebView)
    {
    	System.out.println("Note_adapter/ _showTextView / position = " + position);

    	int viewPort;
    	// load text view data
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
			viewPort = VIEW_PORT_BY_DEVICE_WIDTH;
    	else
    		viewPort = VIEW_PORT_BY_NONE;

    	String strHtml;
		strHtml = getHtmlStringWithViewPort(position,viewPort);
		textWebView.loadData(strHtml,"text/html; charset=utf-8", "UTF-8");
    }
    
    // show picture view
    private void showPictureView(int position,
    		             TouchImageView imageView,
    		             VideoView videoView,
    		             CustomWebView linkWebView,
    		             ProgressBar spinner          )
    {
		String linkUri = db_page.getNoteLinkUri(position,true);
		String pictureUri = db_page.getNotePictureUri(position,true);
    	String audioUri = db_page.getNoteAudioUri(position,true);

    	// Check if Uri is for YouTube
    	if(Util.isEmptyString(pictureUri) && Util.isYouTubeLink(linkUri) )
    	{
			pictureUri = "http://img.youtube.com/vi/"+Util.getYoutubeId(linkUri)+"/0.jpg";
			System.out.println("Note_adapter / _showPictureView / YouTube pictureUri = " + pictureUri);
		}

        // show image view
  		if( UtilImage.hasImageExtension(pictureUri, act)||
  		    (Util.isEmptyString(pictureUri)&& 
  		     Util.isEmptyString(audioUri)&& 
  		     Util.isEmptyString(linkUri)      )             ) // for wrong path icon
  		{
			System.out.println("Note_adapter / _showPictureView / show image view");
  			videoView.setVisibility(View.GONE);
  			linkWebView.setVisibility(View.GONE);
  			UtilVideo.mVideoView = null;
  			imageView.setVisibility(View.VISIBLE);
  			showImageByTouchImageView(spinner, imageView, pictureUri);
  		}
  		// show video view
  		else if(UtilVideo.hasVideoExtension(pictureUri, act))
  		{
			System.out.println("Note_adapter / _showPictureView / show video view");
  			linkWebView.setVisibility(View.GONE);
  			imageView.setVisibility(View.GONE);
  			videoView.setVisibility(View.VISIBLE);
  		}
  		// show audio thumb nail view
  		else if(Util.isEmptyString(pictureUri)&& 
  				!Util.isEmptyString(audioUri)    )
  		{
			System.out.println("Note_adapter / _showPictureView / show audio thumb nail view");
  			videoView.setVisibility(View.GONE);
  			UtilVideo.mVideoView = null;
  			linkWebView.setVisibility(View.GONE);
  			imageView.setVisibility(View.VISIBLE);
  			try
			{
			    AsyncTaskAudioBitmap audioAsyncTask;
			    audioAsyncTask = new AsyncTaskAudioBitmap(Page.mAct,
						    							  audioUri, 
						    							  imageView,
						    							  null, //??? set this has unknown bug
														  false);
				audioAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"Searching media ...");
			}
			catch(Exception e)
			{
				System.out.println("Note_adapter / _AsyncTaskAudioBitmap / exception");
			}
  		}
  		// show link thumb view
  		else if(Util.isEmptyString(pictureUri)&&
  				Util.isEmptyString(audioUri)  &&
  				!Util.isEmptyString(linkUri))
  		{
			System.out.println("Note_adapter / _showPictureView / show link thumb view");
  			videoView.setVisibility(View.GONE);
  			UtilVideo.mVideoView = null;
  			imageView.setVisibility(View.GONE);	
  			linkWebView.setVisibility(View.VISIBLE);
  		}
		else
			System.out.println("Note_adapter / _showPictureView / show none");
    }
    
    
    // Add for FragmentStatePagerAdapter
    @Override
	public Fragment getItem(int arg0) {
        return null;
	}
    
    // Add for calling mPagerAdapter.notifyDataSetChanged() 
    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }
    
	@Override
    public int getCount() 
    {
		if(db_page != null)
			return db_page.getNotesCount(true);
		else
			return 0;
    }

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view.equals(object);
	}
	
	static Intent mIntentView;
    static Note_UI picUI;

	@Override
	public void setPrimaryItem(final ViewGroup container, int position, Object object) 
	{
		// set primary item only
	    if(mLastPosition != position)
		{
			System.out.println("Note_adapter / _setPrimaryItem / mLastPosition = " + mLastPosition);
            System.out.println("Note_adapter / _setPrimaryItem / position = " + position);

			String lastPictureStr = null;
			String lastLinkUri = null;
			String lastAudioUri = null;

			if(mLastPosition != -1)
			{
				lastPictureStr = db_page.getNotePictureUri(mLastPosition,true);
				lastLinkUri = db_page.getNoteLinkUri(mLastPosition, true);
				lastAudioUri = db_page.getNoteAudioUri(mLastPosition, true);
			}

			String pictureStr = db_page.getNotePictureUri(position,true);
			String linkUri = db_page.getNoteLinkUri(position,true);
			String audioUri = db_page.getNoteAudioUri(position,true);

			// remove last text web view
			if (!Note.isPictureMode())
			{
				String tag = "current" + mLastPosition + "textWebView";
				CustomWebView textWebView = (CustomWebView) pager.findViewWithTag(tag);
				if (textWebView != null) {
					textWebView.onPause();
					textWebView.onResume();
				}
			}

			// for web view
			if (!UtilImage.hasImageExtension(pictureStr, act) &&
				!UtilVideo.hasVideoExtension(pictureStr, act)   )
			{
				// remove last link web view
				if(	!UtilImage.hasImageExtension(lastPictureStr, act) &&
					!UtilVideo.hasVideoExtension(lastPictureStr, act) &&
					!UtilAudio.hasAudioExtension(lastAudioUri)         &&
					!Util.isYouTubeLink(lastLinkUri)                      )
				{
					String tag = "current" + mLastPosition + "linkWebView";
					CustomWebView lastLinkWebView = (CustomWebView) pager.findViewWithTag(tag);

					if (lastLinkWebView != null)
					{
						CustomWebView.pauseWebView(lastLinkWebView);
						CustomWebView.blankWebView(lastLinkWebView);//??? if last page has image/video, this line will blank it
					}
				}

				// set current link web view in case no picture Uri
				if (  Util.isEmptyString(pictureStr) &&
					 !Util.isYouTubeLink(linkUri) &&
					  linkUri.startsWith("http") &&
					 !Note.isTextMode()      )
				{
					if(Note.isViewAllMode() )
					{
						String tagStr = "current" + position + "linkWebView";
						CustomWebView linkWebView = (CustomWebView) pager.findViewWithTag(tagStr);
						linkWebView.setVisibility(View.VISIBLE);
                        setWebView(linkWebView,object,CustomWebView.LINK_VIEW);
						System.out.println("Note_adapter / _setPrimaryItem / load linkUri = " + linkUri);
						linkWebView.loadUrl(linkUri);

						//Add for non-stop showing of full screen web view
						linkWebView.setWebViewClient(new WebViewClient() {
							@Override
							public boolean shouldOverrideUrlLoading(WebView view, String url)
							{
								view.loadUrl(url);
								return true;
							}
						});

					}
					else if(Note.isPictureMode())
					{
                        Intent i = new Intent(Intent.ACTION_VIEW,Uri.parse(linkUri));
						act.startActivity(i);
                    }
				}
			}

			// for video view
			if (!Note.isTextMode() )
			{

				// stop last video view running
				if (mLastPosition != -1)
				{
					String tagVideoStr = "current" + mLastPosition + "videoView";
					VideoViewCustom lastVideoView = (VideoViewCustom) pager.findViewWithTag(tagVideoStr);
					lastVideoView.stopPlayback();
				}

                // Show picture view UI
                if (Note.isViewAllMode() || Note.isPictureMode() )
                {
                    if(picUI != null)
                    {
						picUI.handler.removeCallbacks(picUI.runnable);
                        picUI = null;
                    }

					picUI = new Note_UI(act, pager, position);
					picUI.tempShow_picViewUI(5002, pictureStr);//1st touch to turn on UI
                }

				// Set video view
				if ( UtilVideo.hasVideoExtension(pictureStr, act) &&
					 !UtilImage.hasImageExtension(pictureStr, act)   )
				{
					// update current pager view
					UtilVideo.mCurrentPagerView = (View) object;

					// for view mode change
					if (Note.mIsViewModeChanged && (Note.mPlayVideoPositionOfInstance == 0) )
					{
						UtilVideo.mPlayVideoPosition = Note.mPositionOfChangeView;
						UtilVideo.setVideoViewLayout(pictureStr);

                        if (!UtilVideo.hasMediaControlWidget) {
							Note_UI.updateVideoPlayButtonState(pager, Note.mCurrentPosition);
                            picUI.tempShow_picViewUI(5004,pictureStr);
                        }

						if (UtilVideo.mPlayVideoPosition > 0)
							UtilVideo.playOrPauseVideo(pager,pictureStr);
					}
					else
					{
						// for key protect
						if (Note.mPlayVideoPositionOfInstance > 0)
						{
							UtilVideo.setVideoState(UtilVideo.VIDEO_AT_PAUSE);
							UtilVideo.setVideoViewLayout(pictureStr);

							if (!UtilVideo.hasMediaControlWidget) {
								Note_UI.updateVideoPlayButtonState(pager, Note.mCurrentPosition);
                                picUI.tempShow_picViewUI(5003,pictureStr); // for 3 seconds, _showPictureViewUI
                            }

							UtilVideo.playOrPauseVideo(pager,pictureStr);
						}
						else
						{
							if (UtilVideo.hasMediaControlWidget)
								UtilVideo.setVideoState(UtilVideo.VIDEO_AT_PLAY);
							else
								UtilVideo.setVideoState(UtilVideo.VIDEO_AT_STOP);

							UtilVideo.mPlayVideoPosition = 0; // make sure play video position is 0 after page is changed
							UtilVideo.initVideoView(pager,pictureStr, act, position);
						}
					}

					UtilVideo.currentPicturePath = pictureStr;
				}
			}

            ViewGroup audioBlock = (ViewGroup) act.findViewById(R.id.audioGroup);
            audioBlock.setVisibility(View.VISIBLE);
			// init audio block of pager
			if(UtilAudio.hasAudioExtension(audioUri))
			{
				Note.initAudioProgress(act,audioUri);

				if(AudioPlayer.getPlayMode() == AudioPlayer.ONE_TIME_MODE)
				{
					if (AudioPlayer.getPlayState() != AudioPlayer.PLAYER_AT_STOP)
						Note.updateAudioProgress(act);
				}

				Note.updateAudioPlayState(act);
			}
			else
				audioBlock.setVisibility(View.GONE);
		}
	    mLastPosition = position;
	    
	} //setPrimaryItem		

	// Set web view
    private static boolean bWebViewIsShown;
	private void setWebView(final CustomWebView webView,Object object, int whichView)
	{
        final SharedPreferences pref_web_view = act.getSharedPreferences("web_view", 0);
		final ProgressBar spinner = (ProgressBar) ((View)object).findViewById(R.id.loading);
        if( whichView == CustomWebView.TEXT_VIEW )
        {
            int scale = pref_web_view.getInt("KEY_WEB_VIEW_SCALE",0);
            webView.setInitialScale(scale);
        }
        else if( whichView == CustomWebView.LINK_VIEW )
        {
            bWebViewIsShown = false;
            webView.setInitialScale(30);
        }

        int style = Note.getStyle();
		webView.setBackgroundColor(ColorSet.mBG_ColorArray[style]);

    	webView.getSettings().setBuiltInZoomControls(true);
    	webView.getSettings().setSupportZoom(true);
    	webView.getSettings().setUseWideViewPort(true);
//    	customWebView.getSettings().setLoadWithOverviewMode(true);
    	webView.getSettings().setJavaScriptEnabled(true);//??? Using setJavaScriptEnabled can introduce XSS vulnerabilities

//		// speed up
//		if (Build.VERSION.SDK_INT >= 19) {
//			// chromium, enable hardware acceleration
//			webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
//		} else {
//			// older android version, disable hardware acceleration
//			webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
//		}

        if( whichView == CustomWebView.TEXT_VIEW )
   		{
	    	webView.setWebViewClient(new WebViewClient()
	        {
	            @Override
	            public void onScaleChanged(WebView web_view, float oldScale, float newScale)
	            {
	                super.onScaleChanged(web_view, oldScale, newScale);
	//                System.out.println("Note_adapter / onScaleChanged");
	//                System.out.println("    oldScale = " + oldScale);
	//                System.out.println("    newScale = " + newScale);

	                int newDefaultScale = (int) (newScale*100);
	                pref_web_view.edit().putInt("KEY_WEB_VIEW_SCALE",newDefaultScale).apply();

	                //update current position
	                Note.mCurrentPosition = pager.getCurrentItem();
	            }

	            @Override
	            public void onPageFinished(WebView view, String url) {}
	        });

   		}
	    
    	if(whichView == CustomWebView.LINK_VIEW)
    	{
	        webView.setWebChromeClient(new WebChromeClient()
	        {
	            public void onProgressChanged(WebView view, int progress)
	            {
                    System.out.println("---------------- spinner progress = " + progress);

                    if(spinner != null )
	            	{
						if(bWebViewIsShown)
						{
							if (progress < 100 && (spinner.getVisibility() == ProgressBar.GONE)) {
								webView.setVisibility(View.GONE);
								spinner.setVisibility(ProgressBar.VISIBLE);
							}

							spinner.setProgress(progress);

							if (progress > 30)
								bWebViewIsShown = true;
						}

						if(bWebViewIsShown || (progress == 100))
						{
							spinner.setVisibility(ProgressBar.GONE);
							webView.setVisibility(View.VISIBLE);
						}
	            	}
	            }

	            @Override
			    public void onReceivedTitle(WebView view, String title) {
			        super.onReceivedTitle(view, title);
			        if (!TextUtils.isEmpty(title) &&
			        	!title.equalsIgnoreCase("about:blank"))
			        {
			        	System.out.println("Note_adapter / _onReceivedTitle / title = " + title);

						int position = Note.mCurrentPosition;
				    	String tag = "current"+position+"textWebView";
				    	CustomWebView textWebView = (CustomWebView) pager.findViewWithTag(tag);

				    	String strLink = db_page.getNoteLinkUri(position,true);

						// show title of http link
				    	if((textWebView != null) &&
				    	    !Util.isYouTubeLink(strLink) &&
				    	    strLink.startsWith("http")        )
			        	{
				        	mWebTitle = title;
		        			showTextWebView(position,textWebView);
			        	}
			        }
			    }
			});
    	}
	}

    final private static int VIEW_PORT_BY_NONE = 0;
    final private static int VIEW_PORT_BY_DEVICE_WIDTH = 1;
    final private static int VIEW_PORT_BY_SCREEN_WIDTH = 2;
    
    // Get HTML string with view port
    private String getHtmlStringWithViewPort(int position, int viewPort)
    {
    	int mStyle = Note.mStyle;
    	
    	System.out.println("Note_adapter / _getHTMLstringWithViewPort");
    	String strTitle = db_page.getNoteTitle(position,true);
    	String strBody = db_page.getNoteBody(position,true);
    	String audioUri = db_page.getNoteAudioUri(position,true);
    	String linkUri = db_page.getNoteLinkUri(position,true);

    	// replace note title 
    	if(Util.isEmptyString(strTitle))
    	{
    		// with web title
    		if( Util.isEmptyString(audioUri) &&
    		   !Util.isYouTubeLink(linkUri)  &&
    		   !Util.isEmptyString(mWebTitle)  )
    	   	{
    		   strTitle = mWebTitle;
    	   	}
    	   	// with YouTube title
	   		else if(Util.isYouTubeLink(linkUri))
				strTitle = Util.getYoutubeTitle(linkUri);
    	}
    	
    	Long createTime = db_page.getNoteCreatedTime(position,true);
    	String head = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"+
		       	  	  "<html><head>" +
	  		       	  "<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />";
    	
    	if(viewPort == VIEW_PORT_BY_NONE)
    	{
	    	head = head + "<head>";
    	}
    	else if(viewPort == VIEW_PORT_BY_DEVICE_WIDTH)
    	{
	    	head = head + 
	    		   "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" +
	     	  	   "<head>";
    	}
    	else if(viewPort == VIEW_PORT_BY_SCREEN_WIDTH)
    	{
//        	int screen_width = UtilImage.getScreenWidth(act);
        	int screen_width = 640;
	    	head = head +
	    		   "<meta name=\"viewport\" content=\"width=" + String.valueOf(screen_width) + ", initial-scale=1\">"+
   	  			   "<head>";
    	}
    		
       	String separatedLineTitle = (!Util.isEmptyString(strTitle))?"<hr size=2 color=blue width=99% >":"";
       	String separatedLineBody = (!Util.isEmptyString(strBody))?"<hr size=1 color=black width=99% >":"";

       	// title
       	if(!Util.isEmptyString(strTitle))
       	{
       		Spannable spanTitle = new SpannableString(strTitle);
       		Linkify.addLinks(spanTitle, Linkify.ALL);
       		spanTitle.setSpan(new AlignmentSpan.Standard(Alignment.ALIGN_CENTER), 
       							0,
       							spanTitle.length(), 
       							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
       		strTitle = Html.toHtml(spanTitle);
       	}
       	else
       		strTitle = "";
    	
    	// body
       	if(!Util.isEmptyString(strBody))
       	{
	    	Spannable spanBody = new SpannableString(strBody);
	    	Linkify.addLinks(spanBody, Linkify.ALL);
	    	strBody = Html.toHtml(spanBody);
       	}
       	else
       		strBody = "";
	    	
    	// set web view text color
    	String colorStr = Integer.toHexString(ColorSet.mText_ColorArray[mStyle]);
    	colorStr = colorStr.substring(2);
    	
    	String bgColorStr = Integer.toHexString(ColorSet.mBG_ColorArray[mStyle]);
    	bgColorStr = bgColorStr.substring(2);
    	
    	return   head + "<body color=\"" + bgColorStr + "\">" +
		         "<p align=\"center\"><b>" + 
				 "<font color=\"" + colorStr + "\">" + strTitle + "</font>" + 
         		 "</b></p>" + separatedLineTitle +
		         "<p>" + 
				 "<font color=\"" + colorStr + "\">" + strBody + "</font>" +
				 "</p>" + separatedLineBody +
		         "<p align=\"right\">" + 
				 "<font color=\"" + colorStr + "\">"  + Util.getTimeString(createTime) + "</font>" +
		         "</p>" + 
		         "</body></html>";
    }

    // show image by touch image view
    private void showImageByTouchImageView(final View spinner, final TouchImageView pictureView, String strPicture)
    {
        if(Util.isEmptyString(strPicture))
        {
            pictureView.setImageResource(Note.mStyle%2 == 1 ?
                    R.drawable.btn_radio_off_holo_light:
                    R.drawable.btn_radio_off_holo_dark);//R.drawable.ic_empty);
        }
        else if(!Util.isUriExisted(strPicture, act))
        {
            pictureView.setImageResource(R.drawable.ic_not_found);
        }
        else
        {
            Uri imageUri = Uri.parse(strPicture);
            if(imageUri.isAbsolute())
                UilCommon.imageLoader.displayImage(imageUri.toString(),
                        pictureView,
                        UilCommon.optionsForFadeIn,
                        new SimpleImageLoadingListener()
                        {
                            @Override
                            public void onLoadingStarted(String imageUri, View view)
                            {
                                System.out.println("_showImageByTouchImageView / onLoadingStarted");
                                // make spinner appears at center
                                spinner.setVisibility(View.VISIBLE);
                                view.setVisibility(View.GONE);
                            }

                            @Override
                            public void onLoadingFailed(String imageUri, View view, FailReason failReason)
                            {
                                System.out.println("_showImageByTouchImageView / onLoadingFailed");
                                String message = null;
                                switch (failReason.getType())
                                {
                                    case IO_ERROR:
                                        message = "Input/Output error";
                                        break;
                                    case DECODING_ERROR:
                                        message = "Image can't be decoded";
                                        break;
                                    case NETWORK_DENIED:
                                        message = "Downloads are denied";
                                        break;
                                    case OUT_OF_MEMORY:
                                        message = "Out Of Memory error";
                                        break;
                                    case UNKNOWN:
                                        message = "Unknown error";//??? mark this line?
                                        break;
                                }
                                Toast.makeText(act, message, Toast.LENGTH_SHORT).show();
                                spinner.setVisibility(View.GONE);
                                view.setVisibility(View.GONE);
                            }

                            @Override
                            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage)
                            {
                                System.out.println("_showImageByTouchImageView / onLoadingComplete");
                                spinner.setVisibility(View.GONE);
                                view.setVisibility(View.VISIBLE);
                                pictureView.setImageBitmap(loadedImage);
                            }
                        });
        }
    }
}//class Note_view_adapter extends PagerAdapter