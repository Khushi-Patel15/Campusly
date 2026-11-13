package com.example.campusly.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.core.content.ContextCompat;

import com.example.campusly.R;

import java.util.Random;

// Draws one shooting star flying across the top of the screen: a glowing
// head, a fading tail, and a few twinkling sparkles left behind it.
// It only draws; it never takes touches, so the screen underneath still works.
public class ShootingStarView extends View {

    private static final int SPARKLE_COUNT = 16;

    private final Paint tailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint coreTailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint headPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sparklePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final int lime;
    private final float density;

    // For each sparkle: how far behind the head (0..1 of the tail), how far
    // to the side, its size, and a twinkle phase.
    private final float[][] sparkles = new float[SPARKLE_COUNT][4];

    // 0 = star just entered on the left, 1 = left the screen on the right.
    private float progress = -1f;

    public ShootingStarView(Context context) {
        super(context);
        lime = ContextCompat.getColor(context, R.color.neon_lime);
        density = getResources().getDisplayMetrics().density;

        tailPaint.setStyle(Paint.Style.STROKE);
        tailPaint.setStrokeCap(Paint.Cap.ROUND);
        tailPaint.setStrokeWidth(4 * density);

        coreTailPaint.setStyle(Paint.Style.STROKE);
        coreTailPaint.setStrokeCap(Paint.Cap.ROUND);
        coreTailPaint.setStrokeWidth(1.5f * density);

        headPaint.setColor(Color.WHITE);

        Random random = new Random();
        for (float[] sparkle : sparkles) {
            sparkle[0] = 0.05f + random.nextFloat() * 0.9f;
            sparkle[1] = (random.nextFloat() - 0.5f) * 14 * density;
            sparkle[2] = (0.8f + random.nextFloat() * 1.6f) * density;
            sparkle[3] = random.nextFloat() * 6.28f;
        }
    }

    // Plays the flight once. onHalfway runs when the star is near the middle
    // (that's when the notification card appears); onEnd runs when it's gone.
    public void play(long durationMs, Runnable onHalfway, Runnable onEnd) {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(durationMs);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());

        boolean[] halfwayDone = {false};
        animator.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            if (!halfwayDone[0] && progress >= 0.4f) {
                halfwayDone[0] = true;
                onHalfway.run();
            }
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                progress = -1f;
                invalidate();
                onEnd.run();
            }
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (progress < 0) return;

        float w = getWidth();
        float h = getHeight();

        // Path: from just off the left edge near the top, to just off the
        // right edge a little lower, so it crosses where the card appears.
        float startX = -0.15f * w;
        float startY = 0.04f * h;
        float endX = 1.15f * w;
        float endY = 0.16f * h;

        float headX = startX + (endX - startX) * progress;
        float headY = startY + (endY - startY) * progress;

        float dx = endX - startX;
        float dy = endY - startY;
        float length = (float) Math.hypot(dx, dy);
        float dirX = dx / length;
        float dirY = dy / length;
        // Perpendicular direction, for spreading sparkles sideways.
        float perpX = -dirY;
        float perpY = dirX;

        // The tail grows during take-off, then the whole star fades out at the end.
        float tailLength = 0.45f * w * Math.min(1f, progress * 3f);
        float fade = progress > 0.8f ? (1f - progress) / 0.2f : 1f;

        float tailX = headX - dirX * tailLength;
        float tailY = headY - dirY * tailLength;

        int limeFaded = withAlpha(lime, fade);
        int whiteFaded = withAlpha(Color.WHITE, fade);

        tailPaint.setShader(new LinearGradient(tailX, tailY, headX, headY,
                Color.TRANSPARENT, limeFaded, Shader.TileMode.CLAMP));
        canvas.drawLine(tailX, tailY, headX, headY, tailPaint);

        coreTailPaint.setShader(new LinearGradient(tailX, tailY, headX, headY,
                Color.TRANSPARENT, whiteFaded, Shader.TileMode.CLAMP));
        canvas.drawLine(tailX, tailY, headX, headY, coreTailPaint);

        for (int i = 0; i < SPARKLE_COUNT; i++) {
            float[] s = sparkles[i];
            float behind = s[0] * tailLength;
            float x = headX - dirX * behind + perpX * s[1];
            float y = headY - dirY * behind + perpY * s[1];
            float twinkle = 0.55f + 0.45f * (float) Math.sin(progress * 40f + s[3]);
            float alpha = (1f - s[0]) * twinkle * fade;
            sparklePaint.setColor(withAlpha(i % 3 == 0 ? Color.WHITE : lime, alpha));
            canvas.drawCircle(x, y, s[2], sparklePaint);
        }

        float glowRadius = 18 * density;
        glowPaint.setShader(new RadialGradient(headX, headY, glowRadius,
                limeFaded, Color.TRANSPARENT, Shader.TileMode.CLAMP));
        canvas.drawCircle(headX, headY, glowRadius, glowPaint);

        headPaint.setAlpha((int) (255 * fade));
        canvas.drawCircle(headX, headY, 3.5f * density, headPaint);
    }

    private static int withAlpha(int color, float alpha) {
        int a = Math.max(0, Math.min(255, (int) (Color.alpha(color) * alpha)));
        return (color & 0x00FFFFFF) | (a << 24);
    }
}
