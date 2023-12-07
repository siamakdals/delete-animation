package com.siamak.deleteanimation;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import java.util.Arrays;
import java.util.Random;

public class DeleteMessageEffect extends View {

    private Bitmap bitmap;
    private int bitmapWidth, bitmapHeight;
    private int cleared = 0;
    private int gap, space;
    private Paint paint;
    private Particle[] pixelParticles;
    private int pixelsCountX, pixelsCountY;
    private int[] transparentPixels;
    private int viewX, viewY;

    public static final int DEVICE_PERFORMANCE_LOW = 1;
    public static final int DEVICE_PERFORMANCE_AVERAGE = 2;
    public static final int DEVICE_PERFORMANCE_HIGH = 3;

    private ValueAnimator globalAnimator;
    private float globalAnimatedValue = 0f;

    public DeleteMessageEffect(Context context, View view, int devicePerformance) {
        super(context);

        ViewGroup parent = (ViewGroup) view.getParent();

        if (parent != null) {
            parent.addView(this);
        } else {
            return;
        }

        setupDrawingCache(view);

        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                initializeBitmap(view);
                setupViewPosition(view);
                configureDevicePerformance(devicePerformance);
                initializeTransparentPixels();
                initializeParticleArray();
                setPixels();
                setupPaint();
                startGlobalAnimator();
                removeGlobalLayoutListener(view, this);
                view.setVisibility(GONE);
            }
        });
    }

    private void startGlobalAnimator() {
        globalAnimator = ValueAnimator.ofFloat(0f, 1f);
        globalAnimator.setDuration(10000);
        globalAnimator.addUpdateListener(animation -> {
            globalAnimatedValue = (float) animation.getAnimatedValue();
            for (Particle particle : pixelParticles) {
                particle.updateCurrentPosition(globalAnimatedValue);
            }
            invalidate(); // Trigger redraw to update the particles' positions
        });
        globalAnimator.start();
    }

    private void removeGlobalLayoutListener(View view, ViewTreeObserver.OnGlobalLayoutListener listener) {
        view.getViewTreeObserver().removeOnGlobalLayoutListener(listener);
    }

    private void setupPaint() {
        paint = new Paint();
        paint.setAlpha(255);
        paint.setStyle(Paint.Style.FILL);
    }

    private void setupDrawingCache(View view) {
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
    }

    private void initializeBitmap(View view) {
        bitmap = Bitmap.createBitmap(view.getDrawingCache());
        bitmapWidth = bitmap.getWidth();
        bitmapHeight = bitmap.getHeight();
        view.setDrawingCacheEnabled(false);
    }

    private void setupViewPosition(View view) {
        viewX = view.getLeft();
        viewY = view.getTop();
    }

    private void configureDevicePerformance(int devicePerformance) {
        if (devicePerformance == DEVICE_PERFORMANCE_HIGH) {
            gap = 4;
            space = 4;
        } else if (devicePerformance == DEVICE_PERFORMANCE_AVERAGE) {
            gap = 6;
            space = 4;
        } else {
            gap = 8;
            space = 4;
        }
    }

    private void initializeTransparentPixels() {
        transparentPixels = new int[gap * bitmapHeight];
        Arrays.fill(transparentPixels, 0);
    }

    private void initializeParticleArray() {
        pixelsCountX = bitmapWidth / gap;
        pixelsCountY = bitmapHeight / gap;
        pixelParticles = new Particle[pixelsCountX * pixelsCountY];
    }

    private void setPixels() {

        int[] totalPixels = new int[bitmapWidth * bitmapHeight];
        bitmap.getPixels(totalPixels, 0, bitmapWidth, 0, 0, bitmapWidth, bitmapHeight);

        int index = 0;

        for (int y = 0; y < pixelsCountY; y++) {
            for (int x = 0; x < pixelsCountX; x++) {
                int pixelColor = totalPixels[bitmapWidth * (y * gap) + (x * gap)];

                if (Particle.hasNonZeroAlpha(pixelColor)) {
                    int offsetX = (x * gap) + viewX;
                    int offsetY = (y * gap) + viewY;
                    pixelParticles[index] = new Particle(
                            pixelColor,
                            offsetX,
                            offsetY,
                            offsetX + randomInt(-100, 100),
                            offsetY + randomInt(-100, 100),
                            space,
                            (float) x
                    );
                } else {
                    pixelParticles[index] = new Particle(pixelColor, 0, 0, 0, 0, 0, 0.0f);
                }
                index++;
            }
        }
    }


    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawBitmap(bitmap, (float) viewX, (float) viewY, paint);

        boolean allParticlesFinished = true;

        for (Particle particle : pixelParticles) {
            if (particle.step > 1 || particle.color == -1) {
                continue;
            }

            if (particle.delay <= 0.0f && cleared < particle.offsetX) {

                bitmap.setPixels(
                        transparentPixels,
                        0,
                        gap,
                        particle.offsetX - viewX,
                        particle.offsetY - viewY,
                        gap,
                        Math.min(bitmapHeight - Math.max(0, particle.offsetY - viewY), bitmapHeight)
                );

                cleared = particle.offsetX;

                if (cleared >= viewX + bitmapWidth - (gap * 2)) {
                    bitmap.eraseColor(0);
                }
            }

            particle.draw(canvas, paint);
            allParticlesFinished = false;
        }

        if (!allParticlesFinished) {
            paint.setAlpha(255);
        }

    }

    private static int randomInt(int min, int max) {
        return new Random().nextInt((max - min) + 1) + min;
    }

    public static class Particle {
        private int destinationX, destinationY;
        private int offsetX, offsetY;
        private float currentX, currentY;
        private int color;
        private float alpha = 1.0f;
        private float delay = 0f;
        private float ease = 0.07f;
        private int step = 0;
        private int skipPixels;

        public Particle(int color, int offsetX, int offsetY, int destinationX, int destinationY, int skipPixels, float delay) {
            if (hasNonZeroAlpha(color)) {
                initializeParticle(color, offsetX, offsetY, destinationX, destinationY, skipPixels, delay);
                currentX = (float) offsetX;
                currentY = (float) offsetY;
            } else {
                this.color = -1;
            }
        }

        private static boolean hasNonZeroAlpha(int color) {
            return ((color >> 24) & 0xFF) > 0;
        }

        private void initializeParticle(int color, int offsetX, int offsetY, int destinationX, int destinationY, int skipPixels, float delay) {
            this.color = color;
            this.skipPixels = skipPixels;
            this.delay = delay;

            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.currentX = (float) offsetX;
            this.currentY = (float) offsetY;

            this.destinationX = destinationX;
            this.destinationY = destinationY;
        }

        public void updateCurrentPosition(float animatedValue) {
            currentX = offsetX + (destinationX - offsetX) * animatedValue;
        }

        public void draw(Canvas canvas, Paint paint) {
            if (delay > 0.0f) {
                handleDelayedDrawing();
                return;
            }

            setPaintAttributes(paint);

            canvas.drawRect(currentX, currentY, currentX + skipPixels, currentY + skipPixels, paint);

            updateValues();
        }

        private void handleDelayedDrawing() {
            alpha -= 0.0005;
            delay -= 2;
        }

        private void setPaintAttributes(Paint paint) {
            paint.setColor(color);
            paint.setAlpha((int) (alpha * 255));
        }

        private void updateValues() {
            currentX = currentX + (destinationX - currentX) * ease;

            float newY = currentY + (destinationY - currentY) * ease;
            newY = newY - Math.abs(DeleteMessageEffect.randomInt(10, 15));


            //don't want to move bottom, just top
            if (newY < currentY) {
                currentY = newY;
            }


            alpha -= 0.02;

            if (alpha <= 0.001) {
                step = 2;
            }
        }

    }
}
