package me.insertcoin.testpion;

import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

import me.insertcoin.lib.pion.gl.GlCore;
import me.insertcoin.lib.pion.gl.GlEncoderConfig;
import me.insertcoin.lib.pion.gl.GlOffscreenEncoder;
import me.insertcoin.lib.pion.gl.GlRenderMode;
import me.insertcoin.lib.pion.gl.GlRenderer;
import me.insertcoin.lib.pion.gl.GlSurfaceView;
import me.insertcoin.lib.pion.gl.GlTextureView;

public class EncodingActivity extends AppCompatActivity {
    private static final String TAG = EncodingActivity.class.getSimpleName();
    private TextView mTvFps;
    private GlSurfaceView mSurfaceView;
    private GlTextureView mTextureView;
    private GlCore.FrameUpdateCallback mFrameUpdateCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encoding);

        mTvFps = (TextView)findViewById(R.id.tv_fps);
        mSurfaceView = (GlSurfaceView)findViewById(R.id.surface_view);
        mTextureView = (GlTextureView)findViewById(R.id.texture_view);

        final String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "PionEncoding";

        // fps
        mFrameUpdateCallback = new GlCore.FrameUpdateCallback() {
            @Override
            public void onFrameUpdate(float fps) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mTvFps != null)
                            mTvFps.setText(String.format("FPS: %.2f", GlCore.getInstance().getFps()));
                    }
                });
            }
        };

        // surface view
        {
            mSurfaceView.setGlCore(new GlCore(), true);

            Renderer renderer = new Renderer(Color.YELLOW);
            mSurfaceView.setRenderMode(GlRenderMode.CONTINUOUSLY);
            mSurfaceView.setRenderer(renderer, true);

            mSurfaceView.setFixedRenderBufferSize(320, 180);

            GlEncoderConfig.Builder builder = new GlEncoderConfig.Builder();
            builder.setSize(320, 180);
            GlEncoderConfig config = builder.build();

            final String filePath = dirPath + File.separator + "on_screen_surfaceView.mp4";
            EncoderView encoderView = (EncoderView)findViewById(R.id.ctrl_surface_view);
            encoderView.setTarget(mSurfaceView, new File(filePath), config);
        }

        // texture view
        {
            Renderer renderer = new Renderer(Color.BLUE);
            mTextureView.setRenderMode(GlRenderMode.CONTINUOUSLY);
            mTextureView.setRenderer(renderer, true);

            GlEncoderConfig.Builder builder = new GlEncoderConfig.Builder();
            builder.setSize(1280, 720);
            GlEncoderConfig config = builder.build();

            String filePath = dirPath + File.separator + "on_screen_textureView.mp4";
            EncoderView encoderView = (EncoderView) findViewById(R.id.ctrl_texture_view);
            encoderView.setTarget(mTextureView, new File(filePath), config);
        }

        // blocking off-screen encoding
        {
            final Renderer renderer = new Renderer(Color.GREEN);
            final String filePath = dirPath + File.separator + "off_screen_blocking.mp4";

            final GlEncoderConfig.Builder builder = new GlEncoderConfig.Builder();
            builder.setSize(1280, 720);

            final Button button = (Button)findViewById(R.id.btn_off_scr_encode);
            button.setOnClickListener(new View.OnClickListener() {
                GlOffscreenEncoder encoder;

                @Override
                public void onClick(View v) {
                    button.setEnabled(false);

                    final GlCore glCore = new GlCore();

                    try {
                        encoder = new GlOffscreenEncoder(glCore, builder.build(), new File(filePath));
                    } catch (IOException e) {
                        throw new RuntimeException(e.getMessage());
                    }

                    glCore.acquireGl(renderer);

                    encoder.process(new Runnable() {
                        @Override
                        public void run() {
                            int fps = 24;
                            int duration = 3;
                            int totalFrames = fps * duration;
                            float frameInterval = 1.0f / fps;

                            for (int i = 0; i < totalFrames; ++i) {
                                float time = i * frameInterval;
                                long timeMs = (long)(time * 1000);

                                encoder.beginFrame();
                                renderer.onGlResize(encoder.getWidth(), encoder.getHeight());
                                renderer.update(time);
                                renderer.draw();
                                encoder.endFrame(timeMs);
                            }
                        }
                    }, new GlOffscreenEncoder.OnCompleteCallback() {
                        @Override
                        public void onComplete(GlOffscreenEncoder encoder) {
                            glCore.releaseGl(renderer);
                            encoder.release();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    button.setEnabled(true);
                                }
                            });

                            glCore.release();
                        }
                    });
                }
            });
        }

        // non-blocking off-screen encoding
        {
            final GlCore glCore = GlCore.getInstance();

            final Renderer renderer = new Renderer(Color.CYAN);
            final String filePath = dirPath + File.separator + "off_screen_nonblocking.mp4";

            final GlEncoderConfig.Builder builder = new GlEncoderConfig.Builder();
            builder.setSize(1280, 720);

            final Button button = (Button)findViewById(R.id.btn_off_scr_encode_non_blocking);
            button.setOnClickListener(new View.OnClickListener() {
                GlOffscreenEncoder encoder;

                @Override
                public void onClick(View v) {
                    button.setEnabled(false);

                    try {
                        encoder = new GlOffscreenEncoder(glCore, builder.build(), new File(filePath));
                    } catch (IOException e) {
                        throw new RuntimeException(e.getMessage());
                    }

                    new Thread() {
                        @Override
                        public void run() {
                            glCore.acquireGl(renderer);

                            int fps = 24;
                            int duration = 3;
                            int totalFrames = fps * duration;
                            float frameInterval = 1.0f / fps;

                            for (int i = 0; i < totalFrames; ++i) {
                                final float time = i * frameInterval;
                                long timeMs = (long) (time * 1000);

                                // Blocking frame drawing
                                encoder.processFrame(new Runnable() {
                                    @Override
                                    public void run() {
                                        renderer.onGlResize(encoder.getWidth(), encoder.getHeight());
                                        renderer.update(time);
                                        renderer.draw();
                                    }
                                }, timeMs);
                            }

                            glCore.releaseGl(renderer);
                            encoder.release();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    button.setEnabled(true);
                                }
                            });
                        }
                    }.start();
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSurfaceView.resume();
        mTextureView.resume();
        GlCore.getInstance().addFrameUpdateCallback(mFrameUpdateCallback);
    }

    @Override
    protected void onPause() {
        GlCore.getInstance().removeFrameUpdateCallback(mFrameUpdateCallback);
        mSurfaceView.pause();
        mTextureView.pause();
        super.onPause();
    }

    private static class Renderer implements GlRenderer {
        private final float[] mMvpMatrix = new float[16];
        private final float[] mProjectionMatrix = new float[16];
        private final float[] mViewMatrix = new float[16];
        private final float[] mModelMatrix = new float[16];
        private final float[] mClearColor;

        private TestUtils.Shader mShader;
        private TestUtils.Square mSquare;
        private float mPrevTime = -1;
        private float mAngle = 0;

        Renderer(int clearColor) {
            mClearColor = TestUtils.convertColor(clearColor);
        }

        @Override
        public void onGlAcquire() {
            mShader = new TestUtils.Shader();
            mSquare = new TestUtils.Square();
        }

        @Override
        public void onGlRelease() {
            mShader.release();
            mShader = null;
        }

        @Override
        public void onGlResize(int width, int height) {
            GLES20.glViewport(0, 0, width, height);

            float ratio = (float) width / height;
            Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
        }

        @Override
        public void drawFrame() {
            update(currentTime());
            draw();
        }

        private float currentTime() {
            return SystemClock.uptimeMillis() / 1000.0f;
        }

        private float deltaTime(float currentTIme) {
            if (mPrevTime < 0) {
                mPrevTime = currentTIme;
                return 0;
            }

            float deltaTime = currentTIme - mPrevTime;
            mPrevTime = currentTIme;

            return deltaTime;
        }

        void update(float currentTIme) {
            mAngle = (mAngle + 360 * deltaTime(currentTIme));
            while (mAngle > 360)
                mAngle -= 360;
        }

        void draw() {
            GLES20.glClearColor(mClearColor[0], mClearColor[1], mClearColor[2], mClearColor[3]);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            Matrix.setLookAtM(mViewMatrix, 0,
                    0, 0, -3,
                    0, 0, 0,
                    0, 1, 0);

            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.rotateM(mModelMatrix, 0, mAngle, 0, 0, -1);

            float[] tmp = new float[16];
            Matrix.multiplyMM(tmp, 0, mProjectionMatrix, 0, mViewMatrix, 0);
            Matrix.multiplyMM(mMvpMatrix, 0, tmp, 0, mModelMatrix, 0);

            mShader.draw(mSquare, mMvpMatrix, Color.RED);
        }
    }
}