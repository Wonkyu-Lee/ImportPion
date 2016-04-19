package me.insertcoin.testpion;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ToggleButton;

import java.io.File;
import java.io.IOException;

import me.insertcoin.lib.pion.gl.GlEncoderConfig;
import me.insertcoin.lib.pion.gl.GlOnscreenEncoder;
import me.insertcoin.lib.pion.gl.GlView;

/**
 * Created by blazeq on 2016. 4. 12..
 */
public class EncoderView extends FrameLayout {
    private ToggleButton mTglCreate;
    private ToggleButton mTglStart;
    private OnScreenEncoderController mController;

    public EncoderView(Context context) {
        super(context);
        initialize();
    }

    public EncoderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    @Override
    protected void onDetachedFromWindow() {
        release();
        super.onDetachedFromWindow();
    }

    private void initialize() {
        String infService = Context.LAYOUT_INFLATER_SERVICE;
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(infService);
        View view = inflater.inflate(R.layout.view_encoder, this, false);
        addView(view);

        mTglCreate = (ToggleButton)view.findViewById(R.id.tgl_create);
        mTglStart = (ToggleButton)view.findViewById(R.id.tgl_start);

        mTglCreate.setSaveEnabled(false);
        mTglStart.setSaveEnabled(false);
    }

    public void release() {
        resetTarget();
    }

    public void setTarget(GlView glView, File outFile, GlEncoderConfig config) {
        resetTarget();
        mController = new OnScreenEncoderController(glView, outFile, config);
        mTglCreate.setOnCheckedChangeListener(mController);
        mTglStart.setOnCheckedChangeListener(mController);
    }

    private void resetTarget() {
        if (mController != null) {
            mController.releaseEncoder();
            mController = null;
        }

        mTglCreate.setOnCheckedChangeListener(null);
        mTglStart.setOnCheckedChangeListener(null);
    }

    private class OnScreenEncoderController implements CompoundButton.OnCheckedChangeListener {
        private final GlView mGlView;
        private final File mOutFile;
        private final GlEncoderConfig mConfig;
        private GlOnscreenEncoder mEncoder;

        private OnScreenEncoderController(GlView glView, File outFile, GlEncoderConfig config) {
            mGlView = glView;
            mOutFile = outFile;
            mConfig = config;
            mTglStart.setEnabled(false);
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (buttonView == mTglCreate) {
                if (isChecked) {
                    createEncoder();
                    mTglStart.setEnabled(true);
                } else {
                    mTglStart.setEnabled(false);
                    releaseEncoder();
                }
            }

            if (buttonView == mTglStart) {
                if (isChecked) {
                    startEncoder();
                } else {
                    stopEncoder();
                }
            }
        }

        private void createEncoder() {
            try {
                mEncoder = mGlView.createEncoder(mConfig, mOutFile);
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        private void releaseEncoder() {
            if (mEncoder != null) {
                mEncoder.release();
                mEncoder = null;
            }
        }

        private void startEncoder() {
            if (mEncoder == null)
                return;

            mEncoder.start();
        }

        private void stopEncoder() {
            if (mEncoder == null)
                return;

            mEncoder.stop();
        }
    }
}
