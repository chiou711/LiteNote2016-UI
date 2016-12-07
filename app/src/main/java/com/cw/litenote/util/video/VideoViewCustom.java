package com.cw.litenote.util.video;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.VideoView;
// Custom video view
public class VideoViewCustom extends VideoView {

    private int mForceHeight = 0;
    private int mForceWidth = 0;
    public VideoViewCustom(Context context) {
        super(context);
    }

    public VideoViewCustom(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoViewCustom(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setDimensions(int w, int h) {
        this.mForceHeight = h;
        this.mForceWidth = w;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        Log.i("@@@@", "VideoViewCustom / onMeasure");
        setMeasuredDimension(mForceWidth, mForceHeight);
    }
    
    // interface of Play/Pause listener
    public static interface PlayPauseListener {
        void onPlay();
        void onPause();
    }    
    
    // instance of Play/Pause listener
    private PlayPauseListener mListener;
    
    // set Play/Pause listener
    public void setPlayPauseListener(PlayPauseListener listener) 
    {
        mListener = listener;
    }
    
    // redirect VideoView _start/_pause function to listener
    @Override
    public void start() {
        super.start();
        if (mListener != null) {
            mListener.onPlay();
        }
    } 
    
    @Override
    public void pause() {
        super.pause();
        if (mListener != null) {
            mListener.onPause();
        }
    }
    
}
